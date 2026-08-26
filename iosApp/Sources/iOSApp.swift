import SwiftUI
import UIKit
import UserNotifications
import FirebaseCore
import shared

/// Host iOS de CuentaMorosos.
///
/// Toda la app —UI, ViewModels, repositorios, reglas de notificación— vive en el
/// módulo compartido. Este archivo solo hace tres cosas: arrancar Firebase,
/// montar el `UIViewController` de Compose, y traducir los delegados de UIKit a
/// llamadas al bridge.
///
/// Referencia exactamente un símbolo del framework: `MainViewController()`
/// (requisito R002). El `IosAppBridge` que devuelve se obtiene a través de él.
@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
        }
    }
}

/// Envuelve el `UIViewController` que produce Compose Multiplatform.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Debe ir antes de que el bridge construya los repositorios: gitlive
        // resuelve Firebase.auth y Firebase.firestore al instanciarlos.
        FirebaseApp.configure()

        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .badge, .sound]
        ) { _, _ in }

        return true
    }

    /// Mostrar la notificación aunque la app esté en primer plano; en Android el
    /// dispatcher también las emite en ese caso.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}
