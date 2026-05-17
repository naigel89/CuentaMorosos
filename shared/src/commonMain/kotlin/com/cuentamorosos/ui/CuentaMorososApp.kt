package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.lifecycle.viewmodel.compose.viewModel
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

private enum class MainSection(val title: String, val emoji: String) {
    DASHBOARD("Panel", "📊"),
    EVENTS("Eventos", "📅"),
    PROFILES("Perfiles", "👤"),
    SETTINGS("Ajustes", "🎨")
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
    val pendingInvitations by invitationsViewModel.pendingInvitations.collectAsState()
    val dashboardState by dashboardViewModel.state.collectAsState()

    val eventId by eventDetailViewModel.eventId.collectAsState()
    val debts by eventDetailViewModel.debts.collectAsState(initial = emptyList())
    val expenses by eventDetailViewModel.expenses.collectAsState(initial = emptyList())
    val currentRole by eventDetailViewModel.currentRole.collectAsState(initial = EventRole.READER)

    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

    var currentSection by rememberSaveable { mutableStateOf(MainSection.DASHBOARD.name) }
    val snackbarHostState = remember { SnackbarHostState() }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

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
                                    feedbackMessage = "Perfil añadido al evento."
                                }
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
                            onRemoveMember = { uid ->
                                eventsViewModel.removeMember(currentEvent.id, uid)
                                feedbackMessage = "Miembro eliminado del evento."
                            },
                            currentRole = currentRole,
                            canDo = { action -> eventDetailViewModel.canDo(action) },
                        )
                        } else {
                            AnimatedContent(
                                targetState = MainSection.valueOf(currentSection),
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(200)) togetherWith
                                    fadeOut(animationSpec = tween(200))
                                },
                                label = "screen-transition"
                            ) { section ->
                                when (section) {
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
                                reminders = reminderMessages,
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
                                }
                            )

                            MainSection.PROFILES -> ProfilesScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                profiles = profiles.map { profile ->
                                    profile.copy(totalPendingEuros = activeTotalsByProfile[profile.id] ?: 0.0)
                                },
                                currentUid = currentUserUid,
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

// ── T3-04: InviteMemberDialog ─────────────────────────────────────────────────
// ... (rest of the file stays the same)
