package com.cuentamorosos.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuentamorosos.isValidEmail
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.displayNameFor
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.validation.ProfileValidator
import com.cuentamorosos.model.validation.ValidationError
import com.cuentamorosos.model.validation.allErrors
import com.cuentamorosos.model.validation.hasErrors

// ── ProfilesScreen ────────────────────────────────────────────────────────────

@Suppress("UNUSED_PARAMETER")
@Composable
fun ProfilesScreen(
    modifier: Modifier = Modifier,
    profiles: List<ProfileItem>,
    currentUid: String?,
    _eventCount: Int,
    pendingEventsByProfile: Map<String, List<String>>,
    onSaveProfile: (ProfileItem) -> Unit,
    onDeleteProfile: (ProfileItem) -> Unit,
) {
    val colors = LocalNeoFintechColors.current
    var selectedProfile by remember { mutableStateOf<ProfileItem?>(null) }
    var editableProfile by remember { mutableStateOf<ProfileItem?>(null) }
    var profileToDelete by remember { mutableStateOf<ProfileItem?>(null) }

    // Own profile always first, rest sorted alphabetically
    val sortedProfiles = remember(profiles, currentUid) {
        val own = profiles.filter { it.id == currentUid }
        val others = profiles.filter { it.id != currentUid }.sortedBy { it.name.lowercase() }
        own + others
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Perfiles",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Text(
                    text = "Gestioná las personas que participan en tus eventos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        // ProfileBalanceSummary removed — redundant with dashboard indicators

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        editableProfile = ProfileItem(
                            name = "",
                            icon = "🙂",
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryContainer),
                    shape = NeoFintechShapes.lg,
                ) {
                    Text(
                        text = "+ Nuevo perfil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.surface,
                    )
                }
                OutlinedButton(
                    onClick = { /* TODO: implement filter */ },
                    modifier = Modifier.weight(1f),
                    shape = NeoFintechShapes.lg,
                ) {
                    Text(
                        text = "Filtrar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                }
            }
        }

        if (profiles.isEmpty()) {
            item {
                EmptyState(
                    title = "Todavía no hay perfiles",
                    message = "Añade personas con nombre e icono para reutilizarlas en distintos eventos.",
                )
            }
        } else {
            items(sortedProfiles, key = { it.id }) { profile ->
                val isOwnProfile = profile.id == currentUid
                val isSettled = profile.totalPendingEuros == 0.0
                ProfileCard(
                    profile = profile,
                    isOwnProfile = isOwnProfile,
                    currentUid = currentUid ?: "",
                    onClick = { selectedProfile = profile },
                    modifier = Modifier.slideUp().then(if (isSettled) Modifier.alpha(0.75f) else Modifier),
                )
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

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
            },
        )
    }

    editableProfile?.let { profile ->
        ProfileEditorDialog(
            initialProfile = profile,
            existingProfiles = profiles,
            onDismiss = { editableProfile = null },
            onSave = { savedProfile ->
                editableProfile = null
                onSaveProfile(savedProfile)
            },
        )
    }

    profileToDelete?.let { profile ->
        val deleteWarnings = ProfileValidator.checkDeleteWarning(
            _profile = profile,
            activeEventIds = emptySet(), // TODO: thread active event membership from parent
        )
        val warningText = deleteWarnings.joinToString("\n") { it.message }
        NeoAlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = "Eliminar perfil",
            message = buildString {
                append("¿Seguro que quieres eliminar \"${profile.icon} ${profile.name}\"? Se eliminarán también todas sus deudas en todos los eventos. Esta acción no se puede deshacer.")
                if (warningText.isNotBlank()) {
                    append("\n\n⚠ $warningText")
                }
            },
            confirmText = "Eliminar",
            onConfirm = {
                profileToDelete = null
                onDeleteProfile(profile)
            },
        )
    }
}

// ── NeoAlertDialog ────────────────────────────────────────────────────────────

@Composable
private fun NeoAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
) {
    val colors = LocalNeoFintechColors.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = colors.surface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = colors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar", color = colors.onSurfaceVariant)
            }
        },
        shape = NeoFintechShapes.xl,
    )
}

// ── ProfileEditorDialog ───────────────────────────────────────────────────────

@Composable
private fun ProfileEditorDialog(
    initialProfile: ProfileItem,
    existingProfiles: List<ProfileItem>,
    onDismiss: () -> Unit,
    onSave: (ProfileItem) -> Unit,
) {
    val colors = LocalNeoFintechColors.current
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
    var validationErrors by remember(initialProfile.id) { mutableStateOf<List<ValidationError>>(emptyList()) }

    val isNewProfile = initialProfile.name.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = if (isNewProfile) "Nuevo perfil" else "Editar perfil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationErrors = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del perfil", color = colors.onSurfaceVariant) },
                    singleLine = true,
                    shape = NeoFintechShapes.md,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isGhost = !isGhost
                            if (!isGhost) {
                                linkedEmail = ""
                            }
                            validationErrors = emptyList()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isGhost,
                        onCheckedChange = { checked ->
                            isGhost = checked
                            if (!checked) {
                                linkedEmail = ""
                            }
                            validationErrors = emptyList()
                        },
                    )
                    Text(
                        text = "Perfil local (sin cuenta Firebase)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                    )
                }
                if (isGhost) {
                    OutlinedTextField(
                        value = linkedEmail,
                        onValueChange = {
                            linkedEmail = it.trim()
                            validationErrors = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email para vincular (opcional)", color = colors.onSurfaceVariant) },
                        singleLine = true,
                        shape = NeoFintechShapes.md,
                    )
                    Text(
                        text = "Si este email se registra después, el perfil local se vinculará automáticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Selecciona un icono",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    iconOptions.chunked(6).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            row.forEach { icon ->
                                val isSelected = icon == selectedIcon
                                if (isSelected) {
                                    Button(
                                        onClick = { selectedIcon = icon; validationErrors = emptyList() },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp),
                                        shape = NeoFintechShapes.md,
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryContainer),
                                    ) {
                                        Text(icon)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { selectedIcon = icon; validationErrors = emptyList() },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp),
                                        shape = NeoFintechShapes.md,
                                    ) {
                                        Text(icon)
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    text = "Icono seleccionado: $selectedIcon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                if (validationErrors.isNotEmpty()) {
                    validationErrors.forEach { error ->
                        Text(
                            text = "• ${error.message}",
                            color = colors.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalizedLinkedEmail = linkedEmail.trim().lowercase()
                    val invalidLinkedEmail = isGhost &&
                        normalizedLinkedEmail.isNotBlank() &&
                        !isValidEmail(normalizedLinkedEmail)
                    if (invalidLinkedEmail) {
                        validationErrors = listOf(ValidationError("El email de vinculación no es válido.", "email"))
                        return@TextButton
                    }

                    val draftProfile = initialProfile.copy(
                        name = name.trim(),
                        icon = selectedIcon,
                        isGhost = isGhost,
                        linkedEmail = normalizedLinkedEmail.takeIf { isGhost && it.isNotBlank() },
                    )

                    val result = ProfileValidator.validate(draftProfile, existingProfiles)

                    if (result.hasErrors()) {
                        validationErrors = result.allErrors()
                        return@TextButton
                    }

                    validationErrors = emptyList()
                    onSave(draftProfile)
                },
            ) {
                Text("Guardar", color = colors.primaryContainer)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.onSurfaceVariant)
            }
        },
        shape = NeoFintechShapes.xl,
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
    val colors = LocalNeoFintechColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileAvatar(name = profile.name, emoji = profile.icon, photoUrl = profile.photoUrl, size = 32.dp)
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Total pendiente:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = formatEuros(profile.totalPendingEuros),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = JetBrainsMonoFontFamily(),
                        fontWeight = FontWeight.Bold,
                        color = if (profile.totalPendingEuros >= 0) colors.primaryContainer else colors.error,
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = colors.outlineVariant,
                )
                if (pendingEvents.isEmpty()) {
                    Text(
                        text = "No tiene deudas activas ahora mismo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Eventos pendientes:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    pendingEvents.forEach { summary ->
                        Text(
                            text = "• $summary",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) {
                Text("Editar", color = colors.primaryContainer)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = colors.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = colors.onSurfaceVariant)
                }
            }
        },
        shape = NeoFintechShapes.xl,
    )
}
