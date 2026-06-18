package com.cuentamorosos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.SystemBackHandler
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.displayNameFor

// ── AccountScreen (entry point) ────────────────────────────────────────────────

/**
 * Account settings screen with sub-screen navigation via AnimatedContent.
 *
 * Sub-screens:
 *  0 = Main menu (account overview + navigation rows)
 *  1 = Name & Photo editing
 *  2 = Username editing with availability check
 *  3 = Security (email, password change, delete account)
 */
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    currentProfile: ProfileItem?,
    onBack: () -> Unit,
    onPickPhoto: ((OnPhotoReady) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val subScreenIndex by viewModel.subScreenIndex.collectAsState()

    SystemBackHandler(enabled = subScreenIndex > 0) {
        viewModel.onBackToMenu()
    }

    AnimatedContent(
        targetState = subScreenIndex,
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            slideInHorizontally { it * direction } + fadeIn(animationSpec = tween(250)) togetherWith
            slideOutHorizontally { -it * direction } + fadeOut(animationSpec = tween(250))
        },
        label = "account-sub-screen",
        modifier = modifier,
    ) { index ->
        when (index) {
            0 -> AccountMainMenu(
                profile = currentProfile,
                currentUid = currentProfile?.id ?: "",
                onNavigateToNamePhoto = { viewModel.navigateTo(1) },
                onNavigateToUsername = { viewModel.navigateTo(2) },
                onNavigateToSecurity = { viewModel.navigateTo(3) },
                onBack = onBack,
            )
            1 -> NamePhotoScreen(
                viewModel = viewModel,
                onBack = { viewModel.onBackToMenu() },
                onPickPhoto = onPickPhoto,
            )
            2 -> UsernameScreen(
                viewModel = viewModel,
                onBack = { viewModel.onBackToMenu() },
            )
            3 -> SecurityScreen(
                viewModel = viewModel,
                onBack = { viewModel.onBackToMenu() },
            )
        }
    }
}

// ── AccountMainMenu ────────────────────────────────────────────────────────────

/**
 * Main menu of the account settings, showing the user's avatar, display name,
 * email, and navigation rows for each settings sub-section.
 */
@Composable
private fun AccountMainMenu(
    profile: ProfileItem?,
    currentUid: String,
    onNavigateToNamePhoto: () -> Unit,
    onNavigateToUsername: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalNeoFintechColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .slideUp(),
    ) {
        // Top bar
        TextButton(onClick = onBack) {
            Text("← Ajustes", color = colors.primaryContainer)
        }

        // Profile header
        if (profile != null) {
            val displayName = profile.displayNameFor(currentUid)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileAvatar(
                    name = profile.name,
                    emoji = profile.icon,
                    photoUrl = profile.photoUrl,
                    size = 72.dp,
                )

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )

                if (!profile.username.isNullOrBlank()) {
                    Text(
                        text = "@${profile.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }

                Text(
                    text = profile.linkedEmail ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation rows
        AccountMenuRow(
            title = "Nombre y foto",
            subtitle = "Cambiá tu nombre, foto de perfil y cómo te ven los demás.",
            onClick = onNavigateToNamePhoto,
            colors = colors,
        )
        AccountMenuRow(
            title = "Nombre de usuario",
            subtitle = "Elegí un nombre de usuario único para tu perfil.",
            onClick = onNavigateToUsername,
            colors = colors,
        )
        AccountMenuRow(
            title = "Seguridad",
            subtitle = "Cambiá tu contraseña y gestioná la seguridad de tu cuenta.",
            onClick = onNavigateToSecurity,
            colors = colors,
        )
    }
}

/**
 * Single row item in the account main menu, with title, subtitle, and arrow indicator.
 */
@Composable
private fun AccountMenuRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    colors: NeoFintechColorSet,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
            )
            Text(
                text = subtitle,
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

// ── NamePhotoScreen ────────────────────────────────────────────────────────────

/**
 * Sub-screen for editing display name and profile photo.
 * The avatar is clickable — on Android, wire via rememberLauncherForActivityResult
 * with GetContent("image/") to open the system photo picker, then call
 * viewModel.setSelectedPhotoUri(uri.toString()) and upload to Firebase Storage
 * before calling viewModel.updatePhoto(downloadUrl).
 */
@Composable
private fun NamePhotoScreen(
    viewModel: AccountViewModel,
    onBack: () -> Unit,
    onPickPhoto: ((OnPhotoReady) -> Unit)? = null,
) {
    val colors = LocalNeoFintechColors.current
    val displayNameText by viewModel.displayNameText.collectAsState()
    val profile by viewModel.currentProfile.collectAsState()
    val uiState by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .slideUp(),
    ) {
        // Top bar
        TextButton(onClick = onBack) {
            Text("← Nombre y foto", color = colors.primaryContainer)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Avatar (clickable — opens platform photo picker)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        onPickPhoto?.invoke { downloadUrl ->
                            viewModel.updatePhoto(downloadUrl)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                ProfileAvatar(
                    name = profile?.name ?: "",
                    emoji = profile?.icon ?: "",
                    photoUrl = profile?.photoUrl,
                    size = 96.dp,
                )
            }

            Text(
                text = if (profile?.photoUrl != null) "Toque la foto para cambiarla"
                       else "Toque para agregar una foto",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            // Delete photo button (only visible when there's a photo)
            if (profile?.photoUrl != null) {
                OutlinedButton(
                    onClick = { viewModel.deletePhoto() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    shape = NeoFintechShapes.md,
                ) {
                    Text("Eliminar foto")
                }
            }

            OutlinedTextField(
                value = displayNameText,
                onValueChange = { viewModel.setDisplayName(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre") },
                singleLine = true,
                shape = NeoFintechShapes.md,
            )

            // Info note about custom names
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                shape = NeoFintechShapes.md,
            ) {
                Text(
                    text = "Los nombres personalizados que otros perfiles te hayan asignado aparecerán automáticamente.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            Button(
                onClick = { viewModel.saveDisplayName() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AccountUiState.Loading,
                shape = NeoFintechShapes.md,
            ) {
                if (uiState is AccountUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.onButton,
                    )
                } else {
                    Text("Guardar")
                }
            }

            // Status messages
            when (val state = uiState) {
                is AccountUiState.Success -> {
                    Text(
                        text = state.message,
                        color = colors.primaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                is AccountUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                else -> {}
            }
        }
    }
}

// ── UsernameScreen ─────────────────────────────────────────────────────────────

/**
 * Sub-screen for editing the unique username with async availability validation.
 */
@Composable
private fun UsernameScreen(
    viewModel: AccountViewModel,
    onBack: () -> Unit,
) {
    val colors = LocalNeoFintechColors.current
    val usernameText by viewModel.usernameText.collectAsState()
    val usernameAvailability by viewModel.usernameAvailability.collectAsState()
    val uiState by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .slideUp(),
    ) {
        // Top bar
        TextButton(onClick = onBack) {
            Text("← Nombre de usuario", color = colors.primaryContainer)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = usernameText,
                onValueChange = { viewModel.setUsername(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre de usuario") },
                prefix = { Text("@") },
                singleLine = true,
                shape = NeoFintechShapes.md,
            )

            // Availability indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (usernameAvailability) {
                    is UsernameAvailability.Idle -> {}
                    is UsernameAvailability.Checking -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verificando disponibilidad...",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    is UsernameAvailability.Available -> {
                        Text(
                            text = "✓ Disponible",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primaryContainer,
                        )
                    }
                    is UsernameAvailability.Taken -> {
                        Text(
                            text = "✗ No disponible",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Text(
                text = "El nombre de usuario solo puede contener letras, números y guiones bajos.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            Button(
                onClick = { viewModel.saveUsername() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AccountUiState.Loading &&
                    usernameAvailability is UsernameAvailability.Available &&
                    usernameText.length >= 3,
                shape = NeoFintechShapes.md,
            ) {
                if (uiState is AccountUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.onButton,
                    )
                } else {
                    Text("Guardar nombre de usuario")
                }
            }

            // Status messages
            when (val state = uiState) {
                is AccountUiState.Success -> {
                    Text(
                        text = state.message,
                        color = colors.primaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                is AccountUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                else -> {}
            }
        }
    }
}

// ── SecurityScreen ─────────────────────────────────────────────────────────────

/**
 * Sub-screen for security settings: email (read-only), password change,
 * and account deletion (disabled for now, visual only).
 */
@Composable
private fun SecurityScreen(
    viewModel: AccountViewModel,
    onBack: () -> Unit,
) {
    val colors = LocalNeoFintechColors.current
    val currentPassword by viewModel.currentPassword.collectAsState()
    val newPassword by viewModel.newPassword.collectAsState()
    val passwordState by viewModel.passwordState.collectAsState()
    val profile by viewModel.currentProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .slideUp()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top bar
        TextButton(onClick = onBack) {
            Text("← Seguridad", color = colors.primaryContainer)
        }

        // ── Cambiar email section ────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(text = "CAMBIAR EMAIL", colors = colors)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
                    .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = NeoFintechShapes.lg,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = profile?.linkedEmail ?: "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email actual") },
                        enabled = false,
                        singleLine = true,
                        shape = NeoFintechShapes.md,
                    )
                    Text(
                        text = "El cambio de email estará disponible próximamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Cambiar contraseña section ───────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(text = "CAMBIAR CONTRASEÑA", colors = colors)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
                    .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = NeoFintechShapes.lg,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { viewModel.setCurrentPassword(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Contraseña actual") },
                        singleLine = true,
                        shape = NeoFintechShapes.md,
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { viewModel.setNewPassword(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nueva contraseña") },
                        singleLine = true,
                        shape = NeoFintechShapes.md,
                    )
                    Button(
                        onClick = { viewModel.changePassword() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = passwordState !is PasswordState.Loading,
                        shape = NeoFintechShapes.md,
                    ) {
                        if (passwordState is PasswordState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = colors.onButton,
                            )
                        } else {
                            Text("Cambiar contraseña")
                        }
                    }

                    when (val state = passwordState) {
                        is PasswordState.Success -> {
                            Text(
                                text = state.message,
                                color = colors.primaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        is PasswordState.Error -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        else -> {}
                    }
                }
            }
        }

        // ── Eliminar cuenta section ──────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(text = "ELIMINAR CUENTA", colors = colors)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(NeoFintechElevation.cardShadowElevation, NeoFintechElevation.cardShadowShape, clip = false)
                    .border(1.dp, colors.error.copy(alpha = 0.3f), NeoFintechShapes.lg),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = NeoFintechShapes.lg,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Eliminá tu cuenta permanentemente",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.error,
                    )
                    Text(
                        text = "Se eliminarán todos tus datos y no podrás recuperarlos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.error,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            colors.error.copy(alpha = 0.5f),
                        ),
                        shape = NeoFintechShapes.md,
                    ) {
                        Text("Eliminar cuenta")
                    }
                    Text(
                        text = "Esta opción estará disponible próximamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
