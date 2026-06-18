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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.data.ReminderMessage
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.UserPreferences

// ── SettingsScreen ────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: UserPreferences,
    reminders: List<ReminderMessage>,
    onSavePreferences: (UserPreferences) -> Unit,
    onSignOut: (() -> Unit)? = null,
    currentProfile: ProfileItem? = null,
    onOpenAccountSettings: () -> Unit = {},
) {
    var selectedThemeMode by remember(preferences.themeMode) { mutableStateOf(preferences.themeMode) }
    var reminderDaysText by remember(preferences.reminderDays) { mutableStateOf(preferences.reminderDays.toString()) }
    var remindersEnabled by remember(preferences.remindersEnabled) { mutableStateOf(preferences.remindersEnabled) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val colors = LocalNeoFintechColors.current

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

    SettingsContent(
        modifier = modifier,
        colors = colors,
        themeMode = selectedThemeMode,
        onThemeChanged = { mode -> selectedThemeMode = mode },
        remindersEnabled = remindersEnabled,
        onRemindersEnabledChanged = { remindersEnabled = it },
        reminderDaysText = reminderDaysText,
        onReminderDaysTextChanged = onDaysTextChange,
        validationMessage = validationMessage,
        onSaveClick = onSaveClick,
        reminders = reminders,
        onSignOut = onSignOut,
        currentProfile = currentProfile,
        onOpenAccountSettings = onOpenAccountSettings,
    )
}

// ── SettingsContent ───────────────────────────────────────────────────────────

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    colors: NeoFintechColorSet,
    themeMode: String,
    onThemeChanged: (String) -> Unit,
    remindersEnabled: Boolean,
    onRemindersEnabledChanged: (Boolean) -> Unit,
    reminderDaysText: String,
    onReminderDaysTextChanged: (String) -> Unit,
    validationMessage: String?,
    onSaveClick: () -> Unit,
    reminders: List<ReminderMessage>,
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
                        .fadeInStaggered(index = 0)
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
                            text = "Tema",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "Elegí el modo visual de la app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                        ThemeModeSelector(
                            selectedMode = themeMode,
                            onModeSelected = onThemeChanged,
                            colors = colors,
                        )
                    }
                }
            }
        }

        // Recordatorios section (todo en una card)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = "RECORDATORIOS", colors = colors)
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .fadeInStaggered(index = 1)
                        .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
                        .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = NeoFintechShapes.lg,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Toggle de recordatorios
                        ToggleRow(
                            title = "Activar recordatorios",
                            description = "Recordatorios de cuentas pendientes.",
                            checked = remindersEnabled,
                            onCheckedChange = onRemindersEnabledChanged,
                            colors = colors,
                            showDivider = true,
                        )

                        // Configuración de días
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = reminderDaysText,
                                onValueChange = onReminderDaysTextChanged,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Días antes del vencimiento") },
                                singleLine = true,
                                shape = NeoFintechShapes.md,
                                enabled = remindersEnabled,
                            )
                            validationMessage?.let { message ->
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        // Resumen de recordatorios
                        if (reminders.isNotEmpty()) {
                            DividerLine(colors = colors)
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ReminderSummaryCard(reminders = reminders)
                            }
                        } else if (remindersEnabled) {
                            DividerLine(colors = colors)
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "No hay recordatorios activos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Botón global de guardar cambios
        item {
            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.buttonContainer,
                    contentColor = colors.onButton,
                ),
                shape = NeoFintechShapes.md,
            ) {
                Text("Guardar cambios")
            }
        }

        // Sesión
        if (onSignOut != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .fadeInStaggered(index = 2)
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
                                contentColor = colors.error,
                            ),
                            border = BorderStroke(1.dp, colors.error),
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

// ── ThemeModeSelector ─────────────────────────────────────────────────────────

@Composable
private fun ThemeModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    colors: NeoFintechColorSet,
) {
    val segments = listOf("Sistema", "Claro", "Oscuro")
    val selectedIndex = when (selectedMode) {
        "light" -> 1
        "dark" -> 2
        else -> 0 // "system"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) colors.primaryContainer else colors.surfaceContainer,
                        shape = NeoFintechShapes.md
                    )
                    .clickable {
                        val mode = when (index) {
                            0 -> "system"
                            1 -> "light"
                            else -> "dark"
                        }
                        onModeSelected(mode)
                    }
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = if (isSelected) colors.surfaceContainerLowest else colors.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
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
