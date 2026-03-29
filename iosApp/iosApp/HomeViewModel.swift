import Foundation

@MainActor
class HomeViewModel: ObservableObject {
    private let repository = IOSAuthRepository()

    @Published var currentEmail: String = ""

    init() {
        currentEmail = repository.getCurrentUser()?.email ?? ""
    }

    func signOut() {
        try? repository.signOut()
    }
}
