import Foundation
import FirebaseFirestore

@MainActor
class NoticeboardViewModel: ObservableObject {
    private let repo = BackendNoticeboardRepository()
    private var listenerRegistration: ListenerRegistration?

    @Published var notices: [Notice] = []
    @Published var selectedNotice: Notice? = nil

    init() {
        startListening()
    }

    private func startListening() {
        listenerRegistration = repo.observeNotices { [weak self] updated in
            guard let self else { return }
            Task { @MainActor in
                self.notices = updated
                // Refresh selected notice if detail is open
                if let current = self.selectedNotice,
                   let refreshed = updated.first(where: { $0.id == current.id }) {
                    self.selectedNotice = refreshed
                }
            }
        }
    }

    deinit {
        listenerRegistration?.remove()
    }

    func onNoticeSelected(_ notice: Notice) {
        selectedNotice = notice
    }

    func openNoticeDirectly(_ notice: Notice) {
        let refreshed = notices.first { $0.id == notice.id } ?? notice
        selectedNotice = refreshed
    }

    func onBackFromDetail() {
        selectedNotice = nil
    }
}
