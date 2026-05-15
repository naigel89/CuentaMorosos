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
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ExpenseCategory
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros
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
    totalExpensesByEvent: Map<String, Double> = emptyMap(),
    yourShareByEvent: Map<String, Double> = emptyMap(),
    youAreOwedByEvent: Map<String, Double> = emptyMap(),
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
                    editableEvent = EventItem(
                        name = "",
                        dateMillis = currentTimeMillis(),
                        ownerId = currentUserUid ?: "",
                        memberIds = listOfNotNull(currentUserUid?.takeIf { it.isNotBlank() })
                    )
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Crear nuevo evento")
                }
            }

            // Search + filter row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Buscar") },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                )
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
                        message = "Pulsa en \"Create Event\" para registrar el primero con nombre y fecha.",
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
                            val eventProfiles = event.memberIds.mapNotNull { memberId ->
                                profiles.find { it.id == memberId }
                            }
                            EventCard(
                                event = event,
                                participantCount = participantCountByEvent[event.id] ?: 0,
                                pendingTotal = pendingTotalsByEvent[event.id] ?: 0.0,
                                totalExpense = totalExpensesByEvent[event.id] ?: 0.0,
                                yourShare = yourShareByEvent[event.id] ?: 0.0,
                                youAreOwed = youAreOwedByEvent[event.id] ?: 0.0,
                                profiles = eventProfiles,
                                category = ExpenseCategory.fromId("shared"),
                                statusLabel = if ((pendingTotalsByEvent[event.id] ?: 0.0) > 0.0) "Active" else "Settled",
                                onTap = { onOpenEvent(event) },
                                onEdit = { editableEvent = event },
                                onDelete = { eventToDelete = event },
                            )
                        }
                    }
                }
            }
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
