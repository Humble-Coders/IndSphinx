import SwiftUI

enum AppScreen {
    case splash
    case login
    case home
}

struct ContentView: View {
    @State private var currentScreen: AppScreen = .splash

    var body: some View {
        switch currentScreen {
        case .splash:
            SplashView { isLoggedIn in
                currentScreen = isLoggedIn ? .home : .login
            }
        case .login:
            LoginView(
                onAuthSuccess: { currentScreen = .home },
                onForgotPasswordTap: { /* TODO */ }
            )
        case .home:
            HomeView(onSignOut: { currentScreen = .login })
        }
    }
}
