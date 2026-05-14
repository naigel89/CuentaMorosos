package com.cuentamorosos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.isValidEmail
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros

// ── ProfilesScreen ────────────────────────────────────────────────────────────

@Composable
fun ProfilesScreen(
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

// ── ProfileEditorDialog ───────────────────────────────────────────────────────

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
                    items(count = iconOptions.size) { index ->
                        val icon = iconOptions[index]
                        val isSelected = icon == selectedIcon
                        if (isSelected) {
                            Button(
                                onClick = { selectedIcon = icon; validationMessage = null },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.aspectRatio(1f)
                            ) {
                                Text(icon)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { selectedIcon = icon; validationMessage = null },
                                contentPadding = PaddingValues(0.dp),
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
                        !isValidEmail(normalizedLinkedEmail)
                    if (invalidLinkedEmail) {
                        validationMessage = "El email de vinculación no es válido."
                        return@TextButton
                    }

                    validationMessage = null
                    onSave(
                        initialProfile.copy(
                            name = name.trim(),
                            icon = selectedIcon,
                            isGhost = isGhost,
                            linkedEmail = normalizedLinkedEmail.takeIf { isGhost && it.isNotBlank() }
                        )
                    )
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

// ── ProfileDetailDialog ───────────────────────────────────────────────────────

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
