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
import kotlin.math.abs

// ── ProfilesScreen ────────────────────────────────────────────────────────────

@Suppress("UNUSED_PARAMETER")
@Composable
fun ProfilesScreen(
    modifier: Modifier = Modifier,
    profiles: List<ProfileItem>,
    currentUid: String?,
    _eventCount: Int,
    pendingEventsByProfile: Map<String, List<EventDebt>>,
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
                            isGhost = true,
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
                    message = "Añade personas para reutilizarlas en distintos eventos.",
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
        val netBalance = profile.totalPendingEuros
        val events = pendingEventsByProfile[profile.id].orEmpty()
        val direction = if (netBalance >= 0) DebtDirection.OWED_TO_YOU else DebtDirection.YOU_OWE
        // Solo los perfiles ghost (locales) pueden editarse o eliminarse.
        // Los perfiles reales (Firebase Auth) se gestionan desde la cuenta del usuario.
        val uid = currentUid ?: ""
        val canEdit = profile.isGhost && profile.ownerId == uid
        val canDelete = profile.isGhost && profile.ownerId == uid
        EventBreakdownDialog(
            item = UnifiedDebtItem(
                profileId = profile.id,
                profileName = profile.name,
                amount = abs(netBalance),
                direction = direction,
                events = events,
            ),
            onDismiss = { selectedProfile = null },
            onEdit = if (canEdit) {
                {
                    selectedProfile = null
                    editableProfile = profile
                }
            } else null,
            onDelete = if (canDelete) {
                {
                    selectedProfile = null
                    profileToDelete = profile
                }
            } else null,
        )
    }

    editableProfile?.let { profile ->
        ProfileEditorDialog(
            initialProfile = profile,
            existingProfiles = profiles,
            currentUid = currentUid ?: "",
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
                append("¿Seguro que quieres eliminar \"${profile.name}\"? Se eliminarán también todas sus deudas en todos los eventos. Esta acción no se puede deshacer.")
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
    currentUid: String = "",
    onDismiss: () -> Unit,
    onSave: (ProfileItem) -> Unit,
) {
    val colors = LocalNeoFintechColors.current
    var name by remember(initialProfile.id) { mutableStateOf(initialProfile.name) }
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
                // Solo mostrar opciones ghost en edición (nuevo perfil siempre es ghost)
                if (!isNewProfile) {
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
                } else {
                    Text(
                        text = "Perfil local · Se crea sin cuenta Firebase",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
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
                        isGhost = isGhost,
                        linkedEmail = normalizedLinkedEmail.takeIf { isGhost && it.isNotBlank() },
                        ownerId = if (isGhost) currentUid else initialProfile.ownerId,
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


