@file:Suppress("UNUSED_VARIABLE")
@file:OptIn(ExperimentalFoundationApi::class)

package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.sizeIn
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider.Factory
import com.cuentamorosos.calendarFieldsForYearMonth
import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.currentYearMonth
import com.cuentamorosos.data.ReminderMessage
import com.cuentamorosos.data.ReminderService
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.formatDateMillis
import com.cuentamorosos.isValidEmail
import com.cuentamorosos.nextMonth
import com.cuentamorosos.previousMonth
import com.cuentamorosos.shortWeekDayNames
import com.cuentamorosos.model.CalculationResult
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.ExpenseCategory
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.UserPreferences
import com.cuentamorosos.currentDateText
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.formattedDate
import com.cuentamorosos.model.parseEventDate
import com.cuentamorosos.model.parseEuroAmount
import com.cuentamorosos.model.EventInvitation
import com.cuentamorosos.model.EventAction
import com.cuentamorosos.model.TransitionContext

private enum class MainSection(
    val title: String,
    val icon: ImageVector,
    val contentDescription: String,
) {
    DASHBOARD("Panel", Icons.Default.Dashboard, "Panel"),
    EVENTS("Eventos", Icons.Default.CalendarMonth, "Eventos"),
    PROFILES("Perfiles", Icons.Default.People, "Perfiles"),
    SETTINGS("Ajustes", Icons.Default.Settings, "Ajustes"),
}

@Composable
fun CuentaMorososApp(
    viewModelFactory: Factory,
    currentUserUid: String?,
    preferences: UserPreferences,
    onSavePreferences: (UserPreferences) -> Unit,
    onScheduleReminders: () -> Unit,
    onCancelReminders: () -> Unit,
    onPostReminders: (List<ReminderMessage>) -> Unit,
    networkMonitor: NetworkMonitor,
    onSignOut: (() -> Unit)? = null,
) {
    val eventsViewModel: EventsViewModel = viewModel(factory = viewModelFactory)
    val eventDetailViewModel: EventDetailViewModel = viewModel(factory = viewModelFactory)
    val profilesViewModel: ProfilesViewModel = viewModel(factory = viewModelFactory)
    val invitationsViewModel: InvitationsViewModel = viewModel(factory = viewModelFactory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)

    val events by eventsViewModel.events.collectAsState()
    val profiles by profilesViewModel.profiles.collectAsState()
    val _pendingInvitations by invitationsViewModel.pendingInvitations.collectAsState() // TODO: use in UI
    val dashboardState by dashboardViewModel.state.collectAsState()

    val eventId by eventDetailViewModel.eventId.collectAsState()
    val debts by eventDetailViewModel.debts.collectAsState(initial = emptyList())
    val allDebts by eventDetailViewModel.allDebts.collectAsState()
    val expenses by eventDetailViewModel.expenses.collectAsState(initial = emptyList())
    val currentRole by eventDetailViewModel.currentRole.collectAsState(initial = EventRole.READER)
    val transitionWarning by eventDetailViewModel.transitionWarning.collectAsState()
    val validationErrors by eventDetailViewModel.validationErrors.collectAsState()

    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            feedbackMessage = null
        }
    }

    val activeTotalsByProfile by remember(allDebts) {
        derivedStateOf {
            allDebts
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

    val totalSpent by remember(expenses) {
        derivedStateOf { expenses.sumOf { it.amountEuros } }
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
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (selectedEvent == null) {
                            Surface(
                                shape = NeoFintechShapes.pill,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    MainSection.entries.forEachIndexed { index, section ->
                                        PillNavItem(
                                            icon = section.icon,
                                            label = section.title,
                                            contentDescription = section.contentDescription,
                                            selected = pagerState.currentPage == index,
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val event = selectedEvent
                    AnimatedContent(
                        targetState = event,
                        transitionSpec = {
                            if (targetState != null) {
                                slideInHorizontally { it } + fadeIn(animationSpec = tween(200)) togetherWith
                                slideOutHorizontally { -it } + fadeOut(animationSpec = tween(200))
                            } else {
                                slideInHorizontally { -it } + fadeIn(animationSpec = tween(200)) togetherWith
                                slideOutHorizontally { it } + fadeOut(animationSpec = tween(200))
                            }
                        },
                        label = "event-detail-transition"
                    ) { currentEvent ->
                        if (currentEvent != null) {
                            EventDetailScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            event = currentEvent,
                            profiles = profiles.toList(),
                            eventDebts = debts.filter { it.eventId == currentEvent.id },
                            eventExpenses = expenses.filter { it.eventId == currentEvent.id },
                            currentUserUid = currentUserUid,
                            onBack = { eventDetailViewModel.setEventId(null) },
                            onAddProfileToEvent = { profile ->
                                if (debts.none { it.eventId == currentEvent.id && it.profileId == profile.id }) {
                                    eventDetailViewModel.saveDebt(
                                        EventDebtItem(
                                            eventId = currentEvent.id,
                                            profileId = profile.id
                                        )
                                    )
                                }
                                // Sync participant to event's participants list
                                if (currentEvent.participants.none { it.profileId == profile.id }) {
                                    val newParticipant = EventParticipant(
                                        profileId = profile.id,
                                        role = EventRole.CONTRIBUTOR,
                                        joinedAtMillis = currentTimeMillis()
                                    )
                                    eventsViewModel.saveEvent(
                                        currentEvent.copy(participants = currentEvent.participants + newParticipant)
                                    )
                                }
                                feedbackMessage = "Perfil añadido al evento."
                            },
                            onSaveDebt = { debt ->
                                eventDetailViewModel.saveDebt(debt)
                                feedbackMessage = "Importe y notas actualizados."
                            },
                            onTogglePaid = { debt ->
                                eventDetailViewModel.saveDebt(
                                    debt.copy(paid = !debt.paid)
                                )
                                feedbackMessage = if (debt.paid) {
                                    "El perfil vuelve a pendientes."
                                } else {
                                    "Perfil movido a Han pagado."
                                }
                            },
                            onRemoveDebt = { debtId ->
                                eventDetailViewModel.deleteDebt(currentEvent.id, debtId)
                                feedbackMessage = "Perfil eliminado del evento."
                            },
                            onSaveExpense = { expense ->
                                eventDetailViewModel.saveExpense(expense)
                                feedbackMessage = "Ítem del evento guardado."
                            },
                            onRemoveExpense = { expenseId ->
                                eventDetailViewModel.deleteExpense(currentEvent.id, expenseId)
                                feedbackMessage = "Ítem eliminado del evento."
                            },
                            onApplyCalculation = { result ->
                                val eventEntries = debts.filter { it.eventId == currentEvent.id }
                                val balances = result.snapshot?.participantBalances ?: emptyMap()

                                // Pass 1: update existing debts
                                eventEntries.forEach { debt ->
                                    val balance = balances[debt.profileId] ?: 0.0
                                    val amount = if (balance < 0) -balance else 0.0
                                    eventDetailViewModel.saveDebt(
                                        debt.copy(
                                            amountEuros = amount,
                                            calculationMode = currentEvent.lastCalculationMode ?: "real_consumption"
                                        )
                                    )
                                }

                                // Pass 2: create debts for profiles in balances without existing entries
                                val existingProfileIds = eventEntries.map { it.profileId }.toSet()
                                balances.forEach { (profileId, balance) ->
                                    if (profileId !in existingProfileIds) {
                                        val amount = if (balance < 0) -balance else 0.0
                                        eventDetailViewModel.saveDebt(
                                            EventDebtItem(
                                                eventId = currentEvent.id,
                                                profileId = profileId,
                                                amountEuros = amount,
                                                calculationMode = currentEvent.lastCalculationMode ?: "real_consumption"
                                            )
                                        )
                                    }
                                }

                                eventsViewModel.saveEvent(
                                    currentEvent.copy(
                                        lastCalculationMode = currentEvent.lastCalculationMode,
                                        lastCalculationTotal = result.snapshot?.totalExpense,
                                        lastCalculationTimestamp = currentTimeMillis(),
                                        lastCalculationSummary = result.status?.message ?: "Cálculo aplicado"
                                    )
                                )
                                feedbackMessage = "Cálculo aplicado al evento."
                            },
                            onInviteMember = { email ->
                                if (currentUserUid != null) {
                                    invitationsViewModel.sendInvitation(
                                        EventInvitation(
                                            eventId = currentEvent.id,
                                            eventName = currentEvent.name,
                                            invitedByUid = currentUserUid,
                                            invitedByEmail = "",
                                            invitedEmail = email,
                                        )
                                    )
                                    feedbackMessage = "Invitación enviada a $email."
                                }
                            },
                            _onRemoveMember = { uid ->
                                eventsViewModel.removeMember(currentEvent.id, uid)
                                feedbackMessage = "Miembro eliminado del evento."
                            },
                            currentRole = currentRole,
                            canDo = { action -> eventDetailViewModel.canDo(action) },
                            onOpenEvent = {
                                val ctx = TransitionContext(
                                    eventName = currentEvent.name,
                                    eventBaseCurrency = currentEvent.baseCurrency,
                                    memberCount = currentEvent.effectiveMemberIds.size,
                                    expenseCount = expenses.filter { it.eventId == currentEvent.id }.size,
                                    isOwner = currentRole == EventRole.OWNER,
                                )
                                eventDetailViewModel.openEvent(ctx)
                            },
                        )

                        // Transition warning dialog for event detail
                        val currentTransitionWarning = transitionWarning
                        if (currentTransitionWarning != null) {
                            AlertDialog(
                                onDismissRequest = { eventDetailViewModel.dismissTransitionWarning() },
                                title = { Text("Confirmar acción") },
                                text = {
                                    Text(
                                        "⚠ ${currentTransitionWarning.warning}\n\n¿Querés continuar?",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        eventDetailViewModel.confirmTransition(currentTransitionWarning.newState)
                                    }) {
                                        Text("Confirmar", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { eventDetailViewModel.dismissTransitionWarning() }) {
                                        Text("Cancelar")
                                    }
                                },
                            )
                        }

                        // Validation errors dialog for event detail
                        if (validationErrors.isNotEmpty()) {
                            AlertDialog(
                                onDismissRequest = { eventDetailViewModel.clearValidationErrors() },
                                title = { Text("No se puede abrir el evento") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        validationErrors.forEach { error ->
                                            Text("• $error", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { eventDetailViewModel.clearValidationErrors() }) {
                                        Text("Entendido")
                                    }
                                },
                            )
                        }
                        } else {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                            ) { page ->
                                when (MainSection.entries[page]) {
                                    MainSection.DASHBOARD -> DashboardScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                state = dashboardState,
                                onAlertTap = { alert ->
                                    eventDetailViewModel.setEventId(alert.eventId)
                                },
                                onEventTap = { eventRow ->
                                    eventDetailViewModel.setEventId(eventRow.eventId)
                                },
                            )

                            MainSection.EVENTS -> EventsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                events = events,
                                profiles = profiles.toList(),
                                participantCountByEvent = participantCountByEvent,
                                pendingTotalsByEvent = pendingTotalsByEvent,
                                totalSpent = totalSpent,
                                totalExpensesByEvent = events.associate { event ->
                                    event.id to expenses.filter { it.eventId == event.id }.sumOf { it.amountEuros }
                                },
                                yourShareByEvent = emptyMap(),
                                youAreOwedByEvent = emptyMap(),
                                expenseCountByEvent = events.associate { event ->
                                    event.id to expenses.filter { it.eventId == event.id }.size
                                },
                                _reminders = reminderMessages,
                                currentUserUid = currentUserUid,
                                onOpenEvent = { event ->
                                    eventDetailViewModel.setEventId(event.id)
                                },
                                onSaveEvent = { event ->
                                    eventsViewModel.saveEvent(event)
                                    feedbackMessage = "Evento guardado correctamente."
                                },
                                onDeleteEvent = { event ->
                                    eventsViewModel.deleteEvent(event.id)
                                    feedbackMessage = "Evento \"${event.name}\" eliminado."
                                },
                                transitionWarning = transitionWarning,
                                onConfirmTransition = {
                                    selectedEvent?.let {
                                        eventDetailViewModel.confirmTransition(
                                            com.cuentamorosos.model.EventState.OPEN
                                        )
                                    }
                                },
                                onDismissWarning = { eventDetailViewModel.dismissTransitionWarning() },
                                validationErrors = validationErrors,
                                onClearValidationErrors = { eventDetailViewModel.clearValidationErrors() },
                                currentProfileId = currentUserUid,
                            )

                            MainSection.PROFILES -> ProfilesScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                profiles = profiles.map { profile ->
                                    profile.copy(totalPendingEuros = activeTotalsByProfile[profile.id] ?: 0.0)
                                },
                                currentUid = currentUserUid,
                                _eventCount = events.size,
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

                            MainSection.SETTINGS -> SettingsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                preferences = preferences,
                                reminders = reminderMessages,
                                onSavePreferences = { updatedPreferences ->
                                    onSavePreferences(updatedPreferences)
                                    if (updatedPreferences.remindersEnabled) {
                                        onScheduleReminders()
                                    } else {
                                        onCancelReminders()
                                    }
                                    feedbackMessage = "Preferencias actualizadas."
                                },
                                onPostReminders = onPostReminders,
                                onSignOut = onSignOut
                            )
                                }
                            }
                        }
                    }
                }

                if (!isOnline) {
                    OfflineBanner()
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Modo offline: los cambios se sincronizarán al recuperar la conexión",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PillNavItem(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) colors.primaryContainer else colors.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

// ── T3-04: InviteMemberDialog ─────────────────────────────────────────────────
// ... (rest of the file stays the same)
