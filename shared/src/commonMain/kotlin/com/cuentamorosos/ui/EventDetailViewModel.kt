package com.cuentamorosos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuentamorosos.data.repository.DebtRepository
import com.cuentamorosos.data.repository.EventRepository
import com.cuentamorosos.data.repository.ExpenseRepository
import com.cuentamorosos.model.CalculationResult
import com.cuentamorosos.model.EventAction
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.PermissionEngine
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SettlementEngine
import com.cuentamorosos.model.SplitCalculator
import com.cuentamorosos.model.StateTransitionResult
import com.cuentamorosos.model.TransitionContext
import com.cuentamorosos.model.canTransitionTo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EventDetailViewModel(
    private val eventRepository: EventRepository,
    private val debtRepository: DebtRepository,
    private val expenseRepository: ExpenseRepository,
    private val currentProfileId: String = "",
) : ViewModel() {

    private val _eventId = MutableStateFlow<String?>(null)
    val eventId: StateFlow<String?> = _eventId.asStateFlow()

    private val _currentEvent = MutableStateFlow<EventItem?>(null)
    val currentEvent: StateFlow<EventItem?> = _currentEvent.asStateFlow()

    private val _transitionWarning = MutableStateFlow<StateTransitionResult.AllowedWithWarning?>(null)
    val transitionWarning: StateFlow<StateTransitionResult.AllowedWithWarning?> = _transitionWarning.asStateFlow()

    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()

    private val _permissionError = MutableStateFlow<String?>(null)
    val permissionError: StateFlow<String?> = _permissionError.asStateFlow()

    private val _currentRole = MutableStateFlow<EventRole>(EventRole.READER)
    val currentRole: StateFlow<EventRole> = _currentRole.asStateFlow()

    private val _calculationResult = MutableStateFlow<CalculationResult?>(null)
    val calculationResult: StateFlow<CalculationResult?> = _calculationResult.asStateFlow()

    init {
        observeCurrentEvent()
        observeRole()
    }

    private fun observeRole() {
        viewModelScope.launch {
            _currentEvent.collect { event ->
                _currentRole.value = if (event != null && currentProfileId.isNotBlank()) {
                    PermissionEngine.getRole(currentProfileId, event)
                } else {
                    EventRole.READER
                }
            }
        }
    }

    private fun observeCurrentEvent() {
        viewModelScope.launch {
            _eventId.flatMapLatest { id ->
                if (id == null) flowOf(null) else eventRepository.observeEvent(id)
            }.collect { event ->
                _currentEvent.value = event
            }
        }
    }

    val debts: Flow<List<EventDebtItem>> = _eventId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else debtRepository.observeDebts(id)
    }

    val allDebts: StateFlow<List<EventDebtItem>> = debtRepository.observeAllDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: Flow<List<EventExpenseItem>> = _eventId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else expenseRepository.observeExpenses(id)
    }

    val allExpenses: StateFlow<List<EventExpenseItem>> = expenseRepository.observeAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setEventId(id: String?) {
        _eventId.value = id
    }

    fun saveDebt(debt: EventDebtItem) {
        viewModelScope.launch {
            debtRepository.saveDebt(debt)
        }
    }

    fun deleteDebt(eventId: String, debtId: String) {
        viewModelScope.launch {
            debtRepository.deleteDebt(eventId, debtId)
        }
    }

    fun saveExpense(expense: EventExpenseItem) {
        viewModelScope.launch {
            expenseRepository.saveExpense(expense)
        }
    }

    fun deleteExpense(eventId: String, expenseId: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(eventId, expenseId)
        }
    }

    fun calculateEvent(context: TransitionContext) {
        viewModelScope.launch {
            val event = _currentEvent.value ?: return@launch
            val result = event.canTransitionTo(EventState.CALCULATED, context)
            when (result) {
                is StateTransitionResult.Allowed -> {
                    eventRepository.saveEvent(event.copy(state = result.newState))
                }
                is StateTransitionResult.AllowedWithWarning -> {
                    _transitionWarning.value = result
                }
                is StateTransitionResult.Blocked -> {
                    _validationErrors.value = result.reasons
                }
            }
        }
    }

    fun openEvent(context: TransitionContext) {
        viewModelScope.launch {
            val event = _currentEvent.value ?: return@launch
            val result = event.canTransitionTo(EventState.OPEN, context)
            when (result) {
                is StateTransitionResult.Allowed -> {
                    eventRepository.saveEvent(event.copy(state = result.newState))
                }
                is StateTransitionResult.AllowedWithWarning -> {
                    _transitionWarning.value = result
                }
                is StateTransitionResult.Blocked -> {
                    _validationErrors.value = result.reasons
                }
            }
        }
    }

    fun recalculateEvent(context: TransitionContext) {
        viewModelScope.launch {
            val event = _currentEvent.value ?: return@launch
            val result = event.canTransitionTo(EventState.OPEN, context)
            when (result) {
                is StateTransitionResult.Allowed -> {
                    eventRepository.saveEvent(event.copy(state = result.newState))
                }
                is StateTransitionResult.AllowedWithWarning -> {
                    _transitionWarning.value = result
                }
                is StateTransitionResult.Blocked -> {
                    _validationErrors.value = result.reasons
                }
            }
        }
    }

    fun closeEvent(context: TransitionContext) {
        viewModelScope.launch {
            val event = _currentEvent.value ?: return@launch
            val result = event.canTransitionTo(EventState.CLOSED, context)
            when (result) {
                is StateTransitionResult.Allowed -> {
                    eventRepository.saveEvent(event.copy(state = result.newState))
                }
                is StateTransitionResult.AllowedWithWarning -> {
                    _transitionWarning.value = result
                }
                is StateTransitionResult.Blocked -> {
                    _validationErrors.value = result.reasons
                }
            }
        }
    }

    fun confirmTransition(newState: EventState) {
        viewModelScope.launch {
            val event = _currentEvent.value ?: return@launch
            eventRepository.saveEvent(event.copy(state = newState))
            _transitionWarning.value = null
        }
    }

    fun dismissTransitionWarning() {
        _transitionWarning.value = null
    }

    fun clearValidationErrors() {
        _validationErrors.value = emptyList()
    }

    fun dismissPermissionError() {
        _permissionError.value = null
    }

    /**
     * Runs settlement calculation using SettlementEngine.calculateWithEdgeCases().
     * Emits the result via [calculationResult] StateFlow.
     */
    fun calculateSettlement(
        event: EventItem,
        expenses: List<EventExpenseItem>,
        profiles: List<ProfileItem>,
    ) {
        viewModelScope.launch {
            val result = SettlementEngine.calculateWithEdgeCases(
                event = event,
                expenses = expenses,
                profileNameResolver = { id -> profiles.find { it.id == id }?.name ?: id },
            )
            _calculationResult.value = result
        }
    }

    /**
     * Computes net balance per profile from the given expenses.
     * Positive = creditor (received more than owed), negative = debtor.
     */
    fun getProfileBalances(expenses: List<EventExpenseItem>): Map<String, Int> {
        val balances = mutableMapOf<String, Int>()
        for (expense in expenses) {
            for ((payerId, amount) in expense.payerContributions) {
                val cents = (amount * 100).roundToInt()
                balances[payerId] = balances.getOrDefault(payerId, 0) + cents
            }
            val debtorAmounts = computeDebtorAmountsForExpense(expense)
            for ((debtorId, amount) in debtorAmounts) {
                val cents = (amount * 100).roundToInt()
                balances[debtorId] = balances.getOrDefault(debtorId, 0) - cents
            }
        }
        return balances
    }

    /**
     * Sums all expense amounts in cents.
     */
    fun getTotalExpenseCents(expenses: List<EventExpenseItem>): Int {
        return expenses.sumOf { (it.amountEuros * 100).roundToInt() }
    }

    /**
     * Checks whether the current user can perform an action on the current event.
     */
    fun canDo(action: EventAction): Boolean {
        val event = _currentEvent.value ?: return false
        return PermissionEngine.canDo(currentProfileId, event, action)
    }

    /**
     * Returns the role of the current user in the current event.
     */
    fun getRole(): EventRole {
        val event = _currentEvent.value ?: return EventRole.READER
        return PermissionEngine.getRole(currentProfileId, event)
    }

    /**
     * Computes how much each debtor owes for a single expense,
     * mirroring SettlementEngine's computeDebtorAmounts logic.
     */
    private fun computeDebtorAmountsForExpense(expense: EventExpenseItem): Map<String, Double> {
        return when (expense.splitMode) {
            "SIMPLE_AVG" -> SplitCalculator.calculateEqual(expense.amountEuros, expense.debtorIds)
            "CUSTOM_PERCENTAGE" -> SplitCalculator.calculatePercentage(expense.amountEuros, expense.profileWeights)
            "EXACT" -> {
                if (expense.profileWeights.isNotEmpty()) {
                    SplitCalculator.calculateExact(expense.amountEuros, expense.profileWeights)
                } else {
                    SplitCalculator.calculateEqual(expense.amountEuros, expense.debtorIds)
                }
            }
            "PARTS" -> {
                val parts = expense.profileWeights.mapValues { (_, v) -> v.toInt() }
                if (parts.isNotEmpty()) {
                    SplitCalculator.calculateParts(expense.amountEuros, parts)
                } else {
                    SplitCalculator.calculateEqual(expense.amountEuros, expense.debtorIds)
                }
            }
            else -> SplitCalculator.calculateEqual(expense.amountEuros, expense.debtorIds)
        }
    }
}
