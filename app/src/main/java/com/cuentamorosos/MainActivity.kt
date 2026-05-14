package com.cuentamorosos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.cuentamorosos.data.CuentaMorososLocalStore
import com.cuentamorosos.data.FirebaseUserSyncManager
import com.cuentamorosos.data.NetworkMonitorFactory
import com.cuentamorosos.data.NotificationScheduler
import com.cuentamorosos.data.ReminderWorker
import com.cuentamorosos.db.DriverFactory
import com.cuentamorosos.model.UserPreferences
import com.cuentamorosos.ui.CuentaMorososApp
import com.cuentamorosos.ui.CuentaMorososTheme
import com.cuentamorosos.ui.auth.ForgotPasswordScreen
import com.cuentamorosos.ui.auth.LoginScreen
import com.cuentamorosos.ui.auth.RegisterScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private lateinit var repositoryProvider: RepositoryProvider
    private lateinit var viewModelFactory: AppViewModelFactory
    private lateinit var localStore: CuentaMorososLocalStore
    private lateinit var networkMonitor: com.cuentamorosos.data.NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure notification channel exists
        NotificationScheduler.ensureChannel(this)

        // Initialize SQLDelight driver and repositories
        val sqlDriver = DriverFactory(applicationContext).createDriver()
        networkMonitor = NetworkMonitorFactory(applicationContext).create()
        repositoryProvider = RepositoryProvider(sqlDriver, networkMonitor)
        viewModelFactory = AppViewModelFactory(repositoryProvider)
        localStore = CuentaMorososLocalStore(applicationContext)

        // Sync Firebase user on startup if already logged in
        FirebaseAuth.getInstance().currentUser?.let {
            runBlocking {
                FirebaseUserSyncManager.syncCurrentUser()
                FirebaseUserSyncManager.ensureOwnProfile()
            }
        }

        setContent {
            val preferences = remember { localStore.loadPreferences() }
            CuentaMorososTheme(preferences = preferences) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val auth = remember { FirebaseAuth.getInstance() }
                    var currentUser by remember { mutableStateOf(auth.currentUser) }

                    // Listen to auth state changes
                    LaunchedEffect(auth) {
                        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                            currentUser = firebaseAuth.currentUser
                        }
                        auth.addAuthStateListener(listener)
                    }

                    if (currentUser != null) {
                        MainAppContent(
                            user = currentUser!!,
                            viewModelFactory = viewModelFactory,
                            localStore = localStore,
                            networkMonitor = networkMonitor,
                            application = application
                        )
                    } else {
                        AuthFlow(
                            auth = auth,
                            onAuthSuccess = { user ->
                                currentUser = user
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Main app content shown when user is authenticated.
 */
@Composable
private fun MainAppContent(
    user: com.google.firebase.auth.FirebaseUser,
    viewModelFactory: ViewModelProvider.Factory,
    localStore: CuentaMorososLocalStore,
    networkMonitor: com.cuentamorosos.data.NetworkMonitor,
    application: android.app.Application
) {
    var preferences by remember { mutableStateOf(localStore.loadPreferences()) }

    CuentaMorososApp(
        viewModelFactory = viewModelFactory,
        currentUserUid = user.uid,
        preferences = preferences,
        onSavePreferences = { updated ->
            preferences = updated
            localStore.savePreferences(updated)
        },
        onScheduleReminders = {
            ReminderWorker.schedule(application)
        },
        onCancelReminders = {
            ReminderWorker.cancel(application)
        },
        onPostReminders = { messages ->
            NotificationScheduler.postReminders(application, messages)
        },
        networkMonitor = networkMonitor,
        onSignOut = {
            FirebaseAuth.getInstance().signOut()
        }
    )
}

/**
 * Auth flow: login → register → forgot password navigation.
 */
@Composable
private fun AuthFlow(
    auth: FirebaseAuth,
    onAuthSuccess: (com.google.firebase.auth.FirebaseUser) -> Unit
) {
    var showLogin by remember { mutableStateOf(true) }
    var showRegister by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }

    if (showLogin) {
        LoginScreen(
            onLoginSuccess = {
                auth.currentUser?.let { user ->
                    runBlocking {
                        FirebaseUserSyncManager.syncCurrentUser()
                        FirebaseUserSyncManager.ensureOwnProfile()
                    }
                    onAuthSuccess(user)
                }
            },
            onNavigateToRegister = {
                showLogin = false
                showRegister = true
            },
            onNavigateToForgotPassword = {
                showLogin = false
                showForgotPassword = true
            },
            onLogin = { email, password, onResult ->
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        result.user?.let { onResult(null) }
                            ?: onResult("No se pudo iniciar sesión")
                    }
                    .addOnFailureListener { e ->
                        onResult(e.localizedMessage ?: "Error al iniciar sesión")
                    }
            }
        )
    } else if (showRegister) {
        RegisterScreen(
            onRegisterSuccess = {
                auth.currentUser?.let { user ->
                    runBlocking {
                        FirebaseUserSyncManager.syncCurrentUser(defaultMigrated = true)
                        FirebaseUserSyncManager.ensureOwnProfile()
                    }
                    onAuthSuccess(user)
                }
            },
            onNavigateToLogin = {
                showRegister = false
                showLogin = true
            },
            onRegister = { email, password, onResult ->
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        // Set display name
                        val displayName = email.substringBefore("@")
                        result.user?.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build()
                        )?.addOnSuccessListener {
                            onResult(null)
                        }
                    }
                    .addOnFailureListener { e ->
                        onResult(e.localizedMessage ?: "Error al registrarse")
                    }
            }
        )
    } else if (showForgotPassword) {
        ForgotPasswordScreen(
            onNavigateToLogin = {
                showForgotPassword = false
                showLogin = true
            },
            onResetPassword = { email, onResult ->
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener { onResult(null) }
                    .addOnFailureListener { e ->
                        onResult(e.localizedMessage ?: "Error al enviar email")
                    }
            }
        )
    }
}
