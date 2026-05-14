package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
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
import com.cuentamorosos.isValidEmail
import com.cuentamorosos.data.ReminderMessage
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SplitMode
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.formattedDate
import com.cuentamorosos.model.parseEventDate

// ── EventsScreen ──────────────────────────────────────────────────────────────

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    events: List<EventItem>,
    profileCount: Int,
    participantCountByEvent: Map<String, Int>,
    pendingTotalsByEvent: Map<String, Double>,
    reminders: List<ReminderMessage>,
    currentUserUid: String?,
    onOpenEvent: (EventItem) -> Unit,
    onSaveEvent: (EventItem) -> Unit,
    onDeleteEvent: (EventItem) -> Unit,
) {
    var editableEvent by remember { mutableStateOf<EventItem?>(null) }
    var eventToDelete by remember { mutableStateOf<EventItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Filtro: 0 = Todos, 1 = Con deuda pendiente, 2 = Sin deuda
    var activeFilter by remember { mutableStateOf(0) }

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

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Eventos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Desde aquí ya puedes abrir cada evento y controlar deudas, pagos y reparto rápido.",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    editableEvent = EventItem(
                        name = "",
                        dateMillis = currentTimeMillis(),
                        ownerId = currentUserUid ?: "",
                        memberIds = listOfNotNull(currentUserUid?.takeIf { it.isNotBlank() })
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Nuevo evento")
            }
        }

        // ── Barra de búsqueda ─────────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar evento") },
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    TextButton(onClick = { searchQuery = "" }) { Text("✕") }
                }
            },
        )

        // ── Chips de filtro ───────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todos", "Con deuda", "Sin deuda").forEachIndexed { index, label ->
                FilterChip(
                    selected = activeFilter == index,
                    onClick = { activeFilter = index },
                    label = { Text(label) },
                )
            }
        }

        if (reminders.isNotEmpty()) {
            ReminderSummaryCard(reminders = reminders)
        }

        if (events.isEmpty()) {
            EmptyState(
                modifier = Modifier.weight(1f),
                title = "Todavía no hay eventos",
                message = "Pulsa en `Nuevo evento` para registrar el primero con nombre y fecha."
            )
        } else if (filteredEvents.isEmpty()) {
            EmptyState(
                modifier = Modifier.weight(1f),
                title = "Sin resultados",
                message = if (searchQuery.isNotBlank())
                    "No hay eventos que coincidan con «$searchQuery»."
                else
                    "No hay eventos que cumplan el filtro seleccionado.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredEvents, key = { it.id }) { event ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenEvent(event) }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = event.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Fecha: ${event.formattedDate()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Participantes: ${participantCountByEvent[event.id] ?: 0} · Pendiente activo: ${formatEuros(pendingTotalsByEvent[event.id] ?: 0.0)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            event.lastCalculationMode?.let { mode ->
                                val label = SplitMode.fromId(mode).label
                                val summary = event.lastCalculationSummary?.let { " · $it" }.orEmpty()
                                Text(
                                    text = "Último cálculo: $label · ${formatEuros(event.lastCalculationTotal ?: 0.0)}$summary",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenEvent(event) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Abrir evento")
                                }
                                OutlinedButton(
                                    onClick = { editableEvent = event },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Editar")
                                }
                                OutlinedButton(
                                    onClick = { eventToDelete = event },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Eliminar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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
                            validationMessage = "Indica un nombre para el evento."
                            return@TextButton
                        }
                        parsedDate == null -> {
                            validationMessage = "Introduce una fecha válida con formato dd/MM/yyyy."
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
