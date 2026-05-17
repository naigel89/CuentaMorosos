package com.cuentamorosos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.currentDateText
import com.cuentamorosos.data.ReminderMessage
import com.cuentamorosos.model.EventAction
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.ExpenseCategory
import com.cuentamorosos.model.PermissionEngine
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.StateTransitionResult
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.formattedDate
import com.cuentamorosos.model.parseEventDate
import com.cuentamorosos.model.validation.EventValidator
import com.cuentamorosos.model.validation.ValidationError
import com.cuentamorosos.model.validation.allErrors
import com.cuentamorosos.model.validation.allWarnings
import com.cuentamorosos.model.validation.hasErrors
import com.cuentamorosos.model.validation.hasWarnings

// ── EventsScreen ──────────────────────────────────────────────────────────────

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    events: List<EventItem>,
    profiles: List<ProfileItem>,
    participantCountByEvent: Map<String, Int>,
    pendingTotalsByEvent: Map<String, Double>,
    totalSpent: Double,
    totalExpensesByEvent: Map<String, Double> = emptyMap(),
    yourShareByEvent: Map<String, Double> = emptyMap(),
    youAreOwedByEvent: Map<String, Double> = emptyMap(),
    expenseCountByEvent: Map<String, Int> = emptyMap(),
    reminders: List<ReminderMessage>,
    currentUserUid: String?,
    onOpenEvent: (EventItem) -> Unit,
    onSaveEvent: (EventItem) -> Unit,
    onDeleteEvent: (EventItem) -> Unit,
    transitionWarning: StateTransitionResult.AllowedWithWarning? = null,
    onConfirmTransition: (() -> Unit)? = null,
    onDismissWarning: (() -> Unit)? = null,
    validationErrors: List<String> = emptyList(),
    onClearValidationErrors: (() -> Unit)? = null,
    currentProfileId: String? = null,
) {
    var editableEvent by remember { mutableStateOf<EventItem?>(null) }
    var eventToDelete by remember { mutableStateOf<EventItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Filtro: 0 = Todos, 1 = Con deuda, 2 = Sin deuda
    var activeFilter by remember { mutableStateOf(0) }

    val totalPending by remember(pendingTotalsByEvent) {
        derivedStateOf { pendingTotalsByEvent.values.sum() }
    }
    val activeEventCount by remember(pendingTotalsByEvent) {
        derivedStateOf { pendingTotalsByEvent.count { it.value > 0.0 } }
    }
    val owedEventCount by remember(youAreOwedByEvent) {
        derivedStateOf { youAreOwedByEvent.count { it.value > 0.0 } }
    }

    val filteredEvents by remember(events, searchQuery, activeFilter, pendingTotalsByEvent) {
        derivedStateOf {
            events
                .filter { event ->
                    searchQuery.isBlank() ||
                        event.name.contains(searchQuery.trim(), ignoreCase = true)
                }
                .filter { event ->
                    when (activeFilter) {
                        1 -> (pendingTotalsByEvent[event.id] ?: 0.0) > 0.0
                        2 -> (pendingTotalsByEvent[event.id] ?: 0.0) == 0.0
                        else -> true
                    }
                }
        }
    }

    val filterLabel = when (activeFilter) {
        1 -> "con deuda"
        2 -> "sin deuda"
        else -> ""
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        val gridColumns = if (isWide) GridCells.Fixed(2) else GridCells.Fixed(1)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(NeoFintechSpacing.md),
        ) {
            // Compact Balance Summary
            BalanceSummaryCard(
                totalPending = totalPending,
                activeEventCount = activeEventCount,
                totalSpent = totalSpent,
                owedEventCount = owedEventCount,
            )

            // Header row: title + create button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = NeoFintechSpacing.sm, bottom = NeoFintechSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Eventos",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                OutlinedButton(onClick = {
                    val uid = currentUserUid ?: ""
                    editableEvent = EventItem(
                        name = "",
                        dateMillis = currentTimeMillis(),
                        ownerId = uid,
                        creatorId = uid,
                        memberIds = listOfNotNull(uid.takeIf { it.isNotBlank() }),
                        participants = listOfNotNull(
                            uid.takeIf { it.isNotBlank() }?.let {
                                EventParticipant(profileId = it, role = EventRole.OWNER, joinedAtMillis = currentTimeMillis())
                            }
                        )
                    )
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Crear nuevo evento")
                }
            }

            // Search row (full width)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = NeoFintechSpacing.sm, bottom = NeoFintechSpacing.md),
                label = { Text("Buscar evento") },
                singleLine = true,
                shape = NeoFintechShapes.md,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
            )

            // Filter chips row (separate row below search)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Todos", "Con deuda", "Sin deuda").forEachIndexed { index, label ->
                    FilterChip(
                        selected = activeFilter == index,
                        onClick = { activeFilter = index },
                        label = { Text(label) },
                    )
                }
            }

            // Events grid
            Box(modifier = Modifier.weight(1f)) {
                if (events.isEmpty()) {
                    EmptyStateMessage(
                        title = "No tenés eventos aún",
                        message = "Pulsa en \"+ Nuevo perfil\" para registrar el primero con nombre y fecha.",
                    )
                } else if (filteredEvents.isEmpty()) {
                    EmptyStateMessage(
                        title = "Sin resultados",
                        message = if (searchQuery.isNotBlank())
                            "No se encontraron eventos para '$searchQuery'."
                        else
                            "No hay eventos $filterLabel.",
                        onClear = {
                            if (searchQuery.isNotBlank()) searchQuery = ""
                            else activeFilter = 0
                        },
                        clearLabel = if (searchQuery.isNotBlank()) "Limpiar búsqueda" else "Resetear filtro",
                    )
                } else {
                    LazyVerticalGrid(
                        columns = gridColumns,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(NeoFintechSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(NeoFintechSpacing.md),
                    ) {
                        items(filteredEvents, key = { it.id }) { event ->
                            val eventProfiles = event.effectiveMemberIds.mapNotNull { memberId ->
                                profiles.find { it.id == memberId }
                            }
                            val profileId = currentProfileId ?: ""
                            val role = PermissionEngine.getRole(profileId, event)
                            val canEdit = role == EventRole.OWNER
                            val canDelete = PermissionEngine.hasPermission(role, EventAction.DeleteEvent)
                            EventCard(
                                event = event,
                                participantCount = participantCountByEvent[event.id] ?: 0,
                                pendingTotal = pendingTotalsByEvent[event.id] ?: 0.0,
                                totalExpense = totalExpensesByEvent[event.id] ?: 0.0,
                                yourShare = yourShareByEvent[event.id] ?: 0.0,
                                youAreOwed = youAreOwedByEvent[event.id] ?: 0.0,
                                profiles = eventProfiles,
                                category = ExpenseCategory.fromId("shared"),
                                statusLabel = if ((pendingTotalsByEvent[event.id] ?: 0.0) > 0.0) "Activo" else "Saldado",
                                onTap = { onOpenEvent(event) },
                                onEdit = { editableEvent = event },
                                onDelete = { eventToDelete = event },
                                canEdit = canEdit,
                                canDelete = canDelete,
                            )
                        }
                    }
                }
            }
        }
    }

    // ── EventEditorDialog ─────────────────────────────────────────────────
    editableEvent?.let { event ->
        val itemCount = expenseCountByEvent[event.id] ?: 0
        EventEditorDialog(
            initialEvent = event,
            itemCount = itemCount,
            onDismiss = { editableEvent = null },
            onSave = { savedEvent ->
                editableEvent = null
                onSaveEvent(savedEvent)
            }
        )
    }

    // ── Delete Confirmation ───────────────────────────────────────────────
    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Eliminar evento") },
            text = {
                Text("¿Seguro que quieres eliminar \"${event.name}\"? Se borrarán también todas sus deudas y gastos. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    eventToDelete = null
                    onDeleteEvent(event)
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Transition warning confirmation
    if (transitionWarning != null) {
        AlertDialog(
            onDismissRequest = { onDismissWarning?.invoke() },
            title = { Text("Confirmar acción") },
            text = {
                Text(
                    "⚠ ${transitionWarning.warning}\n\n¿Querés continuar?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirmTransition?.invoke() }) {
                    Text("Confirmar", color = NeoFintechColors.dark().warning)
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismissWarning?.invoke() }) {
                    Text("Cancelar")
                }
            },
        )
    }

    // Validation errors dialog
    if (validationErrors.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { onClearValidationErrors?.invoke() },
            title = { Text("No se puede abrir el evento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    validationErrors.forEach { error ->
                        Text("• $error", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onClearValidationErrors?.invoke() }) {
                    Text("Entendido")
                }
            },
        )
    }
}

// ── EmptyStateMessage ────────────────────────────────────────────────────────

@Composable
private fun EmptyStateMessage(
    title: String,
    message: String,
    onClear: (() -> Unit)? = null,
    clearLabel: String = "Limpiar",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        onClear?.let {
            TextButton(onClick = it) {
                Text(clearLabel)
            }
        }
    }
}

// ── EventEditorDialog ─────────────────────────────────────────────────────────

@Composable
private fun EventEditorDialog(
    initialEvent: EventItem,
    itemCount: Int = 0,
    onDismiss: () -> Unit,
    onSave: (EventItem) -> Unit,
) {
    var name by remember(initialEvent.id) { mutableStateOf(initialEvent.name) }
    var dateText by remember(initialEvent.id) { mutableStateOf(initialEvent.formattedDate().ifBlank { currentDateText() }) }
    var validationErrors by remember(initialEvent.id) { mutableStateOf<List<ValidationError>>(emptyList()) }
    var validationWarnings by remember(initialEvent.id) { mutableStateOf<List<ValidationError>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialEvent.name.isBlank()) "Nuevo evento" else "Editar evento")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationErrors = emptyList()
                        validationWarnings = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del evento") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        validationErrors = emptyList()
                        validationWarnings = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Fecha") },
                    supportingText = { Text("Usa el formato dd/MM/yyyy") },
                    singleLine = true
                )
                if (validationErrors.isNotEmpty()) {
                    validationErrors.forEach { error ->
                        Text(
                            text = "• ${error.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (validationWarnings.isNotEmpty()) {
                    validationWarnings.forEach { warning ->
                        Text(
                            text = "⚠ ${warning.message}",
                            color = NeoFintechColors.dark().warning,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedDate = parseEventDate(dateText)
                    if (parsedDate == null) {
                        validationErrors = listOf(ValidationError("Selecciona una fecha válida", "date"))
                        return@TextButton
                    }

                    val draftEvent = initialEvent.copy(
                        name = name.trim(),
                        dateMillis = parsedDate
                    )
                    val result = EventValidator.validate(draftEvent, itemCount)

                    if (result.hasErrors()) {
                        validationErrors = result.allErrors()
                        validationWarnings = result.allWarnings()
                        return@TextButton
                    }

                    // Warnings are shown but save proceeds
                    validationErrors = emptyList()
                    validationWarnings = result.allWarnings()
                    onSave(draftEvent)
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
