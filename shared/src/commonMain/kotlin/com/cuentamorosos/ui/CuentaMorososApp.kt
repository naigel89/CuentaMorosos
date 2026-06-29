@file:Suppress("UNUSED_VARIABLE")
@file:OptIn(ExperimentalFoundationApi::class)

package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.cuentamorosos.notifications.DeepLinkTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider.Factory
import com.cuentamorosos.SystemBackHandler
import com.cuentamorosos.calendarFieldsForYearMonth
import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.currentYearMonth
import com.cuentamorosos.data.ReminderMessage
import com.cuentamorosos.data.ReminderService
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.formatDateMillis
import com.cuentamorosos.data.repository.ProfileRepository
import com.cuentamorosos.isValidEmail
import com.cuentamorosos.nextMonth
import com.cuentamorosos.previousMonth
import com.cuentamorosos.shortWeekDayNames
import com.cuentamorosos.model.CalculationResult
import com.cuentamorosos.model.toJson
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.EventState
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
import com.cuentamorosos.model.resolveEventCreditor
import com.cuentamorosos.data.LogSanitizer

/**
 * Consolidated dashboard aggregates computed in a single pass over allDebts/allExpenses.
 *
 * @property activeTotalsByProfile Total amount each profile owes (debtor-side).
 * @property creditorAmounts Total amount owed TO each profile (creditor-side), resolved from
 *   debts where the current user is the debtor.
 * @property pendingTotalsByEvent Total pending amount per event.
 * @property netBalancesPerProfile Net balance per profile (activeTotalsByProfile - creditorAmounts).
 */
data class DashboardAggregates(
    val activeTotalsByProfile: Map<String, Double>,
    val creditorAmounts: Map<String, Double>,
    val pendingTotalsByEvent: Map<String, Double>,
    val totalSpent: Double,
    val participantCountByEvent: Map<String, Int>,
    val yourShareByEvent: Map<String, Double>,
    val youAreOwedByEvent: Map<String, Double>,
    val pendingEventsByProfile: Map<String, List<EventDebt>>,
)

private enum class MainSection(
    val title: String,
    val icon: ImageVector,
    val contentDescription: String,
) {
    DASHBOARD("Panel", Icons.Default.Dashboard, "Panel"),
    EVENTS("Eventos", Icons.Default.CalendarMonth, "Eventos"),
    PROFILES("Perfiles", Icons.Default.People, "Perfiles"),
    INVITATIONS("Invit.", Icons.Default.MailOutline, "Invitaciones"),
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
    networkMonitor: NetworkMonitor,
    onSignOut: (() -> Unit)? = null,
    onPickPhoto: ((OnPhotoReady) -> Unit)? = null,
    deepLinkEvent: SharedFlow<DeepLinkTarget>? = null,
    onTestNotification: ((com.cuentamorosos.notifications.NotificationEvent) -> Unit)? = null,
    profileRepository: ProfileRepository? = null,
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
    val allDebts by eventDetailViewModel.allDebts.collectAsState()
    val expenses by eventDetailViewModel.expenses.collectAsState(initial = emptyList())
    val allExpenses by eventDetailViewModel.allExpenses.collectAsState()
    val currentRole by eventDetailViewModel.currentRole.collectAsState(initial = EventRole.READER)
    val transitionWarning by eventDetailViewModel.transitionWarning.collectAsState()
    val validationErrors by eventDetailViewModel.validationErrors.collectAsState()

    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Deep link handler ──
    LaunchedEffect(deepLinkEvent) {
        deepLinkEvent?.collect { target ->
            coroutineScope.launch {
                pagerState.animateScrollToPage(target.pagerPage)
            }
            target.eventId?.let { eventId ->
                eventDetailViewModel.setEventId(eventId)
            }
        }
    }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var showCalendar by remember { mutableStateOf(false) }
    var showAccountScreen by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val accountViewModel: AccountViewModel = viewModel(factory = viewModelFactory)

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            feedbackMessage = null
        }
    }

    val aggregates by remember(allDebts, allExpenses, events, currentUserUid) {
        derivedStateOf {
            val uid = currentUserUid ?: ""

            // Single pass over allDebts
            val activeTotalsByProfile = mutableMapOf<String, Double>()
            val creditorAmounts = mutableMapOf<String, Double>()
            val pendingTotalsByEvent = mutableMapOf<String, Double>()
            val participantCountByEvent = mutableMapOf<String, Int>()
            val yourShareByEvent = mutableMapOf<String, Double>()
            val youAreOwedByEvent = mutableMapOf<String, Double>()
            val pendingEventsByProfile = mutableMapOf<String, MutableList<EventDebt>>()
            val owedToProfileEvents = mutableMapOf<String, MutableList<EventDebt>>()

            // Build event map for creditor resolution
            val eventMap = events.associateBy { it.id }

            allDebts.forEach { debt ->
                // participantCountByEvent (includes paid)
                participantCountByEvent[debt.eventId] =
                    (participantCountByEvent[debt.eventId] ?: 0) + 1

                if (!debt.paid && debt.amountEuros > 0.0) {
                    val event = eventMap[debt.eventId]

                    if (debt.profileId == uid) {
                        // Current user owes → resolve creditor
                        yourShareByEvent[debt.eventId] =
                            (yourShareByEvent[debt.eventId] ?: 0.0) + debt.amountEuros

                        val creditorId = debt.creditorId ?: resolveEventCreditor(debt, allExpenses, eventMap, uid)
                        creditorAmounts[creditorId] =
                            (creditorAmounts[creditorId] ?: 0.0) + debt.amountEuros

                        // Track events where user owes TO this profile (negative in net)
                        if (event != null) {
                            owedToProfileEvents.getOrPut(creditorId) { mutableListOf() }
                                .add(EventDebt(event.id, event.name, -debt.amountEuros))
                        }
                    } else if (debt.creditorId == uid || debt.creditorId == null) {
                        // Other profile owes current user (explicit creditorId)
                        // Legacy debts (no creditorId): preserved for backward compat
                        activeTotalsByProfile[debt.profileId] =
                            (activeTotalsByProfile[debt.profileId] ?: 0.0) + debt.amountEuros
                        youAreOwedByEvent[debt.eventId] =
                            (youAreOwedByEvent[debt.eventId] ?: 0.0) + debt.amountEuros

                        // Track events where profile owes (positive in net)
                        if (event != null) {
                            pendingEventsByProfile.getOrPut(debt.profileId) { mutableListOf() }
                                .add(EventDebt(event.id, event.name, debt.amountEuros))
                        }
                    }

                    // pendingTotalsByEvent (total pending regardless of direction)
                    pendingTotalsByEvent[debt.eventId] =
                        (pendingTotalsByEvent[debt.eventId] ?: 0.0) + debt.amountEuros
                }
            }

            // Merge both directions into pendingEventsByProfile for a complete picture
            owedToProfileEvents.forEach { (profileId, events) ->
                pendingEventsByProfile.getOrPut(profileId) { mutableListOf() }
                    .addAll(events)
            }

            DashboardAggregates(
                activeTotalsByProfile = activeTotalsByProfile,
                creditorAmounts = creditorAmounts,
                pendingTotalsByEvent = pendingTotalsByEvent,
                totalSpent = allExpenses.sumOf { it.amountEuros },
                participantCountByEvent = participantCountByEvent,
                yourShareByEvent = yourShareByEvent,
                youAreOwedByEvent = youAreOwedByEvent,
                pendingEventsByProfile = pendingEventsByProfile,
            )
        }
    }

    // Destructure for call sites
    val activeTotalsByProfile = aggregates.activeTotalsByProfile
    val creditorAmounts = aggregates.creditorAmounts
    val pendingTotalsByEvent = aggregates.pendingTotalsByEvent
    val totalSpent = aggregates.totalSpent
    val participantCountByEvent = aggregates.participantCountByEvent
    val yourShareByEvent = aggregates.yourShareByEvent
    val youAreOwedByEvent = aggregates.youAreOwedByEvent
    val pendingEventsByProfile = aggregates.pendingEventsByProfile

    // Net balance per profile: positive = they owe you, negative = you owe them
    val netBalances by remember(activeTotalsByProfile, creditorAmounts) {
        derivedStateOf {
            val allProfileIds = activeTotalsByProfile.keys + creditorAmounts.keys
            allProfileIds.associateWith { profileId ->
                (activeTotalsByProfile[profileId] ?: 0.0) - (creditorAmounts[profileId] ?: 0.0)
            }
        }
    }

    // Only show profiles the current user is allowed to see:
    // VIS-001: own real profile always visible (profile.id == uid)
    // VIS-002: own ghost profiles visible (isGhost && ownerId == uid)
    // VIS-003: co-participants in shared events visible (profile in event's participant set)
    // VIS-004: everything else hidden
    val visibleProfiles by remember(profiles, events, currentUserUid) {
        derivedStateOf {
            val uid = currentUserUid ?: ""
            val eventProfileIds = events.flatMap { event ->
                listOfNotNull(event.ownerId) + event.effectiveMemberIds
            }.toSet()

            profiles.filter { profile ->
                profile.id == uid
                        || (profile.isGhost && profile.ownerId == uid)
                        || profile.id in eventProfileIds
            }
        }
    }

    val selectedEvent by eventDetailViewModel.currentEvent.collectAsState()

    val reminderMessages by remember(events, allDebts, allExpenses, profiles, currentUserUid, preferences) {
        derivedStateOf {
            val uid = currentUserUid ?: ""
            ReminderService.buildReminderMessages(
                events = events,
                debts = allDebts,
                expenses = allExpenses,
                profiles = profiles,
                currentUserUid = uid,
                reminderDays = preferences.reminderDays,
                remindersEnabled = preferences.remindersEnabled,
            )
        }
    }

    val currentProfile by remember(profiles, currentUserUid) {
        derivedStateOf {
            val found = profiles.firstOrNull { it.id == currentUserUid }
            LogSanitizer.log("CuentaMorososApp", "profiles=${profiles.size}, looking for uid=$currentUserUid, found=${found?.name}")
            found
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
                                    .slideUp()
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
                            profiles = visibleProfiles.toList(),
                            eventDebts = debts.filter { it.eventId == currentEvent.id },
                            eventExpenses = expenses.filter { it.eventId == currentEvent.id },
                            currentUserUid = currentUserUid,
                            scrollState = scrollState,
                            onBack = { eventDetailViewModel.setEventId(null) },
                            onAddProfileToEvent = { profilesList ->
                                // Save debts for new profiles
                                profilesList.forEach { profile ->
                                    if (debts.none { it.eventId == currentEvent.id && it.profileId == profile.id }) {
                                        eventDetailViewModel.saveDebt(
                                            EventDebtItem(
                                                eventId = currentEvent.id,
                                                profileId = profile.id
                                            )
                                        )
                                    }
                                }
                                // Accumulate ALL new participants in memory, save ONCE
                                val currentParticipants = currentEvent.participants
                                val newParticipants = profilesList
                                    .filter { profile -> currentParticipants.none { it.profileId == profile.id } }
                                    .map { profile ->
                                        EventParticipant(
                                            profileId = profile.id,
                                            role = EventRole.READER,
                                            joinedAtMillis = currentTimeMillis()
                                        )
                                    }
                                if (newParticipants.isNotEmpty()) {
                                    eventsViewModel.saveEvent(
                                        currentEvent.copy(participants = currentParticipants + newParticipants)
                                    )
                                }
                                feedbackMessage = "Perfiles añadidos al evento."
                            },
                            onSaveDebt = { debt ->
                                eventDetailViewModel.saveDebt(debt)
                                feedbackMessage = "Importe y notas actualizados."
                            },
                            onTogglePaid = { debt ->
                                eventDetailViewModel.saveDebt(debt)
                                feedbackMessage = if (debt.paid) {
                                    "Perfil movido a Han pagado."
                                } else {
                                    "El perfil vuelve a pendientes."
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
                            onApplyCalculation = { modeId, result, paidTransferIndices ->
                                val transfers = result.snapshot?.transfers ?: emptyList()

                                // Fix 6: Single sequential coroutine — debts THEN event state
                                eventDetailViewModel.applyCalculationSequential(
                                    eventId = currentEvent.id,
                                    modeId = modeId,
                                    transfers = transfers,
                                    paidTransferIndices = paidTransferIndices,
                                    event = currentEvent,
                                    result = result,
                                )
                                feedbackMessage = "Cálculo aplicado al evento."
                                // Celebration: set showCelebration = true SYNCHRONOUSLY (not inside
                                // a coroutine). This guarantees the state change triggers recomposition
                                // in the current frame, regardless of any coroutine lifecycle issues.
                                showCelebration = true
                                // Scroll: use snapshotFlow to wait for the new composition's layout
                                // to update scrollState.maxValue before scrolling. This avoids the
                                // timing bug where maxValue still reflects the OLD (OPEN) content.
                                coroutineScope.launch {
                                    delay(200) // Let AnimatedContent transition complete
                                    // Wait for maxValue to reflect the new (CALCULATED) layout
                                    snapshotFlow { scrollState.maxValue }
                                        .drop(1) // Skip current value, wait for a fresh update
                                        .first { it > 0 }
                                    delay(50) // Small buffer
                                    scrollState.animateScrollTo(scrollState.maxValue, tween(400))
                                }
                            },
                            onInviteMember = { email ->
                                if (currentUserUid != null) {
                                    invitationsViewModel.sendInvitation(
                                        EventInvitation(
                                            eventId = currentEvent.id,
                                            eventName = currentEvent.name,
                                            invitedByUid = currentUserUid,
                                            invitedByEmail = currentProfile?.linkedEmail ?: "",
                                            invitedByName = currentProfile?.name ?: currentProfile?.linkedEmail ?: "",
                                            invitedByPhotoUrl = currentProfile?.photoUrl,
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
                            onCloseEvent = {
                                val eventDebts = debts.filter { it.eventId == currentEvent.id }
                                val ctx = TransitionContext(
                                    pendingPayments = eventDebts.count { !it.paid },
                                    isOwner = currentRole == EventRole.OWNER,
                                )
                                eventDetailViewModel.closeEvent(ctx)
                            },
                            profileRepository = profileRepository,
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
                                title = { Text("Acción no disponible") },
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
                                onOpenCalendar = {
                                    showCalendar = true
                                },
                            )

                            MainSection.EVENTS -> EventsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                events = events,
                                profiles = visibleProfiles.toList(),
                                participantCountByEvent = participantCountByEvent,
                                pendingTotalsByEvent = pendingTotalsByEvent,
                                totalSpent = totalSpent,
                                totalExpensesByEvent = events.associate { event ->
                                    event.id to allExpenses.filter { it.eventId == event.id }.sumOf { it.amountEuros }
                                },
                                yourShareByEvent = yourShareByEvent,
                                youAreOwedByEvent = youAreOwedByEvent,
                                expenseCountByEvent = events.associate { event ->
                                    event.id to allExpenses.filter { it.eventId == event.id }.size
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
                                profiles = visibleProfiles.map { profile ->
                                    profile.copy(totalPendingEuros = netBalances[profile.id] ?: 0.0)
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

                            MainSection.INVITATIONS -> InvitationsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                invitations = pendingInvitations,
                                onAccept = { invitation ->
                                    invitationsViewModel.acceptInvitation(
                                        invitation, inviteeName = currentProfile?.name ?: ""
                                    )
                                    feedbackMessage = "Invitación aceptada. ¡Bienvenido a ${invitation.eventName}!"
                                },
                                onReject = { invitation ->
                                    invitationsViewModel.rejectInvitation(invitation.id)
                                    feedbackMessage = "Invitación rechazada."
                                },
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
                                onSignOut = onSignOut,
                                currentProfile = currentProfile,
                                onOpenAccountSettings = { showAccountScreen = true },
                            )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !isOnline,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut(),
                ) {
                    OfflineBanner()
                }
            }

            // ── System back button handling ──────────────────────────────
            SystemBackHandler(enabled = selectedEvent != null) {
                eventDetailViewModel.setEventId(null)
            }
            SystemBackHandler(enabled = showCalendar) {
                showCalendar = false
            }
            SystemBackHandler(enabled = showAccountScreen) {
                showAccountScreen = false
            }

            if (showCalendar) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    CalendarScreen(
                        modifier = Modifier.fillMaxSize(),
                        events = events,
                        pendingTotalsByEvent = pendingTotalsByEvent,
                        onOpenEvent = { event ->
                            showCalendar = false
                            eventDetailViewModel.setEventId(event.id)
                        },
                        onClose = { showCalendar = false },
                    )
                }
            }

            if (showAccountScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    AccountScreen(
                        viewModel = accountViewModel,
                        currentProfile = currentProfile,
                        onBack = { showAccountScreen = false },
                        onPickPhoto = onPickPhoto,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // ── Money Explosion celebration overlay ──────────────────────
            MoneyExplosionAnimation(
                isVisible = showCelebration,
                onDismiss = { showCelebration = false },
            )
        }
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Sin conexión",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Modo offline — los cambios se sincronizarán al volver",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
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
    val neoColors = LocalNeoFintechColors.current

    val iconTint by animateColorAsState(
        targetValue = if (selected) neoColors.primaryContainer else neoColors.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "navIconTint",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) neoColors.onSurface else neoColors.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "navTextColor",
    )

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
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
        )
        // Selected indicator
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(width = if (selected) 16.dp else 0.dp, height = 3.dp)
                .background(
                    color = if (selected) neoColors.primaryContainer else Color.Transparent,
                    shape = RoundedCornerShape(2.dp),
                ),
        )
    }
}

// ── T3-04: InviteMemberDialog ─────────────────────────────────────────────────
// ... (rest of the file stays the same)
