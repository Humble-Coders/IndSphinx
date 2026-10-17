import Foundation
import FirebaseFirestore

@MainActor
class NoticeboardViewModel: ObservableObject {
    private let repo = BackendNoticeboardRepository()
    private var listenerRegistration: ListenerRegistration?

    @Published var notices: [Notice] = []
    @Published var selectedNotice: Notice? = nil
    @Published var questionNotice: Notice? = nil
    @Published var surveyNotice: Notice? = nil

    init() {
        startListening()
    }

    private func startListening() {
        listenerRegistration = repo.observeNotices { [weak self] updated in
            guard let self else { return }
            Task { @MainActor in
                self.notices = updated
                if let current = self.selectedNotice,
                   let refreshed = updated.first(where: { $0.key == current.key }) {
                    self.selectedNotice = refreshed
                }
                if let current = self.questionNotice,
                   let refreshed = updated.first(where: { $0.key == current.key }) {
                    self.questionNotice = refreshed
                }
                if let current = self.surveyNotice,
                   let refreshed = updated.first(where: { $0.key == current.key }) {
                    self.surveyNotice = refreshed
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
        surveyNotice = nil
    }

    private func open(notice: Notice) {
        selectedNotice = nil
        questionNotice = nil
        surveyNotice = nil
        if notice.type == "question" {
            questionNotice = notice
        } else if notice.isSurvey {
            surveyNotice = notice
        } else {
            selectedNotice = notice
        }
    }
}
