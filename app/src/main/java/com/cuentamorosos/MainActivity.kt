package com.cuentamorosos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cuentamorosos.data.CuentaMorososLocalStore
import com.cuentamorosos.data.FirebaseUserSyncManager
import com.cuentamorosos.data.MigrationManager
import com.cuentamorosos.data.NotificationScheduler
import com.cuentamorosos.data.ReminderWorker
import com.cuentamorosos.data.repository.RepositoryProvider
import com.cuentamorosos.ui.CuentaMorososApp

import com.cuentamorosos.ui.auth.ForgotPasswordScreen
import com.cuentamorosos.ui.auth.LoginScreen
import com.cuentamorosos.ui.auth.MigrationScreen
import com.cuentamorosos.ui.auth.RegisterScreen
import com.cuentamorosos.ui.auth.UserProfileScreen
import com.google.firebase.auth.FirebaseAuth

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val MIGRATION = "migration"
    const val MAIN = "main"
    const val USER_PROFILE = "user_profile"
}

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // El usuario aceptó o rechazó — no bloqueamos el flujo en ningún caso.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RepositoryProvider.init(this)
        enableEdgeToEdge()
        NotificationScheduler.ensureChannel(this)
        requestNotificationsPermissionIfNeeded()
        scheduleReminderWorkerIfEnabled()
        setContent {
            val store = remember(applicationContext) {
                CuentaMorososLocalStore(applicationContext)
            }
            AppNavHost(store = store)
        }
    }

    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun scheduleReminderWorkerIfEnabled() {
        val prefs = CuentaMorososLocalStore(applicationContext).loadPreferences()
        if (prefs.remindersEnabled) {
            ReminderWorker.schedule(applicationContext)
        } else {
            ReminderWorker.cancel(applicationContext)
        }
    }
}

@Composable
private fun AppNavHost(store: CuentaMorososLocalStore) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Si hay sesión activa, comprobar si necesita migración antes de ir a MAIN
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            FirebaseUserSyncManager.syncCurrentUser()
            runCatching { FirebaseUserSyncManager.ensureOwnProfile() }
        }
        startDestination = when {
            user == null -> Routes.LOGIN
            MigrationManager.hasLocalData(context) && !MigrationManager.isMigrated(user.uid) -> Routes.MIGRATION
            else -> Routes.MAIN
        }
    }

    val destination = startDestination ?: return

    NavHost(navController = navController, startDestination = destination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    scope.launch {
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            FirebaseUserSyncManager.syncCurrentUser()
                            runCatching { FirebaseUserSyncManager.ensureOwnProfile() }
                            runCatching {
                                RepositoryProvider.profileRepository.linkGhostProfile(
                                    currentUser.email ?: "",
                                    currentUser.uid
                                )
                            }
                        }

                        val uid = currentUser?.uid
                        if (uid != null && MigrationManager.hasLocalData(context)) {
                            val needsMigration = !MigrationManager.isMigrated(uid)
                            if (needsMigration) {
                                navController.navigate(Routes.MIGRATION) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                                return@launch
                            }
                        }

                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    scope.launch {
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            runCatching { FirebaseUserSyncManager.ensureOwnProfile() }
                            runCatching {
                                RepositoryProvider.profileRepository.linkGhostProfile(
                                    currentUser.email ?: "",
                                    currentUser.uid
                                )
                            }
                        }

                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.MIGRATION) {
            MigrationScreen(
                onMigrationComplete = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MIGRATION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            CuentaMorososApp(
                store = store,
                onSignOut = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.USER_PROFILE) {
            UserProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
