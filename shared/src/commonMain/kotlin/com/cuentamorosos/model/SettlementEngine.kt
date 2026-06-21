package com.cuentamorosos.model

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.model.validation.ItemValidator
import com.cuentamorosos.model.validation.allErrors
import com.cuentamorosos.model.validation.hasErrors
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 8-step settlement calculation algorithm.
 * Computes minimum transfers to settle all debts within an event.
 */
object SettlementEngine {

    private const val ALGORITHM_VERSION = "v1-greedy"
    private const val ZERO_TOLERANCE_CENTS = 1

    /**
     * In-memory lock to prevent concurrent calculations for the same event.
     * Uses Mutex from kotlinx.coroutines for KMP compatibility.
     */
    object CalculationLock {
        private val mutex = Mutex()
        private var locks: MutableMap<String, Long> = mutableMapOf()
        private const val TIMEOUT_MS = 30_000L

        fun tryAcquire(eventId: String): Boolean = runBlocking {
            mutex.withLock {
                val now = currentTimeMillis()
                val lockedAt = locks[eventId]
                if (lockedAt != null) {
                    if (now - lockedAt > TIMEOUT_MS) {
                        // Stale lock — auto-release and acquire
                        locks[eventId] = now
                        true
                    } else {
                        false // Active lock, cannot acquire
                    }
                } else {
                    locks[eventId] = now
                    true
                }
            }
        }

        fun release(eventId: String) = runBlocking {
            mutex.withLock {
                locks.remove(eventId)
            }
        }

        /** Test-only: set a lock timestamp for simulating stale locks. */
        internal fun setLockForTest(eventId: String, timestamp: Long) = runBlocking {
            mutex.withLock {
                locks[eventId] = timestamp
            }
        }

        /** Test-only: clear all locks. */
        internal fun clearAllForTest() = runBlocking {
            mutex.withLock {
                locks.clear()
            }
        }
    }

    /**
     * Calculates the settlement for an event.
     * Returns CalculationResult with snapshot on success, or errors on failure.
     */
    fun calculate(
        event: EventItem,
        expenses: List<EventExpenseItem>,
        profileNameResolver: (String) -> String = { it },
    ): CalculationResult {
        // Step 1: Pre-validate all expenses
        val validationErrors = mutableListOf<String>()
        for (expense in expenses) {
            val result = ItemValidator.validate(
                item = expense,
                eventMemberIds = event.effectiveMemberIds.toSet(),
                profileNameResolver = profileNameResolver,
                eventBaseCurrency = event.baseCurrency,
            )
            if (result.hasErrors()) {
                validationErrors.addAll(result.allErrors().map { it.message })
            }
        }
        if (validationErrors.isNotEmpty()) {
            return CalculationResult(
                errors = listOf("Hay ${validationErrors.size} gastos con errores. Revisalos antes de calcular.") + validationErrors
            )
        }

        // Step 2: Currency conversion stub (all 1:1 for now — D6 will implement real FX)

        // Step 3: Net balance per person (in cents to avoid float drift)
        val balances = mutableMapOf<String, Int>()

        for (expense in expenses) {
            // Add what each payer paid
            for ((payerId, amount) in expense.payerContributions) {
                val cents = (amount * 100).roundToInt()
                balances[payerId] = (balances[payerId] ?: 0) + cents
            }

            // Subtract what each debtor owes
            val debtorAmounts = computeDebtorAmounts(expense)
            for ((debtorId, amount) in debtorAmounts) {
                val cents = (amount * 100).roundToInt()
                balances[debtorId] = (balances[debtorId] ?: 0) - cents
            }
        }

        // Step 4: Verify sum zero
        val sumCents = balances.values.sum()
        if (abs(sumCents) > ZERO_TOLERANCE_CENTS) {
            return CalculationResult(
                errors = listOf("Error interno: los balances no suman cero. Contactá soporte.")
            )
        }

        // Step 5: Separate creditors (positive) and debtors (negative)
        val entries = balances.toList() // List<Pair<String, Int>>
        val creditors = entries
            .filter { (_, v) -> v > 0 }
            .sortedByDescending { it.second }
            .toMutableList()

        val debtors = entries
            .filter { (_, v) -> v < 0 }
            .sortedBy { it.second }
            .toMutableList()

        // Step 6: Greedy minimum transfers
        val transfers = mutableListOf<SettlementTransfer>()
        var ci = 0
        var di = 0

        while (ci < creditors.size && di < debtors.size) {
            val (debtorId, debtorBal) = debtors[di]
            val (creditorId, creditorBal) = creditors[ci]

            // Step 7: No self-transfers (defensive)
            if (debtorId == creditorId) {
                ci++
                di++
                continue
            }

            val transferCents = minOf(abs(debtorBal), creditorBal)

            if (transferCents > 0) {
                transfers.add(SettlementTransfer(
                    fromProfileId = debtorId,
                    toProfileId = creditorId,
                    amount = transferCents / 100.0,
                ))
            }

            val newDebtorBal = debtorBal + transferCents
            val newCreditorBal = creditorBal - transferCents

            if (newDebtorBal >= 0) di++
            else debtors[di] = debtorId to newDebtorBal

            if (newCreditorBal <= 0) ci++
            else creditors[ci] = creditorId to newCreditorBal
        }

        // Step 8: Build snapshot
        val totalExpenseCents = expenses.sumOf { (it.amountEuros * 100).roundToInt() }
        val balancesAsDoubles = balances.mapValues { it.value / 100.0 }

        println("🔍 [SettlementEngine] === CALCULATION RESULT ===")
        println("🔍   transfers: ${transfers.size}")
        transfers.forEach { t ->
            println("🔍   transfer: ${t.fromProfileId} -> ${t.toProfileId} ${t.amount}€")
        }
        println("🔍   totalExpenseCents=$totalExpenseCents -> euros=${totalExpenseCents / 100.0}")
        println("🔍   balances=$balancesAsDoubles")
        val totalTransferAmount = transfers.sumOf { it.amount }
        println("🔍   sum(transfers.amount)=$totalTransferAmount")

        val snapshot = CalculationSnapshot(
            transfers = transfers,
            totalExpense = totalExpenseCents / 100.0,
            calculatedAtMillis = currentTimeMillis(),
            algorithmVersion = ALGORITHM_VERSION,
            participantBalances = balancesAsDoubles,
        )

        return CalculationResult(snapshot = snapshot)
    }

    /**
     * Calculates settlement with edge case detection.
     * Wraps [calculate] with pre-checks (lock, participant consistency) and
     * post-checks (zero-balance, zero-transfer filter, deleted creditors, self-netting).
     *
     * @param event The event to calculate.
     * @param expenses List of expenses to include.
     * @param priorSnapshot Optional previous snapshot for participant consistency.
     * @param deletedProfileIds Profile IDs that have been deleted.
     * @param profileNameResolver Function to resolve profile ID to display name.
     * @return CalculationResult with enriched status.
     */
    fun calculateWithEdgeCases(
        event: EventItem,
        expenses: List<EventExpenseItem>,
        priorSnapshot: CalculationSnapshot? = null,
        deletedProfileIds: Set<String> = emptySet(),
        profileNameResolver: (String) -> String = { it },
    ): CalculationResult {
        // Pre-check: acquire lock
        if (!CalculationLock.tryAcquire(event.id)) {
            return CalculationResult(
                errors = listOf("Cálculo en progreso para este evento. Esperá unos segundos."),
                status = CalculationStatus.Error("Cálculo en progreso para este evento. Esperá unos segundos."),
            )
        }

        try {
            // Pre-check: participant consistency
            val currentMemberIds = event.effectiveMemberIds.toSet()
            val consistencyResult = IntegrityGuard.canRecalculate(
                currentMemberIds = currentMemberIds,
                priorSnapshot = priorSnapshot,
                profileNameResolver = profileNameResolver,
            )
            if (consistencyResult.isFailure) {
                val message = consistencyResult.exceptionOrNull()?.message
                    ?: "No se puede recalcular: participante faltante."
                return CalculationResult(
                    errors = listOf(message),
                    status = CalculationStatus.Error(message),
                )
            }

            // Run existing calculation
            val result = calculate(event, expenses, profileNameResolver)
            if (!result.isSuccess) {
                return result.copy(status = CalculationStatus.Error(result.errors.firstOrNull() ?: "Error de validación"))
            }

            // Post-checks
            val snapshot = result.snapshot!!
            val balances = snapshot.participantBalances
            val transfers = snapshot.transfers

            val edgeCaseStatus = runPostChecks(
                balances = balances,
                transfers = transfers,
                deletedProfileIds = deletedProfileIds,
                profileNameResolver = profileNameResolver,
            )

            return if (edgeCaseStatus is CalculationStatus.ZeroBalance) {
                // Zero-balance: return result with empty transfers, caller should NOT persist
                result.copy(
                    snapshot = snapshot.copy(transfers = emptyList()),
                    status = edgeCaseStatus,
                )
            } else {
                result.copy(status = edgeCaseStatus)
            }
        } finally {
            CalculationLock.release(event.id)
        }
    }

    /**
     * Runs post-calculation edge case checks.
     * Returns the appropriate CalculationStatus.
     */
    private fun runPostChecks(
        balances: Map<String, Double>,
        transfers: List<SettlementTransfer>,
        deletedProfileIds: Set<String>,
        profileNameResolver: (String) -> String,
    ): CalculationStatus {
        // Check 1: Zero-balance detection (all balances within ±1 cent of zero)
        val allZero = balances.values.all { abs(it) <= 0.01 }
        if (allZero) {
            return CalculationStatus.ZeroBalance("Todo está saldado")
        }

        // Check 2: Zero-transfer filter
        val (filteredTransfers, statusFromFilter) = filterZeroTransfers(transfers)

        if (statusFromFilter != null) return statusFromFilter

        // Check 3: Deleted creditor handling
        val deletedCreditorTransfers = filteredTransfers.filter { it.toProfileId in deletedProfileIds }
        if (deletedCreditorTransfers.isNotEmpty()) {
            val creditorName = profileNameResolver(deletedCreditorTransfers.first().toProfileId)
            return CalculationStatus.EdgeCaseWarning("El acreedor $creditorName ya no está disponible")
        }

        // Check 4: Self-netting detection (empty transfers but non-zero balances)
        if (filteredTransfers.isEmpty() && balances.values.any { abs(it) > 0.01 }) {
            return CalculationStatus.EdgeCaseWarning("Los saldos se compensan internamente")
        }

        return CalculationStatus.Success("Cálculo completado")
    }

    /**
     * Filters out zero-amount transfers and redistributes any residual.
     * Returns filtered transfers and optionally a status if an edge case was detected.
     */
    private fun filterZeroTransfers(
        transfers: List<SettlementTransfer>,
    ): Pair<List<SettlementTransfer>, CalculationStatus?> {
        val filtered = transfers.filter { it.amount > 0.005 }

        if (filtered.size == transfers.size) {
            return transfers to null // No changes
        }

        // Redistribute residual from filtered-out transfers
        val originalTotalCents = transfers.sumOf { (it.amount * 100).roundToInt() }
        val filteredTotalCents = filtered.sumOf { (it.amount * 100).roundToInt() }
        val residualCents = originalTotalCents - filteredTotalCents

        if (residualCents == 0) {
            return filtered to null
        }

        // Assign residual to first active debtor (alphabetically by profileId)
        val activeDebtors = filtered.map { it.fromProfileId }.toSet().sorted()
        val residualAmount = residualCents / 100.0

        val redistributed = if (activeDebtors.isNotEmpty()) {
            val firstDebtor = activeDebtors.first()
            filtered.map { transfer ->
                if (transfer.fromProfileId == firstDebtor) {
                    transfer.copy(amount = transfer.amount + residualAmount)
                } else {
                    transfer
                }
            }
        } else {
            filtered
        }

        return redistributed to null
    }

    /**
     * Computes how much each debtor owes for a single expense,
     * based on its split mode.
     */
    internal fun computeDebtorAmounts(expense: EventExpenseItem): Map<String, Double> {
        val seed = expense.id.hashCode() xor expense.eventId.hashCode()
        return when (expense.splitMode) {
            "SIMPLE_AVG" -> {
                SplitCalculator.calculateEqual(expense.amountEuros, expense.debtorIds, seed = seed)
            }
            "CUSTOM_PERCENTAGE" -> {
                SplitCalculator.calculatePercentage(expense.amountEuros, expense.profileWeights)
            }
            "EXACT" -> {
                if (expense.profileWeights.isNotEmpty()) {
                    SplitCalculator.calculateExact(expense.amountEuros, expense.profileWeights)
                } else {
                    SplitCalculator.calculateEqual(expense.amountEuros, expense.debtorIds, seed = seed)
                }
            }
            "PARTS" -> {
                val parts = expense.profileWeights.mapValues { (_, v) -> v.toInt() }
                if (parts.isNotEmpty()) {
                    SplitCalculator.calculateParts(expense.amountEuros, parts)
                } else {
                    SplitCalculator.calculateEqual(expense.amountEuros, expense.debtorIds, seed = seed)
                }
            }
            "REAL_CONSUMPTION" -> {
                if (expense.profileWeights.isNotEmpty()) {
                    val weightSum = expense.profileWeights.values.sum()
                    val diff = kotlin.math.abs(weightSum - expense.amountEuros)
                    if (diff > 0.02) {
                        println("[SettlementEngine] REAL_CONSUMPTION weight sum (${weightSum}) differs from item total (${expense.amountEuros}) by ${diff}")
                    }
                    expense.profileWeights
                } else {
                    SplitCalculator.calculateEqual(expense.amountEuros, expense.debtorIds, seed = seed)
                }
            }
            else -> SplitCalculator.calculateEqual(expense.amountEuros, expense.debtorIds, seed = seed)
        }
    }
}
