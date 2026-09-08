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
/// Referencia exactamente dos símbolos del framework: `MainViewController()` y
/// `appBridge()`. Cada símbolo extra es superficie que solo el runner macOS
/// puede validar, así que conviene que la lista no crezca.
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
        ) { _, _ in
            // El puerto cachea el permiso: sin refrescar seguiría con el valor
            // optimista del arranque.
            //
            // Al hilo principal a propósito: este callback llega en uno de
            // fondo, y appBridge() inicializa de forma perezosa la base de datos
            // y los repositorios. Dejarlo aquí los construiría en paralelo con
            // el hilo que monta la UI.
            DispatchQueue.main.async {
                MainViewControllerKt.appBridge().refreshNotificationPermission()
            }
        }

        return true
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // El usuario pudo cambiar el permiso en Ajustes mientras la app dormía.
        MainViewControllerKt.appBridge().refreshNotificationPermission()
    }

    /// Push recibida. El parseo y la deduplicación son los mismos que usa
    /// Android: viven en `commonMain`.
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        let payload = Self.stringPayload(from: userInfo)
        let shown = MainViewControllerKt.appBridge().handlePushPayload(payload: payload)
        completionHandler(shown ? .newData : .noData)
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

    /// El usuario abrió una notificación: el destino sale del `userInfo` que
    /// escribió `IosNotificationPresenter`, con los mismos índices de página que
    /// usa Android.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let info = Self.stringPayload(from: response.notification.request.content.userInfo)
        if let type = info["notificationType"] {
            let page = Int32(info["pagerPage"] ?? "0") ?? 0
            MainViewControllerKt.appBridge().handleNotificationOpened(
                notificationType: type,
                pagerPage: page,
                eventId: info["eventId"]
            )
        }
        completionHandler()
    }

    /// Kotlin espera `Map<String, String>`; UIKit entrega `[AnyHashable: Any]`.
    private static func stringPayload(from userInfo: [AnyHashable: Any]) -> [String: String] {
        var result: [String: String] = [:]
        for (key, value) in userInfo {
            if let k = key as? String, let v = value as? String {
                result[k] = v
            }
        }
        return result
    }
}
