import Foundation

@MainActor
class DocumentsViewModel: ObservableObject {
    private let repository = BackendDocumentRepository()

    enum State {
        case loading
        case ready([AppDocument])
        case error(String)
    }

    @Published var state: State = .loading

    init() {
        Task { await load() }
    }

    private func load() async {
        do {
            let docs = try await repository.getAllDocuments()
            state = .ready(docs)
        } catch {
            state = .error(error.localizedDescription)
        }
    }
}
