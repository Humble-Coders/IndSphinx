import SwiftUI
import FirebaseAuth
import FirebaseCore
import FirebaseMessaging
import UserNotifications

private let tag = "[FCM-iOS]"

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    private static let pendingFcmTokenKey = "pending_fcm_token"

    private var authStateListener: AuthStateDidChangeListenerHandle?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        print("\(tag) didFinishLaunching — configuring Firebase")
        FirebaseApp.configure()
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

        authStateListener = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            if let uid = user?.uid {
                print("\(tag) Auth state changed — user signed in uid=\(uid)")
            } else {
                print("\(tag) Auth state changed — no user signed in")
            }
            self?.pushFcmTokenToFirestoreIfPossible(trigger: "authStateChange")
        }

        print("\(tag) Calling registerForRemoteNotifications")
        application.registerForRemoteNotifications()

        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if let error {
                print("\(tag) requestAuthorization error: \(error)")
            } else {
                print("\(tag) Notification permission granted=\(granted)")
            }
        }

        return true
    }

    deinit {
        if let authStateListener {
            Auth.auth().removeStateDidChangeListener(authStateListener)
        }
    }

    private func pushFcmTokenToFirestoreIfPossible(trigger: String) {
        let uid = Auth.auth().currentUser?.uid
        print("\(tag) pushFcmTokenToFirestoreIfPossible trigger=\(trigger) uid=\(uid ?? "nil")")

        guard let uid else {
            print("\(tag) Skipping Firestore write — no signed-in user yet (token will be saved when user signs in)")
            return
        }

        let sdkToken = Messaging.messaging().fcmToken.flatMap { $0.isEmpty ? nil : $0 }
        let pendingToken = UserDefaults.standard.string(forKey: Self.pendingFcmTokenKey)
        let token = sdkToken ?? pendingToken

        print("\(tag) sdkFcmToken=\(sdkToken ?? "nil") pendingToken=\(pendingToken ?? "nil")")

        guard let token, !token.isEmpty else {
            print("\(tag) Skipping Firestore write — no FCM token available yet")
            return
        }

        print("\(tag) Writing fcm_token to Firestore for uid=\(uid) token prefix=\(String(token.prefix(20)))…")
        Task {
            do {
                try await BackendUserProfileRepository().updateFcmToken(uid: uid, token: token)
                print("\(tag) ✅ fcm_token saved to Firestore successfully")
                UserDefaults.standard.removeObject(forKey: Self.pendingFcmTokenKey)
            } catch {
                print("\(tag) ❌ Firestore write failed: \(error)")
            }
        }
    }

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let hex = deviceToken.map { String(format: "%02x", $0) }.joined()
        print("\(tag) didRegisterForRemoteNotificationsWithDeviceToken apnsToken prefix=\(String(hex.prefix(20)))…")
        Messaging.messaging().apnsToken = deviceToken

        print("\(tag) APNs token set — requesting FCM token explicitly")
        Messaging.messaging().token { [weak self] token, error in
            if let error {
                print("\(tag) ❌ Messaging.token() error after APNs set: \(error)")
                return
            }
            guard let token, !token.isEmpty else {
                print("\(tag) ❌ Messaging.token() returned nil/empty after APNs set")
                return
            }
            print("\(tag) Messaging.token() returned token prefix=\(String(token.prefix(20)))…")
            UserDefaults.standard.set(token, forKey: Self.pendingFcmTokenKey)
            self?.pushFcmTokenToFirestoreIfPossible(trigger: "didRegisterAPNs")
        }
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("\(tag) ❌ didFailToRegisterForRemoteNotifications: \(error)")
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("\(tag) messaging didReceiveRegistrationToken fcmToken=\(fcmToken ?? "nil")")
        guard let token = fcmToken, !token.isEmpty else {
            print("\(tag) Ignoring nil/empty FCM token from delegate")
            return
        }
        UserDefaults.standard.set(token, forKey: Self.pendingFcmTokenKey)
        pushFcmTokenToFirestoreIfPossible(trigger: "didReceiveRegistrationToken")
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .badge, .sound])
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
