package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.cuentamorosos.data.CuentaMorososLocalStore
import com.cuentamorosos.data.NotificationScheduler
import com.cuentamorosos.data.ReminderMessage
import com.cuentamorosos.data.ReminderService
import com.cuentamorosos.data.ReminderWorker
import com.cuentamorosos.model.CalculationApplication
import com.cuentamorosos.model.CalculationPreview
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ExpenseCategory
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SettlementTransfer
import com.cuentamorosos.model.SplitMode
import com.cuentamorosos.model.UserPreferences
import com.cuentamorosos.model.buildCalculationPreview
import com.cuentamorosos.model.buildSettlementTransfers
import com.cuentamorosos.model.currentDateText
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.formattedDate
import com.cuentamorosos.model.parseEventDate
import com.cuentamorosos.model.parseEuroAmount
import com.google.firebase.auth.FirebaseAuth
import com.cuentamorosos.model.EventInvitation
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

private enum class MainSection(val title: String, val emoji: String) {
    EVENTS("Eventos", "📅"),
    CALENDAR("Calendario", "🗓️"),
    PROFILES("Perfiles", "👤"),
    INVITATIONS("Invitaciones", "✉️"),
    SETTINGS("Ajustes", "🎨")
}

@Composable
fun CuentaMorososApp(store: CuentaMorososLocalStore, onSignOut: (() -> Unit)? = null) {
    val context = LocalContext.current
    
    val factory = remember(context) { AppViewModelFactory(context.applicationContext) }
    val eventsViewModel: EventsViewModel = viewModel(factory = factory)
    val eventDetailViewModel: EventDetailViewModel = viewModel(factory = factory)
    val profilesViewModel: ProfilesViewModel = viewModel(factory = factory)
    val invitationsViewModel: InvitationsViewModel = viewModel(factory = factory)
    
    val events by eventsViewModel.events.collectAsState()
    val profiles by profilesViewModel.profiles.collectAsState()
    val pendingInvitations by invitationsViewModel.pendingInvitations.collectAsState()
    
    val eventId by eventDetailViewModel.eventId.collectAsState()
    val debts by eventDetailViewModel.debts.collectAsState(initial = emptyList())
    val expenses by eventDetailViewModel.expenses.collectAsState(initial = emptyList())
    
    var preferences by remember { mutableStateOf(store.loadPreferences()) }
    var currentSection by rememberSaveable { mutableStateOf(MainSection.EVENTS.name) }
    val snackbarHostState = remember { SnackbarHostState() }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    fun persistData() {
        store.savePreferences(preferences)
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            feedbackMessage = null
        }
    }

    val activeTotalsByProfile by remember(debts) {
        derivedStateOf {
            debts
                .filter { !it.paid }
                .groupBy { it.profileId }
                .mapValues { (_, values) -> values.sumOf { it.amountEuros } }
        }
    }

    val pendingTotalsByEvent by remember(debts) {
        derivedStateOf {
            debts
                .filter { !it.paid }
                .groupBy { it.eventId }
                .mapValues { (_, values) -> values.sumOf { it.amountEuros } }
        }
    }

    val participantCountByEvent by remember(debts) {
        derivedStateOf {
            debts.groupBy { it.eventId }.mapValues { it.value.size }
        }
    }

    val pendingEventsByProfile by remember(debts, events) {
        derivedStateOf {
            debts
                .filter { !it.paid }
                .groupBy { it.profileId }
                .mapValues { (_, values) ->
                    values.mapNotNull { debt ->
                        events.firstOrNull { it.id == debt.eventId }?.let { event ->
                            "${event.name} · ${formatEuros(debt.amountEuros)}"
                        }
                    }
                }
        }
    }

    val selectedEvent by remember(events, eventId) {
        derivedStateOf {
            events.firstOrNull { it.id == eventId }
        }
    }

    val reminderMessages by remember(events, debts, expenses, preferences) {
        derivedStateOf {
            ReminderService.buildReminderMessages(
                events = events,
                debts = debts,
                expenses = expenses,
                reminderDays = preferences.reminderDays,
                remindersEnabled = preferences.remindersEnabled,
            )
        }
    }

    CuentaMorososTheme(preferences = preferences) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (selectedEvent == null) {
                        NavigationBar {
                            MainSection.entries.forEach { section ->
                                NavigationBarItem(
                                    selected = currentSection == section.name,
                                    onClick = { currentSection = section.name },
                                    icon = { Text(text = section.emoji) },
                                    label = { Text(text = section.title) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                val event = selectedEvent
                if (event != null) {
                    EventDetailScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                event = event,
                profiles = profiles.toList(),
                eventDebts = debts.filter { it.eventId == event.id },
                eventExpenses = expenses.filter { it.eventId == event.id },
                                onBack = { eventDetailViewModel.setEventId(null) },
                                onAddProfileToEvent = { profile ->
                                    if (debts.none { it.eventId == event.id && it.profileId == profile.id }) {
                                        eventDetailViewModel.saveDebt(
                                            EventDebtItem(
                                                eventId = event.id,
                                                profileId = profile.id
                                            )
                                        )
                                        persistData()
                                        feedbackMessage = "Perfil añadido al evento."
                                    }
                                },
                                onSaveDebt = { debt ->
                                    eventDetailViewModel.saveDebt(debt)
                                    persistData()
                                    feedbackMessage = "Importe y notas actualizados."
                                },
                                onTogglePaid = { debt ->
                                    eventDetailViewModel.saveDebt(
                                        debt.copy(paid = !debt.paid)
                                    )
                                    persistData()
                                    feedbackMessage = if (debt.paid) {
                                        "El perfil vuelve a pendientes."
                                    } else {
                                        "Perfil movido a Han pagado."
                                    }
                                },
                                onRemoveDebt = { debtId ->
                                    eventDetailViewModel.deleteDebt(event.id, debtId)
                                    persistData()
                                    feedbackMessage = "Perfil eliminado del evento."
                                },
                                onSaveExpense = { expense ->
                                    eventDetailViewModel.saveExpense(expense)
                                    persistData()
                                    feedbackMessage = "Ítem del evento guardado."
                                },
                                onRemoveExpense = { expenseId ->
                                    eventDetailViewModel.deleteExpense(event.id, expenseId)
                                    persistData()
                                    feedbackMessage = "Ítem eliminado del evento."
                                },
                                 onApplyCalculation = { calculation ->
                                     val eventEntries = debts.filter { it.eventId == event.id }

                                     eventEntries.forEachIndexed { index, debt ->
                                         eventDetailViewModel.saveDebt(
                                             debt.copy(
                                                 amountEuros = calculation.amounts.getOrElse(index) { 0.0 },
                                                 calculationMode = calculation.mode.id
                                             )
                                         )
                                     }

                                     eventsViewModel.saveEvent(
                                         event.copy(
                                             lastCalculationMode = calculation.mode.id,
                                             lastCalculationTotal = calculation.total,
                                             lastCalculationTimestamp = System.currentTimeMillis(),
                                             lastCalculationSummary = calculation.summary
                                         )
                                     )
                                     persistData()
                                     feedbackMessage = "Cálculo ${calculation.mode.label} aplicado al evento."
                                 },
                                 onInviteMember = { email ->
                                     val currentUser = FirebaseAuth.getInstance().currentUser
                                     if (currentUser != null) {
                                         invitationsViewModel.sendInvitation(
                                             EventInvitation(
                                                 eventId = event.id,
                                                 eventName = event.name,
                                                 invitedByUid = currentUser.uid,
                                                 invitedByEmail = currentUser.email ?: "",
                                                 invitedEmail = email,
                                             )
                                         )
                                         feedbackMessage = "Invitación enviada a $email."
                                     }
                                 },
                                 onRemoveMember = { uid ->
                                     eventsViewModel.removeMember(event.id, uid)
                                     feedbackMessage = "Miembro eliminado del evento."
                                 }

            )
        } else {
            when (MainSection.valueOf(currentSection)) {
                MainSection.EVENTS -> EventsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    events = events,
                    profileCount = profiles.size,
                    participantCountByEvent = participantCountByEvent,
                    pendingTotalsByEvent = pendingTotalsByEvent,
                    reminders = reminderMessages,
                                onOpenEvent = { event ->
                                    eventDetailViewModel.setEventId(event.id)
                                },
                                onSaveEvent = { event ->
                                    eventsViewModel.saveEvent(event)
                                    persistData()
                                    feedbackMessage = "Evento guardado correctamente."
                                },
                                onDeleteEvent = { event ->
                                    eventsViewModel.deleteEvent(event.id)
                                    persistData()
                                    feedbackMessage = "Evento \"${event.name}\" eliminado."
                                }

                )

                MainSection.CALENDAR -> CalendarScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    events = events.toList(),
                    pendingTotalsByEvent = pendingTotalsByEvent,
                    onOpenEvent = { event -> eventDetailViewModel.setEventId(event.id) }
                )

                MainSection.PROFILES -> ProfilesScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    profiles = profiles.map { profile ->
                        profile.copy(totalPendingEuros = activeTotalsByProfile[profile.id] ?: 0.0)
                    },
                    currentUid = FirebaseAuth.getInstance().currentUser?.uid,
                    eventCount = events.size,
                    pendingEventsByProfile = pendingEventsByProfile,
                    onSaveProfile = { profile ->
                        profilesViewModel.saveProfile(profile)
                        feedbackMessage = "Perfil guardado correctamente."
                    },
                    onDeleteProfile = { profile ->
                        profilesViewModel.deleteProfile(profile)
                        feedbackMessage = "Perfil \"${profile.name}\" eliminado."
                    }
                )

                MainSection.INVITATIONS -> InvitationsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    invitations = pendingInvitations,
                    onAccept = { invitation -> invitationsViewModel.acceptInvitation(invitation) },
                    onReject = { invitation -> invitationsViewModel.rejectInvitation(invitation.id) },
                )

                MainSection.SETTINGS -> SettingsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    preferences = preferences,
                    reminders = reminderMessages,
                    onSavePreferences = { updatedPreferences ->
                        preferences = updatedPreferences
                        persistData()
                        if (updatedPreferences.remindersEnabled) {
                            ReminderWorker.schedule(context)
                        } else {
                            ReminderWorker.cancel(context)
                        }
                        feedbackMessage = "Preferencias actualizadas."
                    },
                    onSignOut = onSignOut
                )
            }
        }
    }
}
}
}

// ── T3-04: InviteMemberDialog ─────────────────────────────────────────────────

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
                if (trimmed.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
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

// ── T3-05: InvitationsScreen ──────────────────────────────────────────────────

@Composable
private fun InvitationsScreen(
    modifier: Modifier = Modifier,
    invitations: List<EventInvitation>,
    onAccept: (EventInvitation) -> Unit,
    onReject: (EventInvitation) -> Unit,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Invitaciones",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Invitaciones pendientes para unirte a eventos de otros usuarios.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (invitations.isEmpty()) {
            EmptyState(
                modifier = Modifier.weight(1f),
                title = "Sin invitaciones pendientes",
                message = "Cuando alguien te invite a un evento aparecerá aquí para que puedas aceptarla o rechazarla."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(invitations, key = { it.id }) { invitation ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = invitation.eventName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Invitado por: ${invitation.invitedByEmail}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onAccept(invitation) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Aceptar")
                                }
                                OutlinedButton(
                                    onClick = { onReject(invitation) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Rechazar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsScreen(
    modifier: Modifier = Modifier,
    events: List<EventItem>,
    profileCount: Int,
    participantCountByEvent: Map<String, Int>,
    pendingTotalsByEvent: Map<String, Double>,
    reminders: List<ReminderMessage>,
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
                                        dateMillis = System.currentTimeMillis(),
                                        ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                        memberIds = listOf(FirebaseAuth.getInstance().currentUser?.uid ?: "").filter { it.isNotBlank() }
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

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: UserPreferences,
    reminders: List<ReminderMessage>,
    onSavePreferences: (UserPreferences) -> Unit,
    onSignOut: (() -> Unit)? = null,
) {
    var selectedThemeMode by remember(preferences.themeMode) { mutableStateOf(preferences.themeMode) }
    var selectedAccentColor by remember(preferences.accentColorId) { mutableStateOf(preferences.accentColorId) }
    var reminderDaysText by remember(preferences.reminderDays) { mutableStateOf(preferences.reminderDays.toString()) }
    var remindersEnabled by remember(preferences.remindersEnabled) { mutableStateOf(preferences.remindersEnabled) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Ajustes y apariencia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Text(
                text = "Personaliza el tema visual y el comportamiento de los recordatorios del proyecto.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            StatusCard(
                title = "Tema",
                message = "Selecciona claro, oscuro o sistema y aplica un color destacado persistente."
            )
        }
        item {
            AppearancePreviewCard(
                preferences = UserPreferences(
                    themeMode = selectedThemeMode,
                    accentColorId = selectedAccentColor,
                    reminderDays = reminderDaysText.toIntOrNull() ?: preferences.reminderDays,
                    remindersEnabled = remindersEnabled,
                )
            )
        }
        item {
            Text(
                text = "Modo de tema",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeModeOption.entries.forEach { option ->
                    if (selectedThemeMode == option.id) {
                        Button(
                            onClick = { selectedThemeMode = option.id },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selectedThemeMode = option.id },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.label)
                        }
                    }
                }
            }
        }
        item {
            Text(
                text = "Color secundario",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentColorOption.entries.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { option ->
                            if (selectedAccentColor == option.id) {
                                Button(
                                    onClick = { selectedAccentColor = option.id },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(option.label)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedAccentColor = option.id },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(option.label)
                                }
                            }
                        }
                        if (rowOptions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            StatusCard(
                title = "Recordatorios",
                message = if (remindersEnabled) {
                    "Se avisará al detectar pagos pendientes o eventos incompletos tras los días configurados."
                } else {
                    "Los recordatorios están pausados."
                }
            )
        }
        item {
            OutlinedButton(
                onClick = { remindersEnabled = !remindersEnabled },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (remindersEnabled) "Desactivar recordatorios" else "Activar recordatorios")
            }
        }
        item {
            OutlinedTextField(
                value = reminderDaysText,
                onValueChange = {
                    reminderDaysText = it
                    validationMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Días para recordar") },
                supportingText = { Text("Ejemplo: 7") },
                singleLine = true
            )
        }
        validationMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            Button(
                onClick = {
                    val parsedDays = reminderDaysText.toIntOrNull()
                    validationMessage = when {
                        parsedDays == null -> "Introduce un número de días válido."
                        parsedDays <= 0 -> "Los días deben ser mayores que 0."
                        else -> null
                    }

                    if (validationMessage == null && parsedDays != null) {
                        onSavePreferences(
                            UserPreferences(
                                themeMode = selectedThemeMode,
                                accentColorId = selectedAccentColor,
                                reminderDays = parsedDays,
                                remindersEnabled = remindersEnabled,
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar preferencias")
            }
        }
        if (reminders.isNotEmpty()) {
            item {
                ReminderSummaryCard(reminders = reminders)
            }
        }
        item {
            OutlinedButton(
                onClick = { NotificationScheduler.postReminders(context, reminders) },
                modifier = Modifier.fillMaxWidth(),
                enabled = remindersEnabled && reminders.isNotEmpty()
            ) {
                Text(
                    if (reminders.isEmpty()) "Sin recordatorios activos"
                    else "Enviar recordatorios ahora (${reminders.size})"
                )
            }
        }
        if (onSignOut != null) {
            item {
                HorizontalDivider()
            }
            item {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}

@Composable
private fun ProfilesScreen(
    modifier: Modifier = Modifier,
    profiles: List<ProfileItem>,
    currentUid: String?,
    eventCount: Int,
    pendingEventsByProfile: Map<String, List<String>>,
    onSaveProfile: (ProfileItem) -> Unit,
    onDeleteProfile: (ProfileItem) -> Unit,
) {
    var selectedProfile by remember { mutableStateOf<ProfileItem?>(null) }
    var editableProfile by remember { mutableStateOf<ProfileItem?>(null) }
    var profileToDelete by remember { mutableStateOf<ProfileItem?>(null) }

    // Own profile always first, rest sorted alphabetically
    val sortedProfiles = remember(profiles, currentUid) {
        val own = profiles.filter { it.id == currentUid }
        val others = profiles.filter { it.id != currentUid }.sortedBy { it.name.lowercase() }
        own + others
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Perfiles",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "La deuda activa ya se recalcula por evento y excluye lo marcado como pagado.",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = {
                editableProfile = ProfileItem(
                    name = "",
                    icon = "🙂"
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nuevo perfil")
        }

        StatusCard(
            title = "Resumen global",
            message = "$eventCount eventos creados · totales pendientes listos para seguimiento."
        )

        if (profiles.isEmpty()) {
            EmptyState(
                modifier = Modifier.weight(1f),
                title = "Todavía no hay perfiles",
                message = "Añade personas con nombre e icono para reutilizarlas en distintos eventos."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedProfiles, key = { it.id }) { profile ->
                    val isOwnProfile = profile.id == currentUid
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProfile = profile },
                        colors = if (isOwnProfile) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) else CardDefaults.cardColors()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${profile.icon} ${profile.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOwnProfile) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                if (isOwnProfile) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "Tú",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Pendiente activo: ${formatEuros(profile.totalPendingEuros)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOwnProfile) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Eventos abiertos: ${pendingEventsByProfile[profile.id]?.size ?: 0}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOwnProfile) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (profile.isGhost) {
                                Text(
                                    text = "Perfil local (fantasma)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedProfile?.let { profile ->
        ProfileDetailDialog(
            profile = profile,
            pendingEvents = pendingEventsByProfile[profile.id].orEmpty(),
            onDismiss = { selectedProfile = null },
            onEdit = {
                selectedProfile = null
                editableProfile = profile
            },
            onDelete = {
                selectedProfile = null
                profileToDelete = profile
            }
        )
    }

    editableProfile?.let { profile ->
        ProfileEditorDialog(
            initialProfile = profile,
            onDismiss = { editableProfile = null },
            onSave = { savedProfile ->
                editableProfile = null
                onSaveProfile(savedProfile)
            }
        )
    }

    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Eliminar perfil") },
            text = {
                Text("¿Seguro que quieres eliminar \"${profile.icon} ${profile.name}\"? Se eliminarán también todas sus deudas en todos los eventos. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    profileToDelete = null
                    onDeleteProfile(profile)
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun EventDetailScreen(
    modifier: Modifier = Modifier,
    event: EventItem,
    profiles: List<ProfileItem>,
    eventDebts: List<EventDebtItem>,
    eventExpenses: List<EventExpenseItem>,
    onBack: () -> Unit,
    onAddProfileToEvent: (ProfileItem) -> Unit,
    onSaveDebt: (EventDebtItem) -> Unit,
    onTogglePaid: (EventDebtItem) -> Unit,
    onRemoveDebt: (String) -> Unit,
    onSaveExpense: (EventExpenseItem) -> Unit,
    onRemoveExpense: (String) -> Unit,
    onApplyCalculation: (CalculationApplication) -> Unit,
    onInviteMember: (email: String) -> Unit = {},
    onRemoveMember: (uid: String) -> Unit = {},
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
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
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

@Composable
private fun ProfileEditorDialog(
    initialProfile: ProfileItem,
    onDismiss: () -> Unit,
    onSave: (ProfileItem) -> Unit,
) {
    val iconOptions = listOf(
        "🙂", "😄", "😎", "🤩", "🥳", "😇",
        "🧑", "👩", "👨", "👴", "👵", "🧒",
        "💼", "🎓", "🏋️", "🎨", "🎸", "🎮",
        "🏠", "🚗", "✈️", "⚽", "🍕", "🎉",
        "🌟", "❤️", "🐶", "🐱", "🌿", "💡",
    )
    var name by remember(initialProfile.id) { mutableStateOf(initialProfile.name) }
    var selectedIcon by remember(initialProfile.id) { mutableStateOf(initialProfile.icon.ifBlank { "🙂" }) }
    var isGhost by remember(initialProfile.id) { mutableStateOf(initialProfile.isGhost) }
    var linkedEmail by remember(initialProfile.id) { mutableStateOf(initialProfile.linkedEmail.orEmpty()) }
    var validationMessage by remember(initialProfile.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialProfile.name.isBlank()) "Nuevo perfil" else "Editar perfil")
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
                    label = { Text("Nombre del perfil") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isGhost = !isGhost
                            if (!isGhost) {
                                linkedEmail = ""
                            }
                            validationMessage = null
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isGhost,
                        onCheckedChange = { checked ->
                            isGhost = checked
                            if (!checked) {
                                linkedEmail = ""
                            }
                            validationMessage = null
                        }
                    )
                    Text("Perfil local (sin cuenta Firebase)")
                }
                if (isGhost) {
                    OutlinedTextField(
                        value = linkedEmail,
                        onValueChange = {
                            linkedEmail = it.trim()
                            validationMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email para vincular (opcional)") },
                        singleLine = true
                    )
                    Text(
                        text = "Si este email se registra después, el perfil local se vinculará automáticamente.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "Selecciona un icono",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(iconOptions) { icon ->
                        val isSelected = icon == selectedIcon
                        if (isSelected) {
                            Button(
                                onClick = { selectedIcon = icon; validationMessage = null },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                modifier = Modifier.aspectRatio(1f)
                            ) {
                                Text(icon)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { selectedIcon = icon; validationMessage = null },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                modifier = Modifier.aspectRatio(1f)
                            ) {
                                Text(icon)
                            }
                        }
                    }
                }
                Text(
                    text = "Icono seleccionado: $selectedIcon",
                    style = MaterialTheme.typography.bodyMedium
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
                    if (name.isBlank()) {
                        validationMessage = "Indica un nombre para el perfil."
                        return@TextButton
                    }

                    val normalizedLinkedEmail = linkedEmail.trim().lowercase()
                    val invalidLinkedEmail = isGhost &&
                        normalizedLinkedEmail.isNotBlank() &&
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedLinkedEmail).matches()
                    if (invalidLinkedEmail) {
                        validationMessage = "El email de vinculación no es válido."
                        return@TextButton
                    }

                    validationMessage = null
                    run {
                        onSave(
                            initialProfile.copy(
                                name = name.trim(),
                                icon = selectedIcon,
                                isGhost = isGhost,
                                linkedEmail = normalizedLinkedEmail.takeIf { isGhost && it.isNotBlank() }
                            )
                        )
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
                                if (category != ExpenseCategory.SELECTED) {
                                    showCustomSplit = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(category.label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                selectedCategoryId = category.id
                                if (category != ExpenseCategory.SELECTED) {
                                    showCustomSplit = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(category.label)
                        }
                    }
                }
                Text(
                    text = ExpenseCategory.fromId(selectedCategoryId).helperText,
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
                    
                    if (selectedCategoryId == ExpenseCategory.SELECTED.id && selectedProfileIds.isNotEmpty()) {
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
                        selectedCategoryId == ExpenseCategory.SELECTED.id &&
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
                        selectedCategory == ExpenseCategory.SELECTED && showCustomSplit &&
                            selectedProfileIds.any { parseDecimalValue(selectedWeights[it] ?: "") == null } ->
                                "Revisa los porcentajes del reparto personalizado."
                        selectedCategory == ExpenseCategory.SELECTED && showCustomSplit &&
                            selectedProfileIds.any { (parseDecimalValue(selectedWeights[it] ?: "") ?: 0.0) < 0.0 } ->
                                "Los porcentajes no pueden ser negativos."
                        selectedCategory == ExpenseCategory.SELECTED && showCustomSplit &&
                            kotlin.math.abs(customWeightTotal - 100.0) >= 0.01 ->
                                "El reparto personalizado debe sumar 100%."
                        else -> null
                    }

                    if (validationMessage == null && parsedAmount != null) {
                        val normalizedWeights = if (
                            selectedCategory == ExpenseCategory.SELECTED &&
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
    // REAL_CONSUMPTION es el modo predeterminado: refleja directamente los ítems del evento.
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

                // Descripción + ejemplo del modo seleccionado
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

@Composable
private fun ProfileDetailDialog(
    profile: ProfileItem,
    pendingEvents: List<String>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${profile.icon} ${profile.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Total pendiente actual: ${formatEuros(profile.totalPendingEuros)}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                if (pendingEvents.isEmpty()) {
                    Text(
                        text = "No tiene deudas activas ahora mismo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Eventos pendientes:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    pendingEvents.forEach { summary ->
                        Text(summary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) {
                Text("Editar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    )
}

@Composable
private fun StatusCard(
    title: String,
    message: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SuggestionCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ReminderSummaryCard(reminders: List<ReminderMessage>) {
    var isVisible by remember { mutableStateOf(true) }
    if (!isVisible) return

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔔 Recordatorios activos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                TextButton(onClick = { isVisible = false }) {
                    Text("Ocultar", style = MaterialTheme.typography.labelSmall)
                }
            }
            reminders.take(5).forEach { reminder ->
                                Text(
                                    text = "• ${reminder.title}: ${reminder.body}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

            }
        }
    }
}

@Composable
private fun AppearancePreviewCard(preferences: UserPreferences) {
    val accent = AccentColorOption.fromId(preferences.accentColorId)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Vista previa de apariencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tema: ${ThemeModeOption.fromId(preferences.themeMode).label}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Color destacado: ${accent.label}",
                color = accent.color,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Recordatorios cada ${preferences.reminderDays} día(s)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

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

// ─────────────────────────────────────────────────────────────────────────────
// CalendarScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CalendarScreen(
    modifier: Modifier = Modifier,
    events: List<EventItem>,
    pendingTotalsByEvent: Map<String, Double>,
    onOpenEvent: (EventItem) -> Unit,
) {
    // Estado del mes mostrado: año + mes (1-based)
    val today = remember { Calendar.getInstance() }
    var displayYear by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableStateOf(today.get(Calendar.MONTH) + 1) } // 1-12
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    // Mapa día-del-mes → lista de eventos de ese mes/año
    val eventsByDay by remember(events, displayYear, displayMonth) {
        derivedStateOf {
            val map = mutableMapOf<Int, MutableList<EventItem>>()
            events.forEach { event ->
                val cal = Calendar.getInstance().apply { timeInMillis = event.dateMillis }
                if (cal.get(Calendar.YEAR) == displayYear &&
                    cal.get(Calendar.MONTH) + 1 == displayMonth
                ) {
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    map.getOrPut(day) { mutableListOf() }.add(event)
                }
            }
            map as Map<Int, List<EventItem>>
        }
    }

    // Eventos del día seleccionado
    val selectedDayEvents by remember(eventsByDay, selectedDay) {
        derivedStateOf {
            selectedDay?.let { eventsByDay[it] } ?: emptyList()
        }
    }

    // Calcular la cuadrícula del mes
    val calGrid by remember(displayYear, displayMonth) {
        derivedStateOf {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, displayYear)
                set(Calendar.MONTH, displayMonth - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            // 0=Dom, 1=Lun … ajustamos para semana europea (lunes primero)
            val firstDow = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            // Construimos lista de celdas: null = vacía, Int = día
            val cells = mutableListOf<Int?>()
            repeat(firstDow) { cells.add(null) }
            for (d in 1..daysInMonth) cells.add(d)
            // Rellenar hasta múltiplo de 7
            while (cells.size % 7 != 0) cells.add(null)
            cells
        }
    }

    val monthNames = remember {
        DateFormatSymbols(Locale("es", "ES")).months
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // ── Cabecera mes ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = {
                if (displayMonth == 1) { displayMonth = 12; displayYear-- }
                else displayMonth--
                selectedDay = null
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior")
            }
            Text(
                text = "${monthNames[displayMonth - 1].replaceFirstChar { it.uppercase() }} $displayYear",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = {
                if (displayMonth == 12) { displayMonth = 1; displayYear++ }
                else displayMonth++
                selectedDay = null
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mes siguiente")
            }
        }

        // ── Encabezados días de la semana ──────────────────────────────────────
        val weekDayLabels = listOf("L", "M", "X", "J", "V", "S", "D")
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Cuadrícula de días ────────────────────────────────────────────────
        val todayDay = if (today.get(Calendar.YEAR) == displayYear &&
            today.get(Calendar.MONTH) + 1 == displayMonth
        ) today.get(Calendar.DAY_OF_MONTH) else -1

        calGrid.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .then(
                                if (day != null) Modifier.clickable {
                                    selectedDay = if (selectedDay == day) null else day
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            val isSelected = selectedDay == day
                            val isToday = day == todayDay
                            val hasEvents = eventsByDay.containsKey(day)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .then(
                                        if (isSelected) Modifier.background(
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape,
                                        )
                                        else if (isToday) Modifier.background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            CircleShape,
                                        )
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    if (hasEvents) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary,
                                                    CircleShape,
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Lista de eventos del día seleccionado ─────────────────────────────
        val dayEvents = selectedDayEvents
        if (dayEvents.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Eventos del día $selectedDay",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            dayEvents.forEach { event ->
                val pending = pendingTotalsByEvent[event.id] ?: 0.0
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onOpenEvent(event) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (pending > 0.0) {
                            Text(
                                text = "Pendiente: ${formatEuros(pending)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(
                                text = "Sin deuda pendiente",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        } else if (selectedDay != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Sin eventos el día $selectedDay",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (eventsByDay.isEmpty()) "No hay eventos este mes"
                       else "Toca un día para ver los eventos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
