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
        switch currentScreen {
        case .splash:
            SplashView { destination in
                currentScreen = destination
            }
        case .login:
            LoginView(
                onAuthSuccess: { needsAgreement in
                    currentScreen = needsAgreement ? .residentialForm : .home
                }
            )
        case .residentialForm:
            ResidentialFormView(onFormComplete: { currentScreen = .home })
        case .home:
            HomeView(onSignOut: { currentScreen = .login })
        }
    }
}
