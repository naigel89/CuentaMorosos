package com.cuentamorosos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.data.ReminderMessage
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
            val colors = NeoFintechColors.dark()
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

// ── SettingsSection ───────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = NeoFintechShapes.lg,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

// ── SettingsToggle ────────────────────────────────────────────────────────────

@Composable
private fun SettingsToggle(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

// ── SettingsInfoRow ───────────────────────────────────────────────────────────

@Composable
private fun SettingsInfoRow(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String,
    isMonospace: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = if (isMonospace) {
                MaterialTheme.typography.labelSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
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
) {
    var selectedThemeMode by remember(preferences.themeMode) { mutableStateOf(preferences.themeMode) }
    var reminderDaysText by remember(preferences.reminderDays) { mutableStateOf(preferences.reminderDays.toString()) }
    var remindersEnabled by remember(preferences.remindersEnabled) { mutableStateOf(preferences.remindersEnabled) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val themeSegments = listOf("Automático", "Claro", "Oscuro")
    val themeModeIndex = when (selectedThemeMode) {
        "system" -> 0
        "light" -> 1
        "dark" -> 2
        else -> 0
    }

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
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Personaliza la apariencia y los recordatorios de la app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Apariencia section
        item {
            SettingsSection(title = "Apariencia") {
                SegmentedControl(
                    segments = themeSegments,
                    selectedIndex = themeModeIndex,
                    onSegmentSelected = { index ->
                        selectedThemeMode = when (index) {
                            0 -> "system"
                            1 -> "light"
                            2 -> "dark"
                            else -> "system"
                        }
                    },
                )
            }
        }

        // Recordatorios section
        item {
            SettingsSection(title = "Recordatorios") {
                SettingsToggle(
                    icon = "\uD83D\uDD14",
                    label = "Activar recordatorios",
                    checked = remindersEnabled,
                    onCheckedChange = { remindersEnabled = it },
                )
                SettingsInfoRow(
                    icon = "\uD83D\uDCC5",
                    label = "Días para recordar",
                    value = reminderDaysText,
                    isMonospace = true,
                )
                OutlinedTextField(
                    value = reminderDaysText,
                    onValueChange = {
                        reminderDaysText = it
                        validationMessage = null
                    },
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
                                    accentColorId = preferences.accentColorId,
                                    reminderDays = parsedDays,
                                    remindersEnabled = remindersEnabled,
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = NeoFintechShapes.md,
                ) {
                    Text("Guardar preferencias")
                }
            }
        }

        // Notificaciones section
        item {
            SettingsSection(title = "Notificaciones") {
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

        // Sesión section
        if (onSignOut != null) {
            item {
                SettingsSection(title = "Sesión") {
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
