package com.cuentamorosos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.data.ReminderMessage
import com.cuentamorosos.model.UserPreferences

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

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Text(
                text = "Configura los recordatorios del proyecto.",
                style = MaterialTheme.typography.bodyMedium
            )
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
                                accentColorId = preferences.accentColorId, // keep for backward compat
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
                onClick = { onPostReminders(reminders) },
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
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(
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
