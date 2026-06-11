package com.cuentamorosos

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.cuentamorosos.data.CuentaMorososLocalStore
import com.cuentamorosos.data.FirebaseUserSyncManager
import com.cuentamorosos.data.NetworkMonitorFactory
import com.cuentamorosos.data.NotificationScheduler
import com.cuentamorosos.data.ReminderWorker
import com.cuentamorosos.db.DriverFactory
import com.cuentamorosos.model.UserPreferences
import com.cuentamorosos.notifications.DeepLinkTarget
import com.cuentamorosos.notifications.NotificationDispatcher
import com.cuentamorosos.ui.CuentaMorososApp
import com.cuentamorosos.ui.CuentaMorososTheme
import com.cuentamorosos.ui.OnPhotoReady
import com.cuentamorosos.ui.auth.ForgotPasswordScreen
import com.cuentamorosos.ui.auth.LoginScreen
import com.cuentamorosos.ui.auth.RegisterScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainActivity : ComponentActivity() {

    private lateinit var repositoryProvider: RepositoryProvider
    private lateinit var localStore: CuentaMorososLocalStore
    private lateinit var networkMonitor: com.cuentamorosos.data.NetworkMonitor
    private lateinit var notificationDispatcher: NotificationDispatcher

    // Deep link SharedFlow
    private val _deepLinkEvent = MutableSharedFlow<DeepLinkTarget>(extraBufferCapacity = 1)
    val deepLinkEvent: SharedFlow<DeepLinkTarget> = _deepLinkEvent.asSharedFlow()

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Log.w(TAG, "POST_NOTIFICATIONS permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prevent Firebase Firestore permission errors from crashing the app
        // while Firestore rules are being configured
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is com.google.firebase.firestore.FirebaseFirestoreException &&
                throwable.message?.contains("PERMISSION_DENIED") == true
            ) {
                println("[MainActivity] Firestore PERMISSION_DENIED caught: ${throwable.message}")
                println("[MainActivity] Check Firestore Database rules in Firebase Console")
                // Don't crash — the Flow's catch operator will emit emptyList()
            } else {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        // Initialize NotificationDispatcher (replaces NotificationScheduler.ensureChannel)
        notificationDispatcher = NotificationDispatcher(this)

        // Request POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Ensure legacy notification channel exists (backward compat)
        NotificationScheduler.ensureChannel(this)

        // Initialize SQLDelight driver and repositories
        val sqlDriver = DriverFactory(applicationContext).createDriver()
        networkMonitor = NetworkMonitorFactory(applicationContext).create()
        repositoryProvider = RepositoryProvider(sqlDriver, networkMonitor)
        localStore = CuentaMorososLocalStore(applicationContext)

        // Store RepositoryProvider in Application for Worker access
        (application as CuentaMorososApp).repositoryProvider = repositoryProvider

        // Sync Firebase user on startup if already logged in
        // Profile sync happens in MainAppContent LaunchedEffect (non-blocking)

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
                            repositoryProvider = repositoryProvider,
                            localStore = localStore,
                            networkMonitor = networkMonitor,
                            application = application,
                            notificationDispatcher = notificationDispatcher,
                            deepLinkEvent = deepLinkEvent,
                            onTestNotification = { notificationEvent ->
                                notificationDispatcher.dispatch(notificationEvent)
                            },
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

        // Handle deep link from cold start
        handleDeepLinkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        intent ?: return
        val type = intent.getStringExtra(NotificationDispatcher.EXTRA_NOTIFICATION_TYPE) ?: return
        val eventId = intent.getStringExtra(NotificationDispatcher.EXTRA_EVENT_ID)
        val pagerPage = intent.getIntExtra(NotificationDispatcher.EXTRA_PAGER_PAGE, 0)

        _deepLinkEvent.tryEmit(DeepLinkTarget(pagerPage, eventId, type))

        // Clear extras to avoid re-processing on config change
        intent.removeExtra(NotificationDispatcher.EXTRA_NOTIFICATION_TYPE)
        intent.removeExtra(NotificationDispatcher.EXTRA_EVENT_ID)
        intent.removeExtra(NotificationDispatcher.EXTRA_INVITATION_ID)
        intent.removeExtra(NotificationDispatcher.EXTRA_PAGER_PAGE)
    }
}

/**
 * Main app content shown when user is authenticated.
 */
@Composable
private fun MainAppContent(
    user: com.google.firebase.auth.FirebaseUser,
    repositoryProvider: RepositoryProvider,
    localStore: CuentaMorososLocalStore,
    networkMonitor: com.cuentamorosos.data.NetworkMonitor,
    application: android.app.Application,
    notificationDispatcher: NotificationDispatcher,
    deepLinkEvent: SharedFlow<DeepLinkTarget>,
    onTestNotification: (com.cuentamorosos.notifications.NotificationEvent) -> Unit,
) {
    // Create NotificationCallbacks that dispatch via NotificationDispatcher
    val notificationCallbacks = remember {
        NotificationCallbacks(
            onInvitationReceived = { event -> notificationDispatcher.dispatch(event) },
            onInvitationAccepted = { event -> notificationDispatcher.dispatch(event) },
            onCalculationCompleted = { event -> notificationDispatcher.dispatch(event) },
        )
    }

    val viewModelFactory = remember(user.uid) {
        AppViewModelFactory(
            repositoryProvider,
            currentProfileId = user.uid,
            notificationCallbacks = notificationCallbacks,
        )
    }
    var preferences by remember { mutableStateOf(localStore.loadPreferences()) }

    // Start staggered sync after first render AND on user change
    val syncScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    LaunchedEffect(user.uid) {
        // Profile sync runs in background (non-blocking)
        runCatching {
            FirebaseUserSyncManager.syncCurrentUser()
            FirebaseUserSyncManager.ensureOwnProfile()
        }.onFailure { e ->
            println("[MainActivity] Profile sync failed: ${e.message}")
        }
        repositoryProvider.startSyncStaggered(syncScope)
    }

    // ── Photo picker bridge ───────────────────────────────────────────────────
    // Capture context for image compression
    val context = LocalContext.current

    // Holds the callback that will receive the download URL after upload
    var pendingPhotoCallback by remember { mutableStateOf<OnPhotoReady?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            println("[MainActivity] Photo picker onResult: uri=$uri, hasCallback=${pendingPhotoCallback != null}")
            val callback = pendingPhotoCallback
            pendingPhotoCallback = null
            if (uri != null && callback != null) {
                val currentUid = user.uid

                // 1. Compress image to 256x256 JPEG 85%, read as bytes
                val imageBytes = compressImageToBytes(context, uri)
                if (imageBytes == null) {
                    println("[MainActivity] Failed to compress image, aborting")
                    return@rememberLauncherForActivityResult
                }
                println("[MainActivity] Image compressed: ${imageBytes.size} bytes")

                // 2. Upload to Firebase Storage
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("avatars/$currentUid/profile.jpg")
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build()

                storageRef.putBytes(imageBytes, metadata)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                            println("[MainActivity] Photo uploaded successfully: $downloadUrl")

                            // 3. Update FirebaseAuth profile photo
                            FirebaseAuth.getInstance().currentUser?.updateProfile(
                                UserProfileChangeRequest.Builder()
                                    .setPhotoUri(downloadUrl)
                                    .build()
                            )?.addOnSuccessListener {
                                println("[MainActivity] FirebaseAuth profile photo updated")
                            }?.addOnFailureListener { e ->
                                println("[MainActivity] FirebaseAuth profile photo update failed: ${e.message}")
                            }

                            // 4. Pass download URL to ViewModel
                            callback(downloadUrl.toString())
                        }.addOnFailureListener { e ->
                            println("[MainActivity] Failed to get download URL: ${e.message}")
                        }
                    }
                    .addOnFailureListener { e ->
                        println("[MainActivity] Failed to upload photo to Storage: ${e.message}")
                    }
            }
        }
    )

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
            repositoryProvider.clearLocalData()
            localStore.clearAll()
            FirebaseAuth.getInstance().signOut()
        },
        onPickPhoto = { onPhotoReady ->
            println("[MainActivity] Photo picker requested, launching image/* picker")
            pendingPhotoCallback = onPhotoReady
            photoPickerLauncher.launch("image/*")
        },
        deepLinkEvent = deepLinkEvent,
        onTestNotification = onTestNotification,
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
                    onAuthSuccess(user)  // Auth succeeds immediately
                    // Profile sync happens in MainAppContent LaunchedEffect
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
                    onAuthSuccess(user)  // Auth succeeds immediately
                    // Profile sync happens in MainAppContent LaunchedEffect
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

private const val TAG = "MainActivity"

/**
 * Compresses an image from a content URI to a 256x256 JPEG at 85% quality.
 * Returns the compressed bytes, or null if compression fails.
 */
private fun compressImageToBytes(context: android.content.Context, uri: Uri): ByteArray? {
    return try {
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
            ?: return null
        val scaled = Bitmap.createScaledBitmap(bitmap, MAX_PHOTO_SIZE, MAX_PHOTO_SIZE, true)
        if (scaled != bitmap) bitmap.recycle()

        val output = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, output)
        scaled.recycle()

        output.toByteArray()
    } catch (e: Exception) {
        println("[MainActivity] Image compression failed: ${e.message}")
        null
    }
}

private const val MAX_PHOTO_SIZE = 256
