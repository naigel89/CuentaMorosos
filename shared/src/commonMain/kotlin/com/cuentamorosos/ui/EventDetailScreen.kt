package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.isValidEmail
import com.cuentamorosos.model.CalculationResult
import com.cuentamorosos.model.EventAction
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.ExpenseCategory
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.displayNameFor
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.formattedDate
import com.cuentamorosos.model.parseEuroAmount
import com.cuentamorosos.model.toCalculationSnapshot
import kotlinx.coroutines.launch

// ── EventDetailScreen ─────────────────────────────────────────────────────────

@Suppress("UNUSED_PARAMETER")
@Composable
fun EventDetailScreen(
    modifier: Modifier = Modifier,
    event: EventItem,
    profiles: List<ProfileItem>,
    eventDebts: List<EventDebtItem>,
    eventExpenses: List<EventExpenseItem>,
    currentUserUid: String?,
    onBack: () -> Unit,
    onAddProfileToEvent: (ProfileItem) -> Unit,
    onSaveDebt: (EventDebtItem) -> Unit,
    onTogglePaid: (EventDebtItem) -> Unit,
    onRemoveDebt: (String) -> Unit,
    onSaveExpense: (EventExpenseItem) -> Unit,
    onRemoveExpense: (String) -> Unit,
    onApplyCalculation: (CalculationResult) -> Unit,
    onInviteMember: (String) -> Unit,
    _onRemoveMember: (String) -> Unit,
    currentRole: EventRole = EventRole.OWNER,
    canDo: (EventAction) -> Boolean = { true },
    onCloseEvent: (() -> Unit)? = null,
) {
    val profileById = profiles.associateBy { it.id }
    val eventParticipants = profiles.filter { it.id in event.effectiveMemberIds }
    val pendingTotal = eventDebts.filter { !it.paid }.sumOf { it.amountEuros }
    val eventExpenseTotal = eventExpenses.sumOf { it.amountEuros }
    val availableProfiles = profiles.filter { profile ->
        eventDebts.none { it.profileId == profile.id }
    }
    val canClose = event.state == EventState.CALCULATED &&
        canDo(EventAction.Close) && eventDebts.all { it.paid }

    var editableDebt by remember { mutableStateOf<EventDebtItem?>(null) }
    var editableExpense by remember { mutableStateOf<EventExpenseItem?>(null) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var showQuickSplitDialog by remember { mutableStateOf(false) }
    var showInviteMemberDialog by remember { mutableStateOf(false) }
    var showRemoveOwnerConfirm by remember { mutableStateOf<EventDebtItem?>(null) }
    var profileToRemove by remember { mutableStateOf<String?>(null) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    val currentUid = currentUserUid ?: ""

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header section (always full width) ──────────────────────────
            HeaderSection(
                event = event,
                totalExpenses = eventExpenseTotal,
                _totalPending = pendingTotal,
                _expenseCount = eventExpenses.size,
                onBack = onBack,
                isWide = isWide,
                currentRole = currentRole,
            )

            // ── Main content ───────────────────────────────────────────────
            if (isWide) {
                // Two-column layout: expenses (left 2/3) + settlement (right 1/3)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Expenses column
                    Column(
                        modifier = Modifier.weight(2f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExpensesList(
                            eventExpenses = eventExpenses,
                            _profiles = profiles,
                            currentUid = currentUid,
                            profileById = profileById,
                            currentRole = currentRole,
                            canDo = canDo,
                            onAddExpense = {
                                editableExpense = EventExpenseItem(
                                    eventId = event.id,
                                    name = "",
                                    amountEuros = 0.0,
                                    paidByProfileId = currentUid,
                                )
                            },
                            onEditExpense = { editableExpense = it },
                            onRemoveExpense = onRemoveExpense,
                        )
                    }

                    // Settlement sidebar
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SettlementPanel(
                            _event = event,
                            debts = eventDebts,
                            profiles = profiles,
                            _pendingTotal = pendingTotal,
                            _expenseTotal = eventExpenseTotal,
                            currentUserUid = currentUid,
                            onCalculateTotals = { showQuickSplitDialog = true },
                            onTogglePaid = onTogglePaid,
                            onAddProfile = { showAddProfileDialog = true },
                            onInviteMember = { showInviteMemberDialog = true },
                            canCalculate = canDo(EventAction.Calculate),
                            canManageParticipants = canDo(EventAction.ManageParticipants),
                            canInvite = canDo(EventAction.ManageParticipants),
                            eventState = event.state,
                            canClose = canClose,
                            onCloseEvent = onCloseEvent,
                            onRemoveMember = { profileToRemove = it },
                            lastCalculationSummary = event.lastCalculationSummary,
                            onViewReceipt = { showReceiptDialog = true },
                        )
                    }
                }
            } else {
                // Single column layout (mobile)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Total cost card on mobile (below header)
                    TotalCostCard(
                        totalExpenses = eventExpenseTotal,
                        totalPending = pendingTotal,
                        expenseCount = eventExpenses.size,
                    )

                    ExpensesList(
                        eventExpenses = eventExpenses,
                        _profiles = profiles,
                        currentUid = currentUid,
                        profileById = profileById,
                        currentRole = currentRole,
                        canDo = canDo,
                        onAddExpense = {
                            editableExpense = EventExpenseItem(
                                eventId = event.id,
                                name = "",
                                amountEuros = 0.0,
                                paidByProfileId = currentUid,
                            )
                        },
                        onEditExpense = { editableExpense = it },
                        onRemoveExpense = onRemoveExpense,
                    )

                    SettlementPanel(
                        _event = event,
                        debts = eventDebts,
                        profiles = profiles,
                        _pendingTotal = pendingTotal,
                        _expenseTotal = eventExpenseTotal,
                        currentUserUid = currentUid,
                        onCalculateTotals = { showQuickSplitDialog = true },
                        onTogglePaid = onTogglePaid,
                        onAddProfile = { showAddProfileDialog = true },
                        onInviteMember = { showInviteMemberDialog = true },
                        canCalculate = canDo(EventAction.Calculate),
                        canManageParticipants = canDo(EventAction.ManageParticipants),
                        canInvite = canDo(EventAction.ManageParticipants),
                        eventState = event.state,
                        canClose = canClose,
                        onCloseEvent = onCloseEvent,
                        onRemoveMember = { profileToRemove = it },
                        lastCalculationSummary = event.lastCalculationSummary,
                        onViewReceipt = { showReceiptDialog = true },
                    )
                }
            }
        }
    }

    // ── Dialogs (preserved — all 7) ─────────────────────────────────────────

    // Dialog 1: AddProfileDialog
    if (showAddProfileDialog && canDo(EventAction.ManageParticipants)) {
        AddProfileToEventDialog(
            availableProfiles = availableProfiles,
            onDismiss = { showAddProfileDialog = false },
            onAddProfile = { profile ->
                showAddProfileDialog = false
                onAddProfileToEvent(profile)
            }
        )
    }

    // Dialog 2: ExpenseEditorSheet (NeoFintech ModalBottomSheet)
    editableExpense?.let { expense ->
        ExpenseEditorSheet(
            expense = expense,
            allProfiles = profiles,
            effectiveMemberIds = event.effectiveMemberIds,
            currentUid = currentUid,
            onDismiss = { editableExpense = null },
            onSave = { updatedExpense ->
                editableExpense = null
                onSaveExpense(updatedExpense)
            },
            onRemove = {
                editableExpense = null
                onRemoveExpense(expense.id)
            }
        )
    }

    // Dialog 3: DebtEditorDialog
    editableDebt?.let { debt ->
        DebtEditorDialog(
            debt = debt,
            profile = profileById[debt.profileId],
            onDismiss = { editableDebt = null },
            onSave = { updatedDebt ->
                editableDebt = null
                onSaveDebt(updatedDebt)
            },
            onRemove = {
                editableDebt = null
                if (debt.profileId == event.ownerId) {
                    showRemoveOwnerConfirm = debt
                } else {
                    onRemoveDebt(debt.id)
                }
            }
        )
    }

    // Dialog 4: RemoveOwnerConfirmDialog
    if (showRemoveOwnerConfirm != null) {
        val debtToRemove = showRemoveOwnerConfirm!!
        AlertDialog(
            onDismissRequest = { showRemoveOwnerConfirm = null },
            title = { Text("Eliminar propietario") },
            text = {
                Text("¿Seguro? No participarás en el reparto de este evento.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveOwnerConfirm = null
                    onRemoveDebt(debtToRemove.id)
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveOwnerConfirm = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog 5: CalculatorSheet (replaces QuickSplitDialog)
    if (showQuickSplitDialog && canDo(EventAction.Calculate)) {
        // Derive deleted profile IDs: profiles that were event members but no longer have debts
        val currentParticipantIds = eventParticipants.map { it.id }.toSet()
        val deletedProfileIds = event.effectiveMemberIds.filter { it !in currentParticipantIds }.toSet()

        CalculatorSheet(
            event = event,
            profiles = eventParticipants,
            eventExpenses = eventExpenses,
            onDismiss = { showQuickSplitDialog = false },
            onApply = { calculationResult ->
                showQuickSplitDialog = false
                onApplyCalculation(calculationResult)
            },
            _deletedProfileIds = deletedProfileIds,
            _priorSnapshot = null, // Prior snapshot not yet persisted — future enhancement
            currentUserUid = currentUserUid,
        )
    }

    // Dialog 6: ReceiptPanel — full breakdown of calculated event
    if (showReceiptDialog && event.state == EventState.CALCULATED) {
        val snapshot = event.lastCalculationSummary?.toCalculationSnapshot()
        if (snapshot != null) {
            ReceiptPanel(
                event = event,
                snapshot = snapshot,
                profiles = eventParticipants,
                onDismiss = { showReceiptDialog = false },
            )
        } else {
            // No valid snapshot — close dialog and reset
            showReceiptDialog = false
        }
    }

    // Dialog 7: InviteMemberDialog
    if (showInviteMemberDialog && canDo(EventAction.ManageParticipants)) {
        InviteMemberDialog(
            onDismiss = { showInviteMemberDialog = false },
            onInvite = { email ->
                showInviteMemberDialog = false
                onInviteMember(email)
            }
        )
    }

    // Dialog 8: RemoveParticipantConfirmation
    if (profileToRemove != null) {
        val profileName = profileById[profileToRemove]?.displayNameFor(currentUserUid ?: "") ?: "Este participante"
        AlertDialog(
            onDismissRequest = { profileToRemove = null },
            title = { Text("Eliminar participante") },
            text = {
                Text("¿Estás seguro de que querés eliminar a $profileName del evento?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        _onRemoveMember(profileToRemove!!)
                        profileToRemove = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToRemove = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ── HeaderSection ─────────────────────────────────────────────────────────────

@Suppress("UNUSED_PARAMETER")
@Composable
private fun HeaderSection(
    event: EventItem,
    totalExpenses: Double,
    _totalPending: Double,
    _expenseCount: Int,
    onBack: () -> Unit,
    isWide: Boolean,
    currentRole: EventRole = EventRole.OWNER,
) {
    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Back button
        TextButton(onClick = onBack) {
            Text("← Volver", color = colors.primaryContainer)
        }

        if (isWide) {
            // Wide: title + date on left, total cost card on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface,
                        )
                        StateBadge(state = event.state)
                    }
                    Text(
                        text = "📅 ${event.formattedDate()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColors.onSurfaceVariant,
                    )
                }
                // Compact total cost for wide header
                Card(
                    modifier = Modifier.cardShadow(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceContainerLow),
                    shape = NeoFintechShapes.lg,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = "TOTAL DEL EVENTO",
                            style = MaterialTheme.typography.labelSmall,
                            color = themeColors.onSurfaceVariant,
                        )
                        Text(
                            text = formatEuros(totalExpenses),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryContainer,
                        )
                    }
                }
            }
        } else {
            // Mobile: title + date (TotalCostCard is shown below in the scroll area)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface,
                )
                StateBadge(state = event.state)
            }
            Text(
                text = "📅 ${event.formattedDate()}",
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.onSurfaceVariant,
            )
        }
    }
}

// ── ExpensesList ──────────────────────────────────────────────────────────────

@Suppress("UNUSED_PARAMETER")
@Composable
private fun ExpensesList(
    eventExpenses: List<EventExpenseItem>,
    _profiles: List<ProfileItem>,
    currentUid: String,
    profileById: Map<String, ProfileItem>,
    currentRole: EventRole,
    canDo: (EventAction) -> Boolean,
    onAddExpense: () -> Unit,
    onEditExpense: (EventExpenseItem) -> Unit,
    onRemoveExpense: (String) -> Unit,
) {
    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme

    // Title + Add button row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Gastos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = themeColors.onSurface,
        )
        Button(
            onClick = onAddExpense,
            enabled = canDo(EventAction.CreateExpense),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            ),
            shape = NeoFintechShapes.lg,
        ) {
            Text("Añadir ítem", fontWeight = FontWeight.Bold)
        }
    }

    if (eventExpenses.isEmpty()) {
        SuggestionCard(
            message = "Añade gastos del evento con categoría y perfiles implicados para usar `consumo real` o `por categoría`."
        )
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            eventExpenses.forEach { expense ->
                val paidByProfile = expense.paidByProfileId.takeIf { it.isNotBlank() }
                    ?.let { profileById[it] }
                val isCurrentUser = expense.paidByProfileId == currentUid

                ExpenseItemCard(
                    expense = expense,
                    paidByProfile = paidByProfile,
                    isCurrentUser = isCurrentUser,
                    isReadOnly = currentRole == EventRole.READER,
                    onTap = { onEditExpense(expense) },
                    onEdit = { onEditExpense(expense) },
                    onDelete = { onRemoveExpense(expense.id) },
                    enabledEdit = canDo(EventAction.EditExpense(expense.paidByProfileId)),
                    enabledDelete = canDo(EventAction.DeleteExpense(expense.paidByProfileId)),
                )
            }
        }
    }
}

// ── Dialogs (preserved exactly as before) ─────────────────────────────────────

// ── InviteMemberDialog ────────────────────────────────────────────────────────

@Composable
private fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onInvite: (email: String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invitar miembro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Introduce el email del usuario que quieres invitar a este evento.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    singleLine = true,
                )
                validationMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = email.trim()
                if (trimmed.isEmpty() || !isValidEmail(trimmed)) {
                    validationMessage = "Introduce un email válido."
                } else {
                    onInvite(trimmed)
                }
            }) {
                Text("Invitar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── AddProfileToEventDialog ───────────────────────────────────────────────────

@Composable
private fun AddProfileToEventDialog(
    availableProfiles: List<ProfileItem>,
    onDismiss: () -> Unit,
    onAddProfile: (ProfileItem) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir perfil al evento") },
        text = {
            if (availableProfiles.isEmpty()) {
                Text(
                    text = "No hay perfiles disponibles para añadir. Crea uno primero desde la pestaña `Perfiles`.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableProfiles.forEach { profile ->
                        OutlinedButton(
                            onClick = { onAddProfile(profile) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProfileAvatar(name = profile.name, emoji = profile.icon, photoUrl = profile.photoUrl, size = 28.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(profile.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (availableProfiles.isEmpty()) "Cerrar" else "Cancelar")
            }
        }
    )
}

// ── ExpenseEditorSheet (NeoFintech ModalBottomSheet) ──────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseEditorSheet(
    expense: EventExpenseItem,
    allProfiles: List<ProfileItem>,
    effectiveMemberIds: List<String>,
    currentUid: String,
    onDismiss: () -> Unit,
    onSave: (EventExpenseItem) -> Unit,
    onRemove: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme
    val shapes = NeoFintechShapes
    val typography = MaterialTheme.typography

    var name by remember(expense.id) { mutableStateOf(expense.name) }
    var amountText by remember(expense.id) {
        mutableStateOf(if (expense.amountEuros == 0.0) "" else expense.amountEuros.toString())
    }
    var selectedCategoryId by remember(expense.id) { mutableStateOf(expense.category.ifBlank { ExpenseCategory.SHARED.id }) }
    var selectedProfileIds by remember(expense.id) { mutableStateOf(expense.assignedProfileIds) }
    var selectedWeights by remember(expense.id) {
        mutableStateOf(expense.profileWeights.mapValues { it.value.toString() })
    }
    var showCustomSplit by remember(expense.id) {
        mutableStateOf(expense.profileWeights.isNotEmpty())
    }
    var selectedPaidByProfileId by remember(expense.id) {
        mutableStateOf(expense.paidByProfileId)
    }
    var validationMessage by remember(expense.id) { mutableStateOf<String?>(null) }

    val isNew = expense.name.isBlank() && expense.amountEuros == 0.0
    val title = if (isNew) "Nuevo gasto" else "Editar gasto"

    // Only show profiles that are part of the event
    val currentParticipants = allProfiles.filter { it.id in effectiveMemberIds }

    fun updateSelectedProfiles(newSelection: List<String>) {
        selectedProfileIds = newSelection
        selectedWeights = selectedWeights.filterKeys { it in newSelection }
        validationMessage = null
    }

    fun dismissSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    fun saveAndDismiss() {
        val parsedAmount = parseEuroAmount(amountText)
        val selectedCategory = ExpenseCategory.fromId(selectedCategoryId)
        val customWeightValues = selectedProfileIds.mapNotNull { profileId ->
            parseDecimalValue(selectedWeights[profileId] ?: "")
        }
        val customWeightTotal = customWeightValues.sum()
        validationMessage = when {
            name.isBlank() -> "Indica un nombre para el ítem."
            parsedAmount == null -> "Introduce un importe válido."
            parsedAmount < 0.0 -> "El importe no puede ser negativo."
            selectedPaidByProfileId.isBlank() -> "Seleccioná quién pagó el gasto."
            selectedCategory != ExpenseCategory.SHARED && selectedProfileIds.isEmpty() -> "Selecciona al menos un perfil para esta categoría."
            selectedCategory != ExpenseCategory.SHARED && showCustomSplit &&
                selectedProfileIds.any { parseDecimalValue(selectedWeights[it] ?: "") == null } ->
                    "Revisa los porcentajes del reparto personalizado."
            selectedCategory != ExpenseCategory.SHARED && showCustomSplit &&
                selectedProfileIds.any { (parseDecimalValue(selectedWeights[it] ?: "") ?: 0.0) < 0.0 } ->
                    "Los porcentajes no pueden ser negativos."
            selectedCategory != ExpenseCategory.SHARED && showCustomSplit &&
                kotlin.math.abs(customWeightTotal - 100.0) >= 0.01 ->
                    "El reparto personalizado debe sumar 100%."
            else -> null
        }

        if (validationMessage == null && parsedAmount != null) {
            val normalizedWeights = if (
                selectedCategory != ExpenseCategory.SHARED &&
                showCustomSplit
            ) {
                selectedProfileIds.associateWith { profileId ->
                    parseDecimalValue(selectedWeights[profileId] ?: "") ?: 0.0
                }
            } else {
                emptyMap()
            }

            val updatedExpense = expense.copy(
                name = name.trim(),
                amountEuros = parsedAmount,
                category = selectedCategory.id,
                splitMode = if (selectedCategory != ExpenseCategory.SHARED && showCustomSplit)
                    "CUSTOM_PERCENTAGE" else "SIMPLE_AVG",
                assignedProfileIds = if (selectedCategory == ExpenseCategory.SHARED) {
                    emptyList()
                } else {
                    selectedProfileIds
                },
                profileWeights = normalizedWeights,
                paidByProfileId = selectedPaidByProfileId,
                debtorIds = if (selectedCategory == ExpenseCategory.SHARED) {
                    effectiveMemberIds
                } else {
                    selectedProfileIds
                },
                payerContributions = if (selectedPaidByProfileId.isNotBlank()) {
                    mapOf(selectedPaidByProfileId to parsedAmount)
                } else {
                    emptyMap()
                },
            )

            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) onSave(updatedExpense)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = shapes.xl,
        tonalElevation = 0.dp,
        dragHandle = {
            // Custom handle bar with NeoFintech styling
            Box(
                modifier = Modifier
                    .padding(top = NeoFintechSpacing.sm, bottom = NeoFintechSpacing.xs)
                    .fillMaxWidth()
                    .height(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp),
                    shape = shapes.pill,
                    color = colors.outlineVariant,
                ) {}
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = NeoFintechSpacing.lg,
                    end = NeoFintechSpacing.lg,
                    bottom = NeoFintechSpacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(NeoFintechSpacing.md),
        ) {
            // ── Title ──────────────────────────────────────────────────────
            Text(
                text = title,
                style = typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )

            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))

            // ── Description & Amount ───────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripción") },
                    singleLine = true,
                    shape = shapes.md,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primary,
                        unfocusedBorderColor = colors.outlineVariant,
                        focusedLabelColor = themeColors.primary,
                        unfocusedLabelColor = colors.onSurfaceVariant,
                        cursorColor = themeColors.primary,
                    ),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Importe (€)") },
                    singleLine = true,
                    shape = shapes.md,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primary,
                        unfocusedBorderColor = colors.outlineVariant,
                        focusedLabelColor = themeColors.primary,
                        unfocusedLabelColor = colors.onSurfaceVariant,
                        cursorColor = themeColors.primary,
                    ),
                )
            }

            // ── Paid by ────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm)) {
                Text(
                    text = "Pagado por",
                    style = typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm),
                ) {
                    // "Yo" option
                    item {
                        val isMeSelected = selectedPaidByProfileId == currentUid
                        Surface(
                            onClick = { selectedPaidByProfileId = currentUid },
                            shape = shapes.pill,
                            color = if (isMeSelected) themeColors.primary else colors.surfaceContainerHigh,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isMeSelected) themeColors.primary else colors.outlineVariant,
                            ),
                        ) {
                            Text(
                                text = "Yo",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = typography.labelLarge,
                                color = if (isMeSelected) colors.onPrimaryContainer else colors.onSurface,
                                fontWeight = if (isMeSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                    items(currentParticipants.filter { it.id != currentUid }, key = { "paid_${it.id}" }) { profile ->
                        val isSelected = selectedPaidByProfileId == profile.id
                        Surface(
                            onClick = { selectedPaidByProfileId = profile.id },
                            shape = shapes.pill,
                            color = if (isSelected) themeColors.primary else colors.surfaceContainerHigh,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) themeColors.primary else colors.outlineVariant,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                ProfileAvatar(
                                    name = profile.name,
                                    emoji = profile.icon,
                                    photoUrl = profile.photoUrl,
                                    size = 22.dp,
                                )
                                Text(
                                    text = profile.name,
                                    style = typography.labelLarge,
                                    color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }

            // ── Category chips ─────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm)) {
                Text(
                    text = "Categoría",
                    style = typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm),
                ) {
                    items(ExpenseCategory.entries, key = { it.id }) { category ->
                        val isSelected = selectedCategoryId == category.id
                        Surface(
                            onClick = {
                                selectedCategoryId = category.id
                                if (category != ExpenseCategory.SHARED) {
                                    showCustomSplit = false
                                }
                            },
                            shape = shapes.pill,
                            color = if (isSelected) {
                                category.iconBgColor.copy(alpha = 0.25f)
                            } else {
                                colors.surfaceContainerHigh
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) category.iconBgColor else colors.outlineVariant.copy(alpha = 0.5f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                // Category emoji icon
                                Surface(
                                    modifier = Modifier.size(28.dp),
                                    shape = shapes.full,
                                    color = category.iconBgColor.copy(alpha = if (isSelected) 0.35f else 0.15f),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = category.iconEmoji,
                                            fontSize = 14.sp,
                                        )
                                    }
                                }
                                Text(
                                    text = category.label,
                                    style = typography.labelLarge,
                                    color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }

            // ── Participants ───────────────────────────────────────────────
            val selectedCategory = ExpenseCategory.fromId(selectedCategoryId)
            if (selectedCategory != ExpenseCategory.SHARED && allProfiles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm)) {
                    Text(
                        text = "¿Quiénes participan?",
                        style = typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )

                    // Current participants section
                    if (currentParticipants.isNotEmpty()) {
                        Text(
                            text = "Participantes del evento",
                            style = typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                        currentParticipants.forEach { profile ->
                            ProfileCheckboxRow(
                                profile = profile,
                                isChecked = selectedProfileIds.contains(profile.id),
                                onToggle = { checked ->
                                    val newSelection = if (checked) {
                                        selectedProfileIds + profile.id
                                    } else {
                                        selectedProfileIds - profile.id
                                    }
                                    updateSelectedProfiles(newSelection)
                                },
                                colors = colors,
                                typography = typography,
                            )
                        }
                    }


                }
            }

            // ── Custom split ───────────────────────────────────────────────
            if (
                selectedCategory != ExpenseCategory.SHARED &&
                selectedProfileIds.isNotEmpty()
            ) {
                OutlinedButton(
                    onClick = {
                        showCustomSplit = !showCustomSplit
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = shapes.md,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
                ) {
                    Text(
                        text = if (showCustomSplit) "Ocultar reparto personalizado"
                        else "Reparto personalizado (opcional)",
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            if (
                selectedCategory != ExpenseCategory.SHARED &&
                selectedProfileIds.isNotEmpty() &&
                showCustomSplit
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm)) {
                    Text(
                        text = "Reparto personalizado (%)",
                        style = typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )

                    var totalWeight = 0.0
                    selectedProfileIds.forEach { profileId ->
                        val profile = allProfiles.find { it.id == profileId }
                        val weightText = selectedWeights[profileId] ?: "0"
                        totalWeight += parseDecimalValue(weightText) ?: 0.0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm),
                        ) {
                            ProfileAvatar(
                                name = profile?.name ?: "",
                                emoji = profile?.icon ?: "",
                                photoUrl = profile?.photoUrl,
                                size = 28.dp,
                            )
                            Text(
                                text = profile?.name ?: "Perfil",
                                modifier = Modifier.weight(1f),
                                style = typography.bodyMedium,
                                color = colors.onSurface,
                            )
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = {
                                    selectedWeights = selectedWeights + (profileId to it)
                                    validationMessage = null
                                },
                                modifier = Modifier.width(80.dp),
                                label = { Text("%") },
                                singleLine = true,
                                shape = shapes.md,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = themeColors.primary,
                                    unfocusedBorderColor = colors.outlineVariant,
                                ),
                            )
                        }
                    }
                    Text(
                        text = "Total: ${String.format("%.2f", totalWeight)}% (debe ser 100%)",
                        style = typography.bodySmall,
                        color = if (kotlin.math.abs(totalWeight - 100.0) < 0.01) themeColors.primary
                                else colors.error,
                        fontFamily = JetBrainsMonoFontFamily(),
                    )
                }
            }

            // ── Validation message ─────────────────────────────────────────
            validationMessage?.let { message ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shapes.md,
                    color = colors.errorContainer.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = message,
                        color = colors.error,
                        style = typography.bodySmall,
                        modifier = Modifier.padding(NeoFintechSpacing.sm),
                    )
                }
            }

            // ── Action buttons ─────────────────────────────────────────────
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm),
            ) {
                // Remove button (only for existing expenses)
                if (!isNew) {
                    TextButton(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) onRemove()
                            }
                        },
                    ) {
                        Text("Eliminar", color = colors.error)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = { dismissSheet() },
                    shape = shapes.md,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
                ) {
                    Text("Cancelar", color = colors.onSurfaceVariant)
                }

                Button(
                    onClick = { saveAndDismiss() },
                    modifier = Modifier.weight(1f),
                    shape = shapes.md,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.primary,
                        contentColor = colors.onPrimaryContainer,
                    ),
                ) {
                    Text("Guardar", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── ProfileCheckboxRow ────────────────────────────────────────────────────────

@Composable
private fun ProfileCheckboxRow(
    profile: ProfileItem,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    colors: NeoFintechColorSet,
    typography: androidx.compose.material3.Typography,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isChecked) },
        shape = NeoFintechShapes.lg,
        color = if (isChecked) colors.primaryContainer.copy(alpha = 0.08f)
        else colors.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isChecked) 1.5.dp else 1.dp,
            color = if (isChecked) themeColorPrimary(colors) else colors.outlineVariant.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NeoFintechSpacing.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm),
        ) {
            ProfileAvatar(
                name = profile.name,
                emoji = profile.icon,
                photoUrl = profile.photoUrl,
                size = 32.dp,
            )
            Text(
                text = profile.name,
                modifier = Modifier.weight(1f),
                style = typography.bodyMedium,
                fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal,
                color = colors.onSurface,
            )
            Checkbox(
                checked = isChecked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = themeColorPrimary(colors),
                    checkmarkColor = colors.onPrimaryContainer,
                    uncheckedColor = colors.onSurfaceVariant,
                ),
            )
        }
    }
}

/** Helper to access primary color from NeoFintechColorSet. */
private fun themeColorPrimary(colors: NeoFintechColorSet): androidx.compose.ui.graphics.Color =
    colors.primaryContainer

// ── DebtEditorDialog ──────────────────────────────────────────────────────────

@Composable
private fun DebtEditorDialog(
    debt: EventDebtItem,
    profile: ProfileItem?,
    onDismiss: () -> Unit,
    onSave: (EventDebtItem) -> Unit,
    onRemove: () -> Unit,
) {
    var amountText by remember(debt.id) { mutableStateOf(if (debt.amountEuros == 0.0) "" else debt.amountEuros.toString()) }
    var notesText by remember(debt.id) { mutableStateOf(debt.notes) }
    var validationMessage by remember(debt.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Deuda de ${profile?.name ?: "perfil"}")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Importe (€)") },
                    supportingText = { Text("Admite decimales, por ejemplo 12,50") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notas") },
                    supportingText = { Text("Opcional") }
                )
                validationMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedAmount = if (amountText.isBlank()) 0.0 else parseEuroAmount(amountText)
                    when {
                        parsedAmount == null -> {
                            validationMessage = "Introduce un importe válido en euros."
                            return@TextButton
                        }
                        parsedAmount < 0.0 -> {
                            validationMessage = "El importe no puede ser negativo."
                            return@TextButton
                        }
                        else -> {
                            validationMessage = null
                            onSave(
                                debt.copy(
                                    amountEuros = parsedAmount,
                                    notes = notesText.trim()
                                )
                            )
                        }
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRemove) {
                    Text("Quitar")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

// ── Utility functions ─────────────────────────────────────────────────────────

private fun parseDecimalValue(value: String): Double? = value
    .trim()
    .replace(',', '.')
    .toDoubleOrNull()
