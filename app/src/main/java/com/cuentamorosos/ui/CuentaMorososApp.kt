package com.cuentamorosos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuentamorosos.data.CuentaMorososLocalStore
import com.cuentamorosos.data.ReminderMessage
import com.cuentamorosos.data.ReminderService
import com.cuentamorosos.model.CalculationApplication
import com.cuentamorosos.model.CalculationPreview
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ExpenseCategory
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SplitMode
import com.cuentamorosos.model.UserPreferences
import com.cuentamorosos.model.buildCalculationPreview
import com.cuentamorosos.model.currentDateText
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.formattedDate
import com.cuentamorosos.model.parseEventDate
import com.cuentamorosos.model.parseEuroAmount

private enum class MainSection(val title: String, val emoji: String) {
    EVENTS("Eventos", "📅"),
    PROFILES("Perfiles", "👤"),
    SETTINGS("Ajustes", "🎨")
}

@Composable
fun CuentaMorososApp(store: CuentaMorososLocalStore) {
    val events = remember { mutableStateListOf<EventItem>().apply { addAll(store.loadEvents()) } }
    val profiles = remember { mutableStateListOf<ProfileItem>().apply { addAll(store.loadProfiles()) } }
    val debts = remember { mutableStateListOf<EventDebtItem>().apply { addAll(store.loadDebts()) } }
    val expenses = remember { mutableStateListOf<EventExpenseItem>().apply { addAll(store.loadExpenses()) } }

    var preferences by remember { mutableStateOf(store.loadPreferences()) }
    var currentSection by rememberSaveable { mutableStateOf(MainSection.EVENTS.name) }
    var openedEventId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    fun persistData() {
        store.saveEvents(events.toList())
        store.saveProfiles(profiles.toList())
        store.saveDebts(debts.toList())
        store.saveExpenses(expenses.toList())
        store.savePreferences(preferences)
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            feedbackMessage = null
        }
    }

    val activeTotalsByProfile = debts
        .filter { !it.paid }
        .groupBy { it.profileId }
        .mapValues { (_, values) -> values.sumOf { it.amountEuros } }

    val pendingTotalsByEvent = debts
        .filter { !it.paid }
        .groupBy { it.eventId }
        .mapValues { (_, values) -> values.sumOf { it.amountEuros } }

    val participantCountByEvent = debts.groupBy { it.eventId }.mapValues { it.value.size }

    val pendingEventsByProfile = debts
        .filter { !it.paid }
        .groupBy { it.profileId }
        .mapValues { (_, values) ->
            values.mapNotNull { debt ->
                events.firstOrNull { it.id == debt.eventId }?.let { event ->
                    "${event.name} · ${formatEuros(debt.amountEuros)}"
                }
            }
        }

    val selectedEvent = events.firstOrNull { it.id == openedEventId }
    val reminderMessages = ReminderService.buildReminderMessages(
        events = events,
        debts = debts,
        expenses = expenses,
        reminderDays = preferences.reminderDays,
        remindersEnabled = preferences.remindersEnabled,
    )

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
                if (selectedEvent != null) {
                    EventDetailScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                event = selectedEvent,
                profiles = profiles.toList(),
                eventDebts = debts.filter { it.eventId == selectedEvent.id },
                eventExpenses = expenses.filter { it.eventId == selectedEvent.id },
                onBack = { openedEventId = null },
                onAddProfileToEvent = { profile ->
                    if (debts.none { it.eventId == selectedEvent.id && it.profileId == profile.id }) {
                        debts.add(
                            EventDebtItem(
                                eventId = selectedEvent.id,
                                profileId = profile.id
                            )
                        )
                        persistData()
                        feedbackMessage = "Perfil añadido al evento."
                    }
                },
                onSaveDebt = { debt ->
                    upsertDebt(debts = debts, debt = debt)
                    persistData()
                    feedbackMessage = "Importe y notas actualizados."
                },
                onTogglePaid = { debt ->
                    upsertDebt(
                        debts = debts,
                        debt = debt.copy(paid = !debt.paid)
                    )
                    persistData()
                    feedbackMessage = if (debt.paid) {
                        "El perfil vuelve a pendientes."
                    } else {
                        "Perfil movido a Han pagado."
                    }
                },
                onRemoveDebt = { debtId ->
                    removeDebt(debts = debts, debtId = debtId)
                    persistData()
                    feedbackMessage = "Perfil eliminado del evento."
                },
                onSaveExpense = { expense ->
                    upsertExpense(expenses = expenses, expense = expense)
                    persistData()
                    feedbackMessage = "Ítem del evento guardado."
                },
                onRemoveExpense = { expenseId ->
                    removeExpense(expenses = expenses, expenseId = expenseId)
                    persistData()
                    feedbackMessage = "Ítem eliminado del evento."
                },
                onApplyCalculation = { calculation ->
                    val eventEntries = debts.filter { it.eventId == selectedEvent.id }

                    eventEntries.forEachIndexed { index, debt ->
                        upsertDebt(
                            debts = debts,
                            debt = debt.copy(
                                amountEuros = calculation.amounts.getOrElse(index) { 0.0 },
                                calculationMode = calculation.mode.id
                            )
                        )
                    }

                    upsertEvent(
                        events = events,
                        event = selectedEvent.copy(
                            lastCalculationMode = calculation.mode.id,
                            lastCalculationTotal = calculation.total,
                            lastCalculationTimestamp = System.currentTimeMillis(),
                            lastCalculationSummary = calculation.summary
                        )
                    )
                    persistData()
                    feedbackMessage = "Cálculo ${calculation.mode.label} aplicado al evento."
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
                        openedEventId = event.id
                    },
                    onSaveEvent = { event ->
                        upsertEvent(events = events, event = event)
                        persistData()
                        feedbackMessage = "Evento guardado correctamente."
                    }
                )

                MainSection.PROFILES -> ProfilesScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    profiles = profiles.map { profile ->
                        profile.copy(totalPendingEuros = activeTotalsByProfile[profile.id] ?: 0.0)
                    },
                    eventCount = events.size,
                    pendingEventsByProfile = pendingEventsByProfile,
                    onSaveProfile = { profile ->
                        upsertProfile(profiles = profiles, profile = profile)
                        persistData()
                        feedbackMessage = "Perfil guardado correctamente."
                    }
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
                        feedbackMessage = "Preferencias actualizadas."
                    }
                )
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
) {
    var editableEvent by remember { mutableStateOf<EventItem?>(null) }

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
                        dateMillis = System.currentTimeMillis()
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Nuevo evento")
            }
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.weight(1f)
            ) {
                Text("Calendario próximamente")
            }
        }

        StatusCard(
            title = "Sprint 3 en progreso",
            message = "Recordatorios configurables y apariencia personalizable en arranque · $profileCount perfiles globales disponibles."
        )

        if (reminders.isNotEmpty()) {
            ReminderSummaryCard(reminders = reminders)
        }

        if (events.isEmpty()) {
            EmptyState(
                modifier = Modifier.weight(1f),
                title = "Todavía no hay eventos",
                message = "Pulsa en `Nuevo evento` para registrar el primero con nombre y fecha."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(events, key = { it.id }) { event ->
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
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: UserPreferences,
    reminders: List<ReminderMessage>,
    onSavePreferences: (UserPreferences) -> Unit,
) {
    var selectedThemeMode by remember(preferences.themeMode) { mutableStateOf(preferences.themeMode) }
    var selectedAccentColor by remember(preferences.accentColorId) { mutableStateOf(preferences.accentColorId) }
    var reminderDaysText by remember(preferences.reminderDays) { mutableStateOf(preferences.reminderDays.toString()) }
    var remindersEnabled by remember(preferences.remindersEnabled) { mutableStateOf(preferences.remindersEnabled) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

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
    }
}

@Composable
private fun ProfilesScreen(
    modifier: Modifier = Modifier,
    profiles: List<ProfileItem>,
    eventCount: Int,
    pendingEventsByProfile: Map<String, List<String>>,
    onSaveProfile: (ProfileItem) -> Unit,
) {
    var selectedProfile by remember { mutableStateOf<ProfileItem?>(null) }
    var editableProfile by remember { mutableStateOf<ProfileItem?>(null) }

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
                items(profiles, key = { it.id }) { profile ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProfile = profile }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${profile.icon} ${profile.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Pendiente activo: ${formatEuros(profile.totalPendingEuros)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Eventos abiertos: ${pendingEventsByProfile[profile.id]?.size ?: 0}",
                                style = MaterialTheme.typography.bodySmall
                            )
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
                onRemoveDebt(debt.id)
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
    val iconOptions = listOf("🙂", "🧑", "💼", "🎉", "🏠", "🚗")
    var name by remember(initialProfile.id) { mutableStateOf(initialProfile.name) }
    var selectedIcon by remember(initialProfile.id) { mutableStateOf(initialProfile.icon.ifBlank { "🙂" }) }
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
                Text(
                    text = "Selecciona un icono",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iconOptions.forEach { icon ->
                        OutlinedButton(onClick = {
                            selectedIcon = icon
                            validationMessage = null
                        }) {
                            Text(icon)
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

                    validationMessage = null
                    run {
                        onSave(
                            initialProfile.copy(
                                name = name.trim(),
                                icon = selectedIcon
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
    var validationMessage by remember(expense.id) { mutableStateOf<String?>(null) }

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
                            onClick = { selectedCategoryId = category.id },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(category.label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selectedCategoryId = category.id },
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
                                    selectedProfileIds = if (isChecked) {
                                        selectedProfileIds - profile.id
                                    } else {
                                        selectedProfileIds + profile.id
                                    }
                                    validationMessage = null
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    selectedProfileIds = if (isChecked) {
                                        selectedProfileIds - profile.id
                                    } else {
                                        selectedProfileIds + profile.id
                                    }
                                    validationMessage = null
                                }
                            )
                            Text("${profile.icon} ${profile.name}")
                        }
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
                    validationMessage = when {
                        name.isBlank() -> "Indica un nombre para el ítem."
                        parsedAmount == null -> "Introduce un importe válido."
                        parsedAmount < 0.0 -> "El importe no puede ser negativo."
                        selectedCategory != ExpenseCategory.SHARED && selectedProfileIds.isEmpty() -> "Selecciona al menos un perfil para esta categoría."
                        else -> null
                    }

                    if (validationMessage == null && parsedAmount != null) {
                        onSave(
                            expense.copy(
                                name = name.trim(),
                                amountEuros = parsedAmount,
                                category = selectedCategory.id,
                                assignedProfileIds = if (selectedCategory == ExpenseCategory.SHARED) {
                                    emptyList()
                                } else {
                                    selectedProfileIds
                                }
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
    var selectedModeId by remember { mutableStateOf(SplitMode.SIMPLE_AVG.id) }
    var percentageInputs by remember(profiles.size) { mutableStateOf(defaultPercentageInputs(profiles.size)) }
    var weightInputs by remember(profiles.size) { mutableStateOf(List(profiles.size) { "1" }) }
    var incomeInputs by remember(profiles.size) { mutableStateOf(List(profiles.size) { "1" }) }
    var surplusInputs by remember(profiles.size) { mutableStateOf(List(profiles.size) { "1" }) }
    var attendanceInputs by remember(profiles.size) { mutableStateOf(List(profiles.size) { "1" }) }
    var baseAmountText by remember { mutableStateOf("") }

    val totalValue = parseEuroAmount(totalText)
    val selectedMode = SplitMode.fromId(selectedModeId)

    fun previewFor(mode: SplitMode): CalculationPreview {
        val rawInputs = when (mode) {
            SplitMode.SIMPLE_AVG -> List(profiles.size) { 1.0 }
            SplitMode.REAL_CONSUMPTION -> List(profiles.size) { 1.0 }
            SplitMode.CUSTOM_PERCENTAGE -> percentageInputs.map { parseDecimalValue(it) ?: Double.NaN }
            SplitMode.BY_CATEGORY -> List(profiles.size) { 1.0 }
            SplitMode.BY_WEIGHT -> weightInputs.map { parseDecimalValue(it) ?: Double.NaN }
            SplitMode.BY_INCOME -> incomeInputs.map { parseDecimalValue(it) ?: Double.NaN }
            SplitMode.BASE_PLUS_SURPLUS -> surplusInputs.map { parseDecimalValue(it) ?: Double.NaN }
            SplitMode.BY_ATTENDANCE -> attendanceInputs.map { parseDecimalValue(it) ?: Double.NaN }
            SplitMode.MIXED -> surplusInputs.map { parseDecimalValue(it) ?: Double.NaN }
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
            baseAmount = parseEuroAmount(baseAmountText),
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

                SplitMode.entries.chunked(2).forEach { rowModes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowModes.forEach { mode ->
                            if (selectedMode == mode) {
                                Button(
                                    onClick = { selectedModeId = mode.id },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(mode.label)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedModeId = mode.id },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(mode.label)
                                }
                            }
                        }
                        if (rowModes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    text = selectedMode.helperText,
                    style = MaterialTheme.typography.bodySmall
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

                    SplitMode.BY_WEIGHT -> {
                        Text(
                            text = "Factor por perfil",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        profiles.forEachIndexed { index, profile ->
                            ParameterInputRow(
                                profile = profile,
                                label = "Factor",
                                value = weightInputs[index],
                                onValueChange = { value ->
                                    weightInputs = weightInputs.updateAt(index, value)
                                }
                            )
                        }
                    }

                    SplitMode.BY_INCOME -> {
                        Text(
                            text = "Capacidad de pago por perfil",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        profiles.forEachIndexed { index, profile ->
                            ParameterInputRow(
                                profile = profile,
                                label = "Ingreso",
                                value = incomeInputs[index],
                                onValueChange = { value ->
                                    incomeInputs = incomeInputs.updateAt(index, value)
                                }
                            )
                        }
                    }

                    SplitMode.BASE_PLUS_SURPLUS -> {
                        OutlinedTextField(
                            value = baseAmountText,
                            onValueChange = { baseAmountText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Cuota base común (€)") },
                            singleLine = true
                        )
                        Text(
                            text = "Factor para repartir el excedente",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        profiles.forEachIndexed { index, profile ->
                            ParameterInputRow(
                                profile = profile,
                                label = "Factor",
                                value = surplusInputs[index],
                                onValueChange = { value ->
                                    surplusInputs = surplusInputs.updateAt(index, value)
                                }
                            )
                        }
                    }

                    SplitMode.BY_ATTENDANCE -> {
                        Text(
                            text = "Asistencia por perfil",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        profiles.forEachIndexed { index, profile ->
                            ParameterInputRow(
                                profile = profile,
                                label = "Días",
                                value = attendanceInputs[index],
                                onValueChange = { value ->
                                    attendanceInputs = attendanceInputs.updateAt(index, value)
                                }
                            )
                        }
                    }

                    SplitMode.REAL_CONSUMPTION,
                    SplitMode.BY_CATEGORY,
                    SplitMode.SIMPLE_AVG,
                    SplitMode.MIXED -> {
                        if (selectedMode == SplitMode.MIXED) {
                            Text(
                                text = "Factores opcionales para repartir el remanente del modo mixto",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            profiles.forEachIndexed { index, profile ->
                                ParameterInputRow(
                                    profile = profile,
                                    label = "Factor",
                                    value = surplusInputs[index],
                                    onValueChange = { value ->
                                        surplusInputs = surplusInputs.updateAt(index, value)
                                    }
                                )
                            }
                        }
                        Text(
                            text = when (selectedMode) {
                                SplitMode.REAL_CONSUMPTION -> "Usa los perfiles asignados en cada ítem guardado."
                                SplitMode.BY_CATEGORY -> "La categoría del ítem decide si se reparte entre todos, solo seleccionados o una sola persona."
                                SplitMode.MIXED -> "Combina las categorías de los ítems y, si sobra importe, lo redistribuye según los factores indicados."
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
                        weightInputs = weightInputs,
                        incomeInputs = incomeInputs,
                        surplusInputs = surplusInputs,
                        attendanceInputs = attendanceInputs,
                        baseAmountText = baseAmountText
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
    weightInputs: List<String>,
    incomeInputs: List<String>,
    surplusInputs: List<String>,
    attendanceInputs: List<String>,
    baseAmountText: String,
) {
    fun previewFor(mode: SplitMode): CalculationPreview {
        val rawInputs = when (mode) {
            SplitMode.SIMPLE_AVG -> List(profiles.size) { 1.0 }
            SplitMode.REAL_CONSUMPTION -> List(profiles.size) { 1.0 }
            SplitMode.CUSTOM_PERCENTAGE -> percentageInputs.map { parseDecimalValue(it) ?: Double.NaN }
            SplitMode.BY_CATEGORY -> List(profiles.size) { 1.0 }
            SplitMode.BY_WEIGHT -> weightInputs.map { parseDecimalValue(it) ?: Double.NaN }
            SplitMode.BY_INCOME -> incomeInputs.map { parseDecimalValue(it) ?: Double.NaN }
            SplitMode.BASE_PLUS_SURPLUS -> surplusInputs.map { parseDecimalValue(it) ?: Double.NaN }
            SplitMode.BY_ATTENDANCE -> attendanceInputs.map { parseDecimalValue(it) ?: Double.NaN }
            SplitMode.MIXED -> surplusInputs.map { parseDecimalValue(it) ?: Double.NaN }
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
                baseAmount = parseEuroAmount(baseAmountText),
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
    val transfers = profiles.zip(amounts).filter { (_, amount) -> amount > 0.0 }
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
                text = "Transferencias mínimas estimadas: ${transfers.size}",
                style = MaterialTheme.typography.bodySmall
            )
            transfers.forEach { (profile, amount) ->
                Text(
                    text = "${profile.icon} ${profile.name} → Tú: ${formatEuros(amount)}",
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
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Recordatorios activos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            reminders.take(5).forEach { reminder ->
                Text(
                    text = "• ${reminder.title}: ${reminder.body}",
                    style = MaterialTheme.typography.bodySmall
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

private fun upsertEvent(events: SnapshotStateList<EventItem>, event: EventItem) {
    val updatedEvents = events
        .filterNot { it.id == event.id }
        .plus(event)
        .sortedByDescending { it.dateMillis }

    events.clear()
    events.addAll(updatedEvents)
}

private fun upsertProfile(profiles: SnapshotStateList<ProfileItem>, profile: ProfileItem) {
    val updatedProfiles = profiles
        .filterNot { it.id == profile.id }
        .plus(profile)
        .sortedBy { it.name.lowercase() }

    profiles.clear()
    profiles.addAll(updatedProfiles)
}

private fun upsertDebt(debts: SnapshotStateList<EventDebtItem>, debt: EventDebtItem) {
    val updatedDebts = debts
        .filterNot { it.id == debt.id }
        .plus(debt)

    debts.clear()
    debts.addAll(updatedDebts)
}

private fun removeDebt(debts: SnapshotStateList<EventDebtItem>, debtId: String) {
    val updatedDebts = debts.filterNot { it.id == debtId }
    debts.clear()
    debts.addAll(updatedDebts)
}

private fun upsertExpense(expenses: SnapshotStateList<EventExpenseItem>, expense: EventExpenseItem) {
    val updatedExpenses = expenses
        .filterNot { it.id == expense.id }
        .plus(expense)
        .sortedBy { it.name.lowercase() }

    expenses.clear()
    expenses.addAll(updatedExpenses)
}

private fun removeExpense(expenses: SnapshotStateList<EventExpenseItem>, expenseId: String) {
    val updatedExpenses = expenses.filterNot { it.id == expenseId }
    expenses.clear()
    expenses.addAll(updatedExpenses)
}
