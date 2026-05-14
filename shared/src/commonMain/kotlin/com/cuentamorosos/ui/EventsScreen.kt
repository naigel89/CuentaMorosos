package com.cuentamorosos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formattedDate
import com.cuentamorosos.model.parseEventDate

// ── EventsScreen ──────────────────────────────────────────────────────────────

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    events: List<EventItem>,
    profiles: List<ProfileItem>,
    participantCountByEvent: Map<String, Int>,
    pendingTotalsByEvent: Map<String, Double>,
    totalSpent: Double,
    reminders: List<ReminderMessage>,
    currentUserUid: String?,
    onOpenEvent: (EventItem) -> Unit,
    onSaveEvent: (EventItem) -> Unit,
    onDeleteEvent: (EventItem) -> Unit,
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

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { BalanceSummaryCard(totalPending, activeEventCount, totalSpent) }
            item { CreateEventCard(onCreate = {
                editableEvent = EventItem(
                    name = "",
                    dateMillis = currentTimeMillis(),
                    ownerId = currentUserUid ?: "",
                    memberIds = listOfNotNull(currentUserUid?.takeIf { it.isNotBlank() })
                )
            }) }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar evento") },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todos", "Con deuda", "Sin deuda").forEachIndexed { index, label ->
                        FilterChip(
                            selected = activeFilter == index,
                            onClick = { activeFilter = index },
                            label = { Text(label) },
                        )
                    }
                }
            }
            if (reminders.isNotEmpty()) {
                item { ReminderSummaryCard(reminders = reminders) }
            }

            if (events.isEmpty()) {
                item {
                    EmptyStateMessage(
                        title = "No tenés eventos aún",
                        message = "Pulsa en \"Crear nuevo evento\" para registrar el primero con nombre y fecha.",
                    )
                }
            } else if (filteredEvents.isEmpty()) {
                item {
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
                }
            } else {
                items(filteredEvents, key = { it.id }) { event ->
                    val eventProfiles = event.memberIds.mapNotNull { memberId ->
                        profiles.find { it.id == memberId }
                    }
                    EventCard(
                        event = event,
                        participantCount = participantCountByEvent[event.id] ?: 0,
                        pendingTotal = pendingTotalsByEvent[event.id] ?: 0.0,
                        profiles = eventProfiles,
                        onTap = { onOpenEvent(event) },
                        onEdit = { editableEvent = event },
                        onDelete = { eventToDelete = event },
                    )
                }
            }
        }

        // FAB for quick event creation
        FloatingActionButton(
            onClick = {
                editableEvent = EventItem(
                    name = "",
                    dateMillis = currentTimeMillis(),
                    ownerId = currentUserUid ?: "",
                    memberIds = listOfNotNull(currentUserUid?.takeIf { it.isNotBlank() })
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = NeoFintechColors.dark().primaryContainer,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Crear evento")
        }
    }

    // ── EventEditorDialog ─────────────────────────────────────────────────
    editableEvent?.let { event ->
        EventEditorDialog(
            initialEvent = event,
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
    onDismiss: () -> Unit,
    onSave: (EventItem) -> Unit,
) {
    var name by remember(initialEvent.id) { mutableStateOf(initialEvent.name) }
    var dateText by remember(initialEvent.id) { mutableStateOf(initialEvent.formattedDate().ifBlank { currentDateText() }) }
    var validationMessage by remember(initialEvent.id) { mutableStateOf<String?>(null) }

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
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del evento") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Fecha") },
                    supportingText = { Text("Usa el formato dd/MM/yyyy") },
                    singleLine = true
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
                    val parsedDate = parseEventDate(dateText)
                    when {
                        name.isBlank() -> {
                            validationMessage = "El nombre no puede estar vacío"
                            return@TextButton
                        }
                        parsedDate == null -> {
                            validationMessage = "Selecciona una fecha válida"
                            return@TextButton
                        }
                        else -> {
                            validationMessage = null
                            onSave(
                                initialEvent.copy(
                                    name = name.trim(),
                                    dateMillis = parsedDate
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
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
