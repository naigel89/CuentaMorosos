package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.isValidEmail
import com.cuentamorosos.model.CalculationApplication
import com.cuentamorosos.model.CalculationPreview
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ExpenseCategory
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SettlementTransfer
import com.cuentamorosos.model.SplitMode
import com.cuentamorosos.model.buildCalculationPreview
import com.cuentamorosos.model.buildSettlementTransfers
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.formattedDate
import com.cuentamorosos.model.parseEuroAmount

// ── EventDetailScreen ─────────────────────────────────────────────────────────

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
    onApplyCalculation: (CalculationApplication) -> Unit,
    onInviteMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
) {
    val profileById = profiles.associateBy { it.id }
    val eventParticipants = eventDebts.mapNotNull { debt ->
        profileById[debt.profileId]?.let { profile -> debt to profile }
    }
    val pendingParticipants = eventParticipants.filter { !it.first.paid }
    val paidParticipants = eventParticipants.filter { it.first.paid }
    val pendingTotal = pendingParticipants.sumOf { it.first.amountEuros }
    val eventExpenseTotal = eventExpenses.sumOf { it.amountEuros }
    val availableProfiles = profiles.filter { profile ->
        eventDebts.none { it.profileId == profile.id }
    }

    var editableDebt by remember { mutableStateOf<EventDebtItem?>(null) }
    var editableExpense by remember { mutableStateOf<EventExpenseItem?>(null) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var showQuickSplitDialog by remember { mutableStateOf(false) }
    var showInviteMemberDialog by remember { mutableStateOf(false) }
    var showRemoveOwnerConfirm by remember { mutableStateOf<EventDebtItem?>(null) }
    var showRemoveOwnerFromMembersConfirm by remember { mutableStateOf(false) }
    val currentUid = currentUserUid ?: ""
    val isOwner = event.ownerId == currentUid

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Volver")
            }
            Text(
                text = event.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Fecha: ${event.formattedDate()}",
            style = MaterialTheme.typography.bodyMedium
        )

        StatusCard(
            title = "Resumen del evento",
            message = "Pendientes: ${pendingParticipants.size} · Han pagado: ${paidParticipants.size} · Total activo: ${formatEuros(pendingTotal)}"
        )

        event.lastCalculationMode?.let { mode ->
            val label = SplitMode.fromId(mode).label
            val summary = event.lastCalculationSummary?.let { " · $it" }.orEmpty()
            StatusCard(
                title = "Último cálculo aplicado",
                message = "Modo: $label · Total del evento: ${formatEuros(event.lastCalculationTotal ?: 0.0)}$summary"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddProfileDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Añadir perfil")
            }
            OutlinedButton(
                onClick = {
                    editableExpense = EventExpenseItem(
                        eventId = event.id,
                        name = "",
                        amountEuros = 0.0
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Añadir ítem")
            }
        }

        if (eventDebts.isNotEmpty()) {
            OutlinedButton(
                onClick = { showQuickSplitDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir calculadora")
            }
        }

        if (eventDebts.isNotEmpty() && eventDebts.all { it.amountEuros == 0.0 }) {
            SuggestionCard(
                message = "Todavía no hay importes asignados. Usa la calculadora para simular distintos repartos y aplicarlos cuando te convenzan."
            )
        }

        Text(
            text = "Ítems del evento",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (eventExpenses.isEmpty()) {
            SuggestionCard(
                message = "Añade gastos del evento con categoría y perfiles implicados para usar `consumo real` o `por categoría`."
            )
        } else {
            Text(
                text = "Total registrado en ítems: ${formatEuros(eventExpenseTotal)}",
                style = MaterialTheme.typography.bodySmall
            )
            eventExpenses.forEach { expense ->
                ExpenseCard(
                    expense = expense,
                    profiles = profiles,
                    onEdit = { editableExpense = expense }
                )
            }
        }

        Text(
            text = "Pendientes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (pendingParticipants.isEmpty()) {
            EmptyState(
                title = if (eventDebts.isEmpty()) "Evento sin perfiles" else "No hay pagos pendientes",
                message = if (eventDebts.isEmpty()) {
                    "Añade uno o más perfiles existentes para empezar a registrar importes y notas."
                } else {
                    "Todos los perfiles del evento están actualmente marcados como pagados."
                }
            )
        } else {
            pendingParticipants.forEach { (debt, profile) ->
                DebtCard(
                    profile = profile,
                    debt = debt,
                    onEdit = { editableDebt = debt },
                    onTogglePaid = { onTogglePaid(debt) }
                )
            }
        }

        if (paidParticipants.isNotEmpty()) {
            Text(
                text = "Han pagado",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            paidParticipants.forEach { (debt, profile) ->
                DebtCard(
                    profile = profile,
                    debt = debt,
                    onEdit = { editableDebt = debt },
                    onTogglePaid = { onTogglePaid(debt) }
                )
            }
        }

        // ── Miembros del evento ───────────────────────────────────────────────
        Text(
            text = "Miembros",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Button(
            onClick = { showInviteMemberDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Invitar miembro por email")
        }

        if (event.memberIds.isEmpty()) {
            SuggestionCard(message = "Aún no hay miembros en este evento.")
        } else {
            event.memberIds.forEach { uid ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val label = if (uid == event.ownerId) "$uid (propietario)" else uid
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (isOwner) {
                        if (uid == currentUid) {
                            TextButton(onClick = { showRemoveOwnerFromMembersConfirm = true }) {
                                Text("No participar", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            TextButton(onClick = { onRemoveMember(uid) }) {
                                Text("Expulsar", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showAddProfileDialog) {
        AddProfileToEventDialog(
            availableProfiles = availableProfiles,
            onDismiss = { showAddProfileDialog = false },
            onAddProfile = { profile ->
                showAddProfileDialog = false
                onAddProfileToEvent(profile)
            }
        )
    }

    editableExpense?.let { expense ->
        ExpenseEditorDialog(
            expense = expense,
            profiles = eventParticipants.map { it.second },
            onDismiss = {
                editableExpense = null
            },
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

    if (showRemoveOwnerFromMembersConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveOwnerFromMembersConfirm = false },
            title = { Text("Salir del reparto") },
            text = {
                Text("¿Seguro? No participarás en el reparto de este evento.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveOwnerFromMembersConfirm = false
                    onRemoveMember(currentUid)
                }) {
                    Text("Confirmar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveOwnerFromMembersConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showQuickSplitDialog) {
        QuickSplitDialog(
            profiles = eventParticipants.map { it.second },
            eventExpenses = eventExpenses,
            onDismiss = { showQuickSplitDialog = false },
            onApply = { calculation ->
                showQuickSplitDialog = false
                onApplyCalculation(calculation)
            }
        )
    }

    if (showInviteMemberDialog) {
        InviteMemberDialog(
            onDismiss = { showInviteMemberDialog = false },
            onInvite = { email ->
                showInviteMemberDialog = false
                onInviteMember(email)
            }
        )
    }
}

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
                            Text("${profile.icon} ${profile.name}")
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

// ── ExpenseCard ───────────────────────────────────────────────────────────────

@Composable
private fun ExpenseCard(
    expense: EventExpenseItem,
    profiles: List<ProfileItem>,
    onEdit: () -> Unit,
) {
    val category = ExpenseCategory.fromId(expense.category)
    val assignedNames = expense.assignedProfileIds
        .mapNotNull { profileId -> profiles.firstOrNull { it.id == profileId }?.name }
        .ifEmpty { listOf("Todos los perfiles") }
        .joinToString()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = expense.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${formatEuros(expense.amountEuros)} · ${category.label}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = assignedNames,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = onEdit) {
                Text("Editar ítem")
            }
        }
    }
}

// ── ExpenseEditorDialog ───────────────────────────────────────────────────────

@Composable
private fun ExpenseEditorDialog(
    expense: EventExpenseItem,
    profiles: List<ProfileItem>,
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
                            Text("${profile.icon} ${profile.name}")
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
                                profileWeights = normalizedWeights
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

// ── DebtCard ──────────────────────────────────────────────────────────────────

@Composable
private fun DebtCard(
    profile: ProfileItem,
    debt: EventDebtItem,
    onEdit: () -> Unit,
    onTogglePaid: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${profile.icon} ${profile.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatEuros(debt.amountEuros),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = debt.paid,
                        onCheckedChange = { onTogglePaid() }
                    )
                    Text(if (debt.paid) "Pagado" else "Pendiente")
                }
            }

            if (debt.notes.isNotBlank()) {
                Text(
                    text = debt.notes,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            debt.calculationMode?.let { mode ->
                Text(
                    text = "Origen del importe: $mode",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedButton(onClick = onEdit) {
                Text("Editar importe y notas")
            }
        }
    }
}

// ── QuickSplitDialog ──────────────────────────────────────────────────────────

@Composable
private fun QuickSplitDialog(
    profiles: List<ProfileItem>,
    eventExpenses: List<EventExpenseItem>,
    onDismiss: () -> Unit,
    onApply: (CalculationApplication) -> Unit,
) {
    val expenseTotal = eventExpenses.sumOf { it.amountEuros }
    var totalText by remember(eventExpenses) {
        mutableStateOf(if (eventExpenses.isEmpty()) "" else expenseTotal.toString())
    }
    var selectedModeId by remember { mutableStateOf(SplitMode.REAL_CONSUMPTION.id) }
    var showModeSelector by remember { mutableStateOf(false) }
    var percentageInputs by remember(profiles.size) { mutableStateOf(defaultPercentageInputs(profiles.size)) }

    val totalValue = parseEuroAmount(totalText)
    val selectedMode = SplitMode.fromId(selectedModeId)

    fun previewFor(mode: SplitMode): CalculationPreview {
        val rawInputs = when (mode) {
            SplitMode.REAL_CONSUMPTION -> List(profiles.size) { 1.0 }
            SplitMode.SIMPLE_AVG -> List(profiles.size) { 1.0 }
            SplitMode.BY_CATEGORY -> List(profiles.size) { 1.0 }
            SplitMode.CUSTOM_PERCENTAGE -> percentageInputs.map { parseDecimalValue(it) ?: Double.NaN }
        }

        if (rawInputs.any { it.isNaN() }) {
            return CalculationPreview(validationMessage = "Revisa los valores introducidos para este reparto.")
        }

        return buildCalculationPreview(
            total = totalValue ?: expenseTotal,
            mode = mode,
            inputs = rawInputs,
            participantIds = profiles.map { it.id },
            expenses = eventExpenses,
        )
    }

    val selectedPreview = if (totalValue != null || eventExpenses.isNotEmpty()) previewFor(selectedMode) else null
    val canApply = selectedPreview?.validationMessage == null && selectedPreview != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calculadora automática") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Simula el reparto en tiempo real y confirma solo cuando el resultado te encaje.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { totalText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Total del evento (€)") },
                    supportingText = {
                        if (eventExpenses.isNotEmpty()) {
                            Text("Sugerido según ítems: ${formatEuros(expenseTotal)}")
                        }
                    },
                    singleLine = true
                )

                Text(
                    text = "Modo de reparto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedMode.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { showModeSelector = !showModeSelector }) {
                        Text(if (showModeSelector) "Ocultar modos" else "Cambiar modo de reparto")
                    }
                }

                if (showModeSelector) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(SplitMode.SIMPLE_AVG, SplitMode.BY_CATEGORY).forEach { mode ->
                            if (mode != selectedMode) {
                                OutlinedButton(
                                    onClick = {
                                        selectedModeId = mode.id
                                        showModeSelector = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(mode.label)
                                }
                            }
                        }

                        Text(
                            text = "Avanzado",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        if (selectedMode != SplitMode.CUSTOM_PERCENTAGE) {
                            OutlinedButton(
                                onClick = {
                                    selectedModeId = SplitMode.CUSTOM_PERCENTAGE.id
                                    showModeSelector = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(SplitMode.CUSTOM_PERCENTAGE.label)
                            }
                        }
                    }
                }

                Text(
                    text = selectedMode.helperText,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Ej.: ${selectedMode.exampleText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (eventExpenses.isNotEmpty()) {
                    Text(
                        text = "Ítems disponibles",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    eventExpenses.forEach { expense ->
                        val category = ExpenseCategory.fromId(expense.category)
                        Text(
                            text = "• ${expense.name}: ${formatEuros(expense.amountEuros)} · ${category.label}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                when (selectedMode) {
                    SplitMode.CUSTOM_PERCENTAGE -> {
                        Text(
                            text = "Porcentaje por perfil",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        profiles.forEachIndexed { index, profile ->
                            ParameterInputRow(
                                profile = profile,
                                label = "%",
                                value = percentageInputs[index],
                                onValueChange = { value ->
                                    percentageInputs = percentageInputs.updateAt(index, value)
                                }
                            )
                        }
                    }

                    SplitMode.REAL_CONSUMPTION,
                    SplitMode.BY_CATEGORY,
                    SplitMode.SIMPLE_AVG -> {
                        Text(
                            text = when (selectedMode) {
                                SplitMode.REAL_CONSUMPTION -> "Usa los perfiles asignados en cada ítem guardado."
                                SplitMode.BY_CATEGORY -> "La categoría del ítem decide si se reparte entre todos, solo seleccionados o una sola persona."
                                else -> "No necesita parámetros adicionales."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                selectedPreview?.validationMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (selectedPreview != null && selectedPreview.validationMessage == null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Vista previa por perfil",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    profiles.zip(selectedPreview.amounts).forEach { (profile, amount) ->
                        Text("${profile.icon} ${profile.name}: ${formatEuros(amount)}")
                    }
                    Text(
                        text = "Resumen: ${selectedPreview.summary}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    CalculationComparisonCard(
                        total = totalValue ?: expenseTotal,
                        profiles = profiles,
                        eventExpenses = eventExpenses,
                        percentageInputs = percentageInputs,
                    )

                    SettlementSuggestionCard(
                        profiles = profiles,
                        amounts = selectedPreview.amounts
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedPreview != null && selectedPreview.validationMessage == null) {
                        onApply(
                            CalculationApplication(
                                total = selectedPreview.calculatedTotal,
                                mode = selectedMode,
                                amounts = selectedPreview.amounts,
                                summary = selectedPreview.summary
                            )
                        )
                    }
                },
                enabled = canApply
            ) {
                Text("Aplicar cálculo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ── ParameterInputRow ─────────────────────────────────────────────────────────

@Composable
private fun ParameterInputRow(
    profile: ProfileItem,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${profile.icon} ${profile.name}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text(label) },
            singleLine = true
        )
    }
}

// ── CalculationComparisonCard ─────────────────────────────────────────────────

@Composable
private fun CalculationComparisonCard(
    total: Double,
    profiles: List<ProfileItem>,
    eventExpenses: List<EventExpenseItem>,
    percentageInputs: List<String>,
) {
    fun previewFor(mode: SplitMode): CalculationPreview {
        val rawInputs = when (mode) {
            SplitMode.REAL_CONSUMPTION -> List(profiles.size) { 1.0 }
            SplitMode.SIMPLE_AVG -> List(profiles.size) { 1.0 }
            SplitMode.BY_CATEGORY -> List(profiles.size) { 1.0 }
            SplitMode.CUSTOM_PERCENTAGE -> percentageInputs.map { parseDecimalValue(it) ?: Double.NaN }
        }

        return if (rawInputs.any { it.isNaN() }) {
            CalculationPreview(validationMessage = "Requiere completar parámetros válidos.")
        } else {
            buildCalculationPreview(
                total = total,
                mode = mode,
                inputs = rawInputs,
                participantIds = profiles.map { it.id },
                expenses = eventExpenses,
            )
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Comparativa rápida",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            SplitMode.entries.forEach { mode ->
                val preview = previewFor(mode)
                val text = preview.validationMessage ?: preview.amounts.mapIndexed { index, amount ->
                    "${profiles[index].name}: ${formatEuros(amount)}"
                }.joinToString(separator = " · ")

                Text(
                    text = "${mode.label}: $text",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ── SettlementSuggestionCard ──────────────────────────────────────────────────

@Composable
private fun SettlementSuggestionCard(
    profiles: List<ProfileItem>,
    amounts: List<Double>,
) {
    val transfers = remember(profiles, amounts) {
        buildSettlementTransfers(
            profileNames = profiles.map { it.name },
            amounts = amounts,
        )
    }
    if (transfers.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Liquidación sugerida",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Transferencias mínimas necesarias: ${transfers.size}",
                style = MaterialTheme.typography.bodySmall
            )
            transfers.forEach { transfer ->
                Text(
                    text = "${transfer.fromName} → ${transfer.toName}: ${formatEuros(transfer.amount)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ── Utility functions ─────────────────────────────────────────────────────────

private fun parseDecimalValue(value: String): Double? = value
    .trim()
    .replace(',', '.')
    .toDoubleOrNull()

private fun defaultPercentageInputs(count: Int): List<String> {
    if (count <= 0) return emptyList()
    val base = 100 / count
    val remainder = 100 % count

    return List(count) { index ->
        (base + if (index == 0) remainder else 0).toString()
    }
}

private fun List<String>.updateAt(index: Int, value: String): List<String> = mapIndexed { currentIndex, currentValue ->
    if (currentIndex == index) value else currentValue
}
