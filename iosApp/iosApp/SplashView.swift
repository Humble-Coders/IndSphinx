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
                    .fill(Color.white.opacity(0.15))
                    .frame(width: 160, height: 160)
                    .overlay {
                        Image(systemName: "building.2")
                            .font(.system(size: 70, weight: .thin))
                            .foregroundColor(.white)
                    }

                Spacer().frame(height: 32)

                Text("INDSPHINX")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.white)
                    .kerning(6)

                Spacer().frame(height: 8)

                Text("Accommodation System")
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
                try? Auth.auth().signOut()
                onSplashComplete(.login)
                return
            }
            if let token = try? await Messaging.messaging().token() {
                try? await userProfileRepo.updateFcmToken(uid: currentUser.uid, token: token)
            }
            let hasAccepted: Bool
            if let profile = try? await userProfileRepo.getProfile(uid: currentUser.uid) {
                hasAccepted = profile.hasAcceptedAgreement
            } else {
                hasAccepted = true
            }
            onSplashComplete(hasAccepted ? .home : .residentialForm)
        }
    }
}
