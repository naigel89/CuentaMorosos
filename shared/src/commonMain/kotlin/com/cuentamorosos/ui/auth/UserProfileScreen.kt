package com.cuentamorosos.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    /** Current user's email address (read-only display). */
    userEmail: String,
    /** Current display name; null if not set. */
    currentDisplayName: String?,
    onNavigateBack: () -> Unit,
    /**
     * Platform provides the profile update operation.
     * Call [onResult] with null on success, or an error message on failure.
     */
    onUpdateDisplayName: (name: String, onResult: (error: String?) -> Unit) -> Unit,
) {
    var displayName by remember(currentDisplayName) { mutableStateOf(currentDisplayName ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isDirty = displayName != (currentDisplayName ?: "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil de usuario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Avatar placeholder
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (userEmail.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = userEmail,
                onValueChange = {},
                label = { Text("Email") },
                readOnly = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it; successMessage = null; errorMessage = null },
                label = { Text("Nombre de visualización") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (successMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = successMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    successMessage = null
                    onUpdateDisplayName(displayName) { error ->
                        isLoading = false
                        if (error == null) successMessage = "Nombre actualizado correctamente."
                        else errorMessage = "Error al actualizar el nombre."
                    }
                },
                enabled = isDirty && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar cambios")
                }
            }
        }
    }
}
