import SwiftUI
import FirebaseAuth
import FirebaseMessaging

struct SplashView: View {
    let onSplashComplete: (AppScreen) -> Void

    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [
                    Color(red: 0.165, green: 0.188, blue: 0.502),
                    Color(red: 0.482, green: 0.565, blue: 0.784)
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                RoundedRectangle(cornerRadius: 28)
                    .fill(Color.white)
                    .frame(width: 160, height: 160)
                    .overlay {
                        Image("AppLogo")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .padding(12)
                            .frame(width: 160, height: 160)
                    }

                Spacer().frame(height: 32)

                Text("MY NEST")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.white)
                    .kerning(6)

                Spacer().frame(height: 8)

                Text("By Ind-Sphinx")
                    .font(.system(size: 16, weight: .regular))
                    .foregroundColor(.white.opacity(0.8))

                Spacer().frame(height: 48)

                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    .scaleEffect(1.2)

                Spacer()
            }

            VStack {
                Spacer()
                Text("RESIDENTIAL MAINTENANCE MANAGEMENT")
                    .font(.system(size: 10, weight: .regular))
                    .foregroundColor(.white.opacity(0.5))
                    .kerning(2)
                    .multilineTextAlignment(.center)
                    .padding(.bottom, 48)
            }
        }
        .task {
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            guard let currentUser = Auth.auth().currentUser else {
                onSplashComplete(.login)
                return
            }
            let userProfileRepo = BackendUserProfileRepository()
            let isEnabled = (try? await userProfileRepo.isUserEnabled(uid: currentUser.uid)) ?? true
            if !isEnabled {
                // Disabled-account cold start: full cleanup so the device
                // stops receiving pushes targeting this user.
                await IOSAuthRepository().signOutAndClearFcm()
                onSplashComplete(.login)
                return
            }
            // Best-effort fallback: AppDelegate handles the primary token path.
            print("[FCM-iOS] SplashView — requesting Messaging.token()")
            do {
                let token = try await Messaging.messaging().token()
                if !token.isEmpty {
                    print("[FCM-iOS] SplashView — got token prefix=\(String(token.prefix(20)))… saving to Firestore")
                    try await userProfileRepo.updateFcmToken(uid: currentUser.uid, token: token)
                } else {
                    print("[FCM-iOS] SplashView — Messaging.token() returned empty string")
                }
            } catch {
                print("[FCM-iOS] SplashView — Messaging.token() failed: \(error)")
            }
            // The agreement covers a specific flat, so it can only be presented
            // once one is assigned. Treating "no flat" as nothing-to-sign sends
            // the occupant to Home instead of trapping them on a form that
            // cannot be built.
            let needsAgreement: Bool
            if let profile = try? await userProfileRepo.getProfile(uid: currentUser.uid) {
                needsAgreement = !profile.hasAcceptedAgreement && !profile.flatId.isEmpty
            } else {
                needsAgreement = false
            }
            onSplashComplete(needsAgreement ? .residentialForm : .home)
        }
    }
}
