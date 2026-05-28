import SwiftUI

enum AppScreen {
    case splash
    case login
    case residentialForm
    case home
}

struct ContentView: View {
    @State private var currentScreen: AppScreen = .splash

    var body: some View {
        ZStack {
            switch currentScreen {
            case .splash:
                SplashView { destination in
                    withAnimation(.easeInOut(duration: 0.4)) {
                        currentScreen = destination
                    }
                }
                .transition(.opacity)
            case .login:
                LoginView(
                    onAuthSuccess: { needsAgreement in
                        withAnimation(.easeInOut(duration: 0.4)) {
                            currentScreen = needsAgreement ? .residentialForm : .home
                        }
                    }
                )
                .transition(.opacity)
            case .residentialForm:
                ResidentialFormView(onFormComplete: {
                    withAnimation(.easeInOut(duration: 0.4)) {
                        currentScreen = .home
                    }
                })
                .transition(.opacity)
            case .home:
                HomeView(onSignOut: {
                    withAnimation(.easeInOut(duration: 0.4)) {
                        currentScreen = .login
                    }
                })
                .transition(.opacity)
            }
        }
    }
}
