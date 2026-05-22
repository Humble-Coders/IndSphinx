import Foundation
import FirebaseFirestore

@MainActor
class NoticeboardViewModel: ObservableObject {
    private let repo = BackendNoticeboardRepository()
    private var listenerRegistration: ListenerRegistration?

    @Published var notices: [Notice] = []
    @Published var selectedNotice: Notice? = nil
    @Published var questionNotice: Notice? = nil

    init() {
        startListening()
    }

    private func startListening() {
        listenerRegistration = repo.observeNotices { [weak self] updated in
            guard let self else { return }
            Task { @MainActor in
                self.notices = updated
                if let current = self.selectedNotice,
                   let refreshed = updated.first(where: { $0.id == current.id }) {
                    self.selectedNotice = refreshed
                }
                if let current = self.questionNotice,
                   let refreshed = updated.first(where: { $0.id == current.id }) {
                    self.questionNotice = refreshed
                }
                // Resolve pending notice ID
                if let pendingId = self.pendingNoticeId {
                    if let found = updated.first(where: { $0.id == pendingId }) {
                        self.pendingNoticeId = nil
                        self.open(notice: found)
                    }
                }
            }
        }
    }

    deinit {
        listenerRegistration?.remove()
    }

    private var pendingNoticeId: String?

    func onNoticeSelected(_ notice: Notice) {
        open(notice: notice)
    }

    func openNoticeDirectly(_ notice: Notice) {
        let refreshed = notices.first { $0.id == notice.id } ?? notice
        open(notice: refreshed)
    }

    func openNoticeByIdWhenLoaded(_ noticeId: String) {
        if let found = notices.first(where: { $0.id == noticeId }) {
            open(notice: found)
        } else {
            pendingNoticeId = noticeId
        }
    }

    func onBackFromDetail() {
        selectedNotice = nil
        questionNotice = nil
    }

    private func open(notice: Notice) {
        if notice.type == "question" {
            questionNotice = notice
            selectedNotice = nil
        } else {
            selectedNotice = notice
            questionNotice = nil
        }
    }
}
