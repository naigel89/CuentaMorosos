package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onOpenEvent: (() -> Unit)? = null,
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
                onOpenEvent = onOpenEvent,
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

    // Dialog 2: ExpenseEditorDialog
    editableExpense?.let { expense ->
        ExpenseEditorDialog(
            expense = expense,
            profiles = eventParticipants,
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
        )
    }

    // Dialog 6: InviteMemberDialog
    if (showInviteMemberDialog && canDo(EventAction.ManageParticipants)) {
        InviteMemberDialog(
            onDismiss = { showInviteMemberDialog = false },
            onInvite = { email ->
                showInviteMemberDialog = false
                onInviteMember(email)
            }
        )
    }

    // Dialog 7: RemoveParticipantConfirmation
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
    onOpenEvent: (() -> Unit)? = null,
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

        // "Abrir evento" button — visible only for DRAFT events with OWNER/CONTRIBUTOR role
        if (event.state == EventState.DRAFT &&
            (currentRole == EventRole.OWNER || currentRole == EventRole.CONTRIBUTOR) &&
            onOpenEvent != null
        ) {
            Button(
                onClick = onOpenEvent,
                modifier = Modifier.padding(top = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                ),
                shape = NeoFintechShapes.lg,
            ) {
                Text("Abrir evento", fontWeight = FontWeight.Bold)
            }
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

// ── ExpenseEditorDialog ───────────────────────────────────────────────────────

@Composable
private fun ExpenseEditorDialog(
    expense: EventExpenseItem,
    profiles: List<ProfileItem>,
    effectiveMemberIds: List<String>,
    currentUid: String,
    onDismiss: () -> Unit,
    onSave: (EventExpenseItem) -> Unit,
    onRemove: () -> Unit,
) {
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

    fun updateSelectedProfiles(newSelection: List<String>) {
        selectedProfileIds = newSelection
        selectedWeights = selectedWeights.filterKeys { it in newSelection }
        validationMessage = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense.name.isBlank()) "Nuevo ítem del evento" else "Editar ítem del evento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del gasto") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Importe (€)") },
                    singleLine = true
                )

                // Paid by selector
                Text(
                    text = "Pagado por",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // "Yo" option
                    val isMeSelected = selectedPaidByProfileId == currentUid
                    Button(
                        onClick = { selectedPaidByProfileId = currentUid },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMeSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isMeSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text("Yo")
                    }
                    profiles.forEach { profile ->
                        OutlinedButton(
                            onClick = { selectedPaidByProfileId = profile.id },
                            modifier = Modifier.weight(1f),
                        ) {
                            ProfileAvatar(name = profile.name, emoji = profile.icon, photoUrl = profile.photoUrl, size = 24.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(profile.name)
                        }
                    }
                }

                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                ExpenseCategory.entries.forEach { category ->
                    if (selectedCategoryId == category.id) {
                        Button(
                            onClick = {
                                selectedCategoryId = category.id
                                if (category != ExpenseCategory.SHARED) {
                                    showCustomSplit = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${category.iconEmoji} ${category.label}")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                selectedCategoryId = category.id
                                if (category != ExpenseCategory.SHARED) {
                                    showCustomSplit = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${category.iconEmoji} ${category.label}")
                        }
                    }
                }
                Text(
                    text = "Categoría: ${ExpenseCategory.fromId(selectedCategoryId).label}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (profiles.isNotEmpty()) {
                    Text(
                        text = "Perfiles implicados",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    profiles.forEach { profile ->
                        val isChecked = selectedProfileIds.contains(profile.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSelection = if (isChecked) {
                                        selectedProfileIds - profile.id
                                    } else {
                                        selectedProfileIds + profile.id
                                    }
                                    updateSelectedProfiles(newSelection)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    val newSelection = if (isChecked) {
                                        selectedProfileIds - profile.id
                                    } else {
                                        selectedProfileIds + profile.id
                                    }
                                    updateSelectedProfiles(newSelection)
                                }
                            )
                            ProfileAvatar(name = profile.name, emoji = profile.icon, photoUrl = profile.photoUrl, size = 24.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(profile.name)
                        }
                    }

                    if (selectedCategoryId != ExpenseCategory.SHARED.id && selectedProfileIds.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                showCustomSplit = !showCustomSplit
                                validationMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showCustomSplit) {
                                    "Ocultar reparto personalizado"
                                } else {
                                    "Reparto personalizado (opcional)"
                                }
                            )
                        }
                    }

                    if (
                        selectedCategoryId != ExpenseCategory.SHARED.id &&
                        selectedProfileIds.isNotEmpty() &&
                        showCustomSplit
                    ) {
                        Text(
                            text = "Reparto personalizado (%)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        var totalWeight = 0.0
                        selectedProfileIds.forEach { profileId ->
                            val profile = profiles.find { it.id == profileId }
                            val weightText = selectedWeights[profileId] ?: "0"
                            totalWeight += parseDecimalValue(weightText) ?: 0.0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${profile?.icon ?: ""} ${profile?.name ?: "Perfil"}",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedTextField(
                                    value = weightText,
                                    onValueChange = {
                                        selectedWeights = selectedWeights + (profileId to it)
                                        validationMessage = null
                                    },
                                    modifier = Modifier.width(80.dp),
                                    label = { Text("%") },
                                    singleLine = true
                                )
                            }
                        }
                        Text(
                            text = "Total: ${String.format("%.2f", totalWeight)}% (debe ser 100%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (kotlin.math.abs(totalWeight - 100.0) < 0.01) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

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

                        onSave(
                            expense.copy(
                                name = name.trim(),
                                amountEuros = parsedAmount,
                                category = selectedCategory.id,
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
                        )
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (expense.name.isNotBlank() || expense.amountEuros != 0.0) {
                    TextButton(onClick = onRemove) {
                        Text("Quitar")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

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
