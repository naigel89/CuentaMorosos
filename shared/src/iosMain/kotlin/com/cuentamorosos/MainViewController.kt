package com.cuentamorosos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.cuentamorosos.data.IosUserPreferencesStore
import com.cuentamorosos.data.NetworkMonitorFactory
import com.cuentamorosos.db.DriverFactory
import com.cuentamorosos.notifications.DeepLinkTarget
import com.cuentamorosos.notifications.IosNotificationDedupStore
import com.cuentamorosos.notifications.IosNotificationPresenter
import com.cuentamorosos.notifications.IosReminderScheduler
import com.cuentamorosos.notifications.NotificationCoordinator
import com.cuentamorosos.notifications.NotificationEvent
import com.cuentamorosos.notifications.PushPayloadParser
import com.cuentamorosos.ui.CuentaMorososApp
import com.cuentamorosos.ui.CuentaMorososLogo
import com.cuentamorosos.ui.CuentaMorososTheme
import com.cuentamorosos.ui.auth.SplashAuthScreen
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import platform.UIKit.UIViewController

/**
 * Punto de entrada del host iOS. **Es el único símbolo que Swift debe conocer**
 * del módulo compartido (requisito R002 de `migrate-ios-mvp`): cada símbolo extra
 * que cruce la frontera es superficie que solo el runner macOS puede validar.
 *
 * El host de Swift debe haber llamado a `FirebaseApp.configure()` antes, porque
 * los repositorios de gitlive resuelven `Firebase.auth` y `Firebase.firestore`
 * en cuanto se construye el [RepositoryProvider].
 */
fun MainViewController(): UIViewController =
    ComposeUIViewController { sharedBridge.Content() }

/**
 * El bridge de la app, para que los delegados de UIKit puedan empujarle push y
 * pulsaciones de notificación.
 *
 * Es el segundo —y último— símbolo que Swift referencia. R002 pedía uno solo,
 * pero sin este el deep link al abrir una notificación (R104) es inalcanzable:
 * `MainViewController()` solo devuelve el controller y el bridge queda dentro.
 */
fun appBridge(): IosAppBridge = sharedBridge

/**
 * Instancia única. Antes se creaba una nueva por llamada a [MainViewController],
 * lo que habría abierto un segundo driver de SQLDelight sobre el mismo archivo
 * si UIKit reconstruía la vista.
 */
private val sharedBridge = IosAppBridge()

/**
 * Estado del host que debe sobrevivir a las recomposiciones: driver de base de
 * datos, repositorios, puertos de notificaciones y el canal de deep links.
 *
 * Existe como clase, y no como una función con `remember`, para que Swift pueda
 * empujarle push y pulsaciones de notificación desde sus delegados de UIKit —
 * que ocurren fuera de cualquier composición.
 */
class IosAppBridge {

    private val driver = DriverFactory().createDriver()
    private val networkMonitor = NetworkMonitorFactory().create()
    private val repositoryProvider = RepositoryProvider(driver, networkMonitor)
    private val preferencesStore = IosUserPreferencesStore()

    private val presenter = IosNotificationPresenter()
    private val reminderScheduler = IosReminderScheduler()
    private val coordinator = NotificationCoordinator(
        presenter = presenter,
        dedupStore = IosNotificationDedupStore(),
    )

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _deepLinks = MutableSharedFlow<DeepLinkTarget>(extraBufferCapacity = 1)

    // ── API para el host de Swift ───────────────────────────────────────────

    /**
     * Entrega el payload de una push. Devuelve `true` si produjo una
     * notificación; `false` si el payload no era reconocible o estaba duplicada.
     *
     * El parseo y la deduplicación son los mismos que usa Android.
     */
    fun handlePushPayload(payload: Map<String, String>): Boolean {
        val event = PushPayloadParser.parse(payload) ?: return false
        return coordinator.dispatch(event)
    }

    /** El usuario abrió una notificación: navega a donde apunte su deep link. */
    fun handleNotificationOpened(
        notificationType: String,
        pagerPage: Int,
        eventId: String?,
    ) {
        _deepLinks.tryEmit(DeepLinkTarget(pagerPage, eventId, notificationType))
    }

    /** Relee el permiso de notificaciones. Llamar al arrancar y al volver a primer plano. */
    fun refreshNotificationPermission() {
        presenter.refreshAuthorizationStatus()
    }

    // ── Composición ─────────────────────────────────────────────────────────

    @Composable
    fun Content() {
        val user by Firebase.auth.authStateChanged.collectAsState(
            initial = Firebase.auth.currentUser,
        )
        var preferences by remember { mutableStateOf(preferencesStore.load()) }

        CuentaMorososTheme(preferences = preferences) {
            Surface(modifier = Modifier.fillMaxSize()) {
                val currentUser = user
                if (currentUser == null) {
                    LoginGate()
                } else {
                    val uid = currentUser.uid

                    LaunchedEffect(uid) {
                        repositoryProvider.startSyncStaggered(syncScope)
                    }

                    LaunchedEffect(preferences.remindersEnabled) {
                        if (preferences.remindersEnabled) {
                            reminderScheduler.schedule()
                        } else {
                            reminderScheduler.cancel()
                        }
                    }

                    DisposableEffect(Unit) {
                        presenter.ensureChannels()
                        presenter.refreshAuthorizationStatus()
                        onDispose { }
                    }

                    // Con clave el uid, como en MainActivity: sin remember se
                    // construiría una factory nueva en cada recomposición.
                    val viewModelFactory = remember(uid) {
                        AppViewModelFactory(
                            repositoryProvider,
                            currentProfileId = uid,
                            notificationCallbacks = NotificationCallbacks(
                                onInvitationReceived = { coordinator.dispatch(it) },
                                onInvitationAccepted = { coordinator.dispatch(it) },
                                onCalculationCompleted = { coordinator.dispatch(it) },
                            ),
                        )
                    }

                    CuentaMorososApp(
                        viewModelFactory = viewModelFactory,
                        currentUserUid = uid,
                        preferences = preferences,
                        onSavePreferences = { updated ->
                            preferences = updated
                            preferencesStore.save(updated)
                        },
                        onScheduleReminders = { reminderScheduler.schedule() },
                        onCancelReminders = { reminderScheduler.cancel() },
                        networkMonitor = networkMonitor,
                        onSignOut = {
                            syncScope.launch { Firebase.auth.signOut() }
                        },
                        // Fuera de alcance en el MVP: subir la foto necesita el
                        // tipo Data de gitlive, que es expect por plataforma.
                        // Ver design.md § Tradeoffs. La foto se ve, no se cambia.
                        onPickPhoto = null,
                        deepLinkEvent = _deepLinks.asSharedFlow(),
                        onTestNotification = { event: NotificationEvent ->
                            coordinator.dispatch(event)
                        },
                        profileRepository = repositoryProvider.profileRepository,
                    )
                }
            }
        }
    }

    @Composable
    private fun LoginGate() {
        val scope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(false) }

        SplashAuthScreen(
            logo = { modifier -> CuentaMorososLogo(modifier) },
            isLoading = isLoading,
            onLoginSuccess = {
                // authStateChanged reemite y Content() recompone al usuario.
            },
            // MVP: el alta y la recuperación de contraseña siguen siendo solo de
            // Android. Las pantallas ya existen en commonMain; conectarlas aquí
            // es trabajo de la fase siguiente, no de la que persigue compilar.
            onNavigateToRegister = { },
            onNavigateToForgotPassword = { },
            onLogin = { email, password, onResult ->
                isLoading = true
                scope.launch {
                    val error = runCatching {
                        Firebase.auth.signInWithEmailAndPassword(email, password)
                    }.exceptionOrNull()
                    isLoading = false
                    onResult(error?.message ?: if (error != null) "Error al iniciar sesión" else null)
                }
            },
        )
    }
}
