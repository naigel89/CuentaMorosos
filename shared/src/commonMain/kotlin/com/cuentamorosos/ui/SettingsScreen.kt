package com.cuentamorosos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.data.ReminderMessage
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.UserPreferences

// ── SegmentedControl ──────────────────────────────────────────────────────────

@Composable
private fun SegmentedControl(
    modifier: Modifier = Modifier,
    segments: List<String>,
    selectedIndex: Int,
    onSegmentSelected: (Int) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            val colors = LocalNeoFintechColors.current
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) colors.primaryContainer else colors.surfaceContainer,
                        shape = NeoFintechShapes.md
                    )
                    .clickable { onSegmentSelected(index) }
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = if (isSelected) colors.surfaceContainerLowest else colors.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ── SettingsScreen ────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: UserPreferences,
    reminders: List<ReminderMessage>,
    onSavePreferences: (UserPreferences) -> Unit,
    onPostReminders: (List<ReminderMessage>) -> Unit,
    onSignOut: (() -> Unit)? = null,
    currentProfile: ProfileItem? = null,
    onOpenAccountSettings: () -> Unit = {},
) {
    var selectedThemeMode by remember(preferences.themeMode) { mutableStateOf(preferences.themeMode) }
    var reminderDaysText by remember(preferences.reminderDays) { mutableStateOf(preferences.reminderDays.toString()) }
    var remindersEnabled by remember(preferences.remindersEnabled) { mutableStateOf(preferences.remindersEnabled) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var emailSummariesEnabled by remember { mutableStateOf(false) }
    var selectedDensity by remember { mutableStateOf(1) } // 0=Compacto, 1=Estándar, 2=Amplio

    val densityOptions = listOf("Compacto", "Estándar", "Amplio")

    val themeModeIndex = when (selectedThemeMode) {
        "light" -> 0
        "dark" -> 1
        else -> 1 // default to dark for "system"
    }

    val colors = LocalNeoFintechColors.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        val activeNavItem = "preferences"
        val navItems = listOf(
            Triple("preferences", "\uD83C\uDF9B️", "Preferencias"),
            Triple("security", "\uD83D\uDD12", "Seguridad"),
            Triple("account", "\uD83D\uDC64", "Cuenta"),
        )

        val onSaveClick: () -> Unit = {
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
                        reminderDays = parsedDays,
                        remindersEnabled = remindersEnabled,
                    )
                )
            }
        }

        val onDaysTextChange: (String) -> Unit = {
            reminderDaysText = it
            validationMessage = null
        }

        val onThemeChange: (Int) -> Unit = { index ->
            selectedThemeMode = when (index) {
                0 -> "light"
                1 -> "dark"
                else -> "dark"
            }
        }

        if (isWide) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sidebar
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                navItems.forEach { item ->
                    val (id, icon, label) = item
                    val isActive = id == activeNavItem
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isActive) colors.surfaceContainerHigh else colors.surface,
                                shape = NeoFintechShapes.xl
                            )
                            .clickable(enabled = isActive) { }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                            color = if (isActive) colors.onSurface else colors.onSurfaceVariant,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
            // Content
            SettingsContent(
                modifier = Modifier.weight(3f),
                colors = colors,
                themeModeIndex = themeModeIndex,
                onThemeSelected = onThemeChange,
                selectedDensity = selectedDensity,
                densityOptions = densityOptions,
                onDensitySelected = { selectedDensity = it },
                pushNotificationsEnabled = pushNotificationsEnabled,
                onPushNotificationsChanged = { pushNotificationsEnabled = it },
                emailSummariesEnabled = emailSummariesEnabled,
                onEmailSummariesChanged = { emailSummariesEnabled = it },
                remindersEnabled = remindersEnabled,
                onRemindersEnabledChanged = { remindersEnabled = it },
                reminderDaysText = reminderDaysText,
                onReminderDaysTextChanged = onDaysTextChange,
                validationMessage = validationMessage,
                onSaveClick = onSaveClick,
                reminders = reminders,
                onPostReminders = onPostReminders,
                onSignOut = onSignOut,
                currentProfile = currentProfile,
                onOpenAccountSettings = onOpenAccountSettings,
            )
        }
    } else {
        SettingsContent(
            modifier = modifier,
            colors = colors,
            themeModeIndex = themeModeIndex,
            onThemeSelected = onThemeChange,
            selectedDensity = selectedDensity,
            densityOptions = densityOptions,
            onDensitySelected = { selectedDensity = it },
            pushNotificationsEnabled = pushNotificationsEnabled,
            onPushNotificationsChanged = { pushNotificationsEnabled = it },
            emailSummariesEnabled = emailSummariesEnabled,
            onEmailSummariesChanged = { emailSummariesEnabled = it },
            remindersEnabled = remindersEnabled,
            onRemindersEnabledChanged = { remindersEnabled = it },
            reminderDaysText = reminderDaysText,
            onReminderDaysTextChanged = onDaysTextChange,
            validationMessage = validationMessage,
            onSaveClick = onSaveClick,
            reminders = reminders,
            onPostReminders = onPostReminders,
            onSignOut = onSignOut,
            currentProfile = currentProfile,
            onOpenAccountSettings = onOpenAccountSettings,
        )
    }
    }
}

// ── SettingsContent ───────────────────────────────────────────────────────────

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    colors: NeoFintechColorSet,
    themeModeIndex: Int,
    onThemeSelected: (Int) -> Unit,
    selectedDensity: Int,
    densityOptions: List<String>,
    onDensitySelected: (Int) -> Unit,
    pushNotificationsEnabled: Boolean,
    onPushNotificationsChanged: (Boolean) -> Unit,
    emailSummariesEnabled: Boolean,
    onEmailSummariesChanged: (Boolean) -> Unit,
    remindersEnabled: Boolean,
    onRemindersEnabledChanged: (Boolean) -> Unit,
    reminderDaysText: String,
    onReminderDaysTextChanged: (String) -> Unit,
    validationMessage: String?,
    onSaveClick: () -> Unit,
    reminders: List<ReminderMessage>,
    onPostReminders: (List<ReminderMessage>) -> Unit,
    onSignOut: (() -> Unit)?,
    currentProfile: ProfileItem? = null,
    onOpenAccountSettings: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Ajustes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Text(
                    text = "Personaliza la apariencia y los recordatorios de la app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        // Mi perfil section (visible when a profile is available)
        if (currentProfile != null) {
            item {
                ProfileSettingsSection(
                    profile = currentProfile!!,
                    onClick = onOpenAccountSettings,
                    colors = colors,
                )
            }
        }

        // Apariencia section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = "APARIENCIA", colors = colors)
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
                        .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = NeoFintechShapes.lg,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        ToggleRow(
                            title = "Tema",
                            description = "Elegí tu modo visual preferido.",
                            checked = themeModeIndex == 1,
                            onCheckedChange = { onThemeSelected(if (it) 1 else 0) },
                            colors = colors,
                            showDivider = true,
                        )
                        AccentColorRow(colors = colors, showDivider = true)
                        DensityRow(
                            selectedDensity = selectedDensity,
                            densityOptions = densityOptions,
                            onDensitySelected = onDensitySelected,
                            colors = colors,
                            showDivider = false,
                        )
                    }
                }
            }
        }

        // Notificaciones section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = "NOTIFICACIONES", colors = colors)
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
                        .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = NeoFintechShapes.lg,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        ToggleRow(
                            title = "Notificaciones push",
                            description = "Recibí alertas en tu dispositivo.",
                            checked = pushNotificationsEnabled,
                            onCheckedChange = onPushNotificationsChanged,
                            colors = colors,
                            showDivider = true,
                        )
                        ToggleRow(
                            title = "Resúmenes por email",
                            description = "Informe semanal de tu actividad.",
                            checked = emailSummariesEnabled,
                            onCheckedChange = onEmailSummariesChanged,
                            colors = colors,
                            showDivider = true,
                        )
                        ToggleRow(
                            title = "Activar recordatorios",
                            description = "Recordatorios de cuentas pendientes.",
                            checked = remindersEnabled,
                            onCheckedChange = onRemindersEnabledChanged,
                            colors = colors,
                            showDivider = false,
                        )
                    }
                }
            }
        }

        // Recordatorios config
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
                    .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = NeoFintechShapes.lg,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Configuración de recordatorios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    OutlinedTextField(
                        value = reminderDaysText,
                        onValueChange = onReminderDaysTextChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Días") },
                        singleLine = true,
                        shape = NeoFintechShapes.md,
                    )
                    validationMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(
                        onClick = onSaveClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = NeoFintechShapes.md,
                    ) {
                        Text("Guardar preferencias")
                    }
                }
            }
        }

        // Enviar recordatorios
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
                    .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = NeoFintechShapes.lg,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (reminders.isNotEmpty()) {
                        ReminderSummaryCard(reminders = reminders)
                    }
                    OutlinedButton(
                        onClick = { onPostReminders(reminders) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = remindersEnabled && reminders.isNotEmpty(),
                        shape = NeoFintechShapes.md,
                    ) {
                        Text(
                            if (reminders.isEmpty()) "Sin recordatorios activos"
                            else "Enviar ahora (${reminders.size})"
                        )
                    }
                }
            }
        }

        // Sesión
        if (onSignOut != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
                        .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = NeoFintechShapes.lg,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error
                            ),
                            shape = NeoFintechShapes.md,
                        ) {
                            Text("Cerrar sesión")
                        }
                    }
                }
            }
        }
    }
}

// ── SectionHeader ─────────────────────────────────────────────────────────────

@Composable
internal fun SectionHeader(text: String, colors: NeoFintechColorSet) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.primaryContainer,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

// ── DividerLine ───────────────────────────────────────────────────────────────

@Composable
private fun DividerLine(colors: NeoFintechColorSet) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        drawLine(
            color = colors.outlineVariant,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1f,
        )
    }
}

// ── ToggleRow ─────────────────────────────────────────────────────────────────

@Composable
private fun ToggleRow(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: NeoFintechColorSet,
    showDivider: Boolean = false,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
        if (showDivider) {
            DividerLine(colors = colors)
        }
    }
}

// ── AccentColorRow ────────────────────────────────────────────────────────────

@Composable
private fun AccentColorRow(
    colors: NeoFintechColorSet,
    showDivider: Boolean = false,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Color de acento",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
                Text(
                    text = "Personalizá el color principal de la interfaz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Neon green (selected) — with ring
                Canvas(modifier = Modifier.size(28.dp)) {
                    drawCircle(color = Color(0xFF39FF14))
                }
                Canvas(
                    modifier = Modifier
                        .size(28.dp)
                        .border(2.dp, colors.primaryContainer, NeoFintechShapes.full)
                ) {
                    drawCircle(color = Color(0xFF39FF14))
                }
                // Green
                Canvas(modifier = Modifier.size(28.dp)) {
                    drawCircle(color = Color(0xFF00C566))
                }
                // Gray
                Canvas(modifier = Modifier.size(28.dp)) {
                    drawCircle(color = colors.secondary)
                }
            }
        }
        if (showDivider) {
            DividerLine(colors = colors)
        }
    }
}

// ── DensityRow ────────────────────────────────────────────────────────────────

@Composable
private fun DensityRow(
    selectedDensity: Int,
    densityOptions: List<String>,
    onDensitySelected: (Int) -> Unit,
    colors: NeoFintechColorSet,
    showDivider: Boolean = false,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Densidad de lista",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
                Text(
                    text = "Ajustá el espaciado en las vistas de datos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            SegmentedControl(
                modifier = Modifier.weight(2f),
                segments = densityOptions,
                selectedIndex = selectedDensity,
                onSegmentSelected = onDensitySelected,
            )
        }
        if (showDivider) {
            DividerLine(colors = colors)
        }
    }
}

// ── ProfileSettingsSection ─────────────────────────────────────────────────────

/**
 * Profile card shown at the top of Settings when a current profile is available.
 * Tapping navigates to the account settings screen.
 */
@Composable
private fun ProfileSettingsSection(
    profile: ProfileItem,
    onClick: () -> Unit,
    colors: NeoFintechColorSet,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
            .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = NeoFintechShapes.lg,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileAvatar(
                name = profile.name,
                emoji = profile.icon,
                photoUrl = profile.photoUrl,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mi perfil",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
                Text(
                    text = "Toque para editar tu nombre, foto y más",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
