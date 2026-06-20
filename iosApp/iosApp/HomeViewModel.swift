import Foundation
import FirebaseFirestore

@MainActor
class HomeViewModel: ObservableObject {
    enum RevisedFormState {
        case hidden
        case loading
        case ready(commonAmenities: [String], roomAmenities: [String], selectedAmenities: Set<String>, isSubmitting: Bool)
        case error(String)
    }

    private let authRepository = IOSAuthRepository()
    private let userProfileRepository = BackendUserProfileRepository()
    private let noticeboardRepository = BackendNoticeboardRepository()
    private let notificationRepository = BackendNotificationRepository()
    private let formRepository = BackendResidentialFormRepository()
    private let configRepository = BackendConfigRepository()
    private let coordinatorFormRepository = BackendCoordinatorFormRepository()
    private var isEnabledListener: ListenerRegistration?
    private var occupantListener: ListenerRegistration?
    private var noticesListener: ListenerRegistration?
    private var notificationsListener: ListenerRegistration?

    enum State {
        case loading
        case ready(name: String, greeting: String, email: String, role: String, empId: String, flatNumber: String, occupantFrom: Date?, isCoordinator: Bool, occupantDocId: String, flatId: String)
        case accessDenied(reason: String)
    }

    @Published var state: State = .loading
    @Published var shouldSignOut: Bool = false
    @Published var latestNotice: Notice?
    @Published var formDueStatus: (isDue: Bool, frequencyMonths: Int)? = nil
    @Published var notifications: [AppNotification] = []
    var unreadCount: Int { notifications.filter { !$0.isRead }.count }
    @Published var revisedFormState: RevisedFormState = .hidden
    var showRevisedForm: Bool {
        if case .hidden = revisedFormState { return false }
        return true
    }

    init() {
        Task { await loadProfile() }
    }

    deinit {
        isEnabledListener?.remove()
        occupantListener?.remove()
        noticesListener?.remove()
        notificationsListener?.remove()
    }

    private func loadProfile() async {
        guard let user = authRepository.getCurrentUser() else {
            state = .accessDenied(reason: "Session expired. Please sign in again.")
            shouldSignOut = true
            return
        }
        do {
            let profile = try await userProfileRepository.getProfile(uid: user.uid)
            guard profile.enabled else {
                await authRepository.signOutAndClearFcm()
                state = .accessDenied(reason: "Your account has been disabled. Please contact the admin.")
                shouldSignOut = true
                return
            }
            guard profile.role == "OCCUPANT" || profile.role == "COORDINATOR" else {
                await authRepository.signOutAndClearFcm()
                state = .accessDenied(reason: "Access is restricted to occupants and coordinators only.")
                shouldSignOut = true
                return
            }
            state = .ready(
                name: profile.name,
                greeting: greeting(),
                email: profile.email,
                role: profile.role,
                empId: profile.empId,
                flatNumber: profile.flatNumber,
                occupantFrom: profile.occupantFrom,
                isCoordinator: profile.isCoordinator,
                occupantDocId: profile.occupantDocId,
                flatId: profile.flatId
            )
            startObservingEnabled(uid: user.uid)
            startObservingOccupant(occupantDocId: profile.occupantDocId)
            startObservingNotices()
            startObservingNotifications(occupantId: user.uid)
            // Revised-amenities form only makes sense once a flat is
            // assigned. Without a flat, there's nothing to confirm and the
            // Firestore read would blow up on an empty document path.
            if !profile.hasAcceptedRevisedForm && !profile.flatId.isEmpty {
                await loadRevisedFormAmenities(flatId: profile.flatId)
            }
            if profile.isCoordinator { await checkFormDue(occupantDocId: profile.occupantDocId) }
        } catch {
            await authRepository.signOutAndClearFcm()
            state = .accessDenied(reason: error.localizedDescription)
            shouldSignOut = true
        }
    }

    private func startObservingNotices() {
        noticesListener?.remove()
        noticesListener = noticeboardRepository.observeNotices { [weak self] notices in
            Task { @MainActor in
                self?.latestNotice = notices.first
            }
        }
    }

    private func startObservingOccupant(occupantDocId: String) {
        occupantListener?.remove()
        occupantListener = userProfileRepository.observeOccupant(occupantDocId: occupantDocId) { [weak self] data in
            Task { @MainActor in
                guard let self, let data,
                      case .ready(let name, let greeting, let email, let role, let empId,
                                  let flatNumber, let occupantFrom, let isCoordinator,
                                  let occupantDocId, let flatId) = self.state else { return }
                let updatedFlatId = data["flatId"] as? String ?? flatId
                self.state = .ready(
                    name: data["Name"] as? String ?? name,
                    greeting: greeting,
                    email: email,
                    role: role,
                    empId: empId,
                    flatNumber: data["FlatNumber"] as? String ?? flatNumber,
                    occupantFrom: occupantFrom,
                    isCoordinator: data["isCoordinator"] as? Bool ?? isCoordinator,
                    occupantDocId: occupantDocId,
                    flatId: updatedFlatId
                )
                let hasAccepted = data.keys.contains("has_accepted_revised_form")
                    ? (data["has_accepted_revised_form"] as? Bool ?? true)
                    : true
                if !hasAccepted, !updatedFlatId.isEmpty, case .hidden = self.revisedFormState {
                    await self.loadRevisedFormAmenities(flatId: updatedFlatId)
                }
            }
        }
    }

    private func startObservingEnabled(uid: String) {
        isEnabledListener?.remove()
        isEnabledListener = userProfileRepository.observeIsEnabled(uid: uid) { [weak self] enabled in
            guard let self else { return }
            Task { @MainActor in
                if !enabled {
                    self.isEnabledListener?.remove()
                    self.isEnabledListener = nil
                    await self.authRepository.signOutAndClearFcm()
                    self.shouldSignOut = true
                }
            }
        }
    }

    private func checkFormDue(occupantDocId: String) async {
        guard let frequencyMonths = try? await configRepository.getFormFrequencyMonths() else {
            print("[FormDueCheck] checkFormDue: failed to fetch frequencyMonths for occupant=\(occupantDocId)")
            return
        }
        let lastDate = try? await coordinatorFormRepository.getLastFormSubmittedAt(occupantId: occupantDocId)
        let isDue: Bool
        if let last = lastDate {
            print("[FormDueCheck] checkFormDue: last form submitted at \(last) for occupant=\(occupantDocId)")
            let frequencySeconds = Double(frequencyMonths) * 30 * 24 * 60 * 60
            isDue = Date().timeIntervalSince(last) >= frequencySeconds
        } else {
            print("[FormDueCheck] checkFormDue: no previous form found for occupant=\(occupantDocId)")
            isDue = true
        }
        print("[FormDueCheck] checkFormDue: isDue=\(isDue), frequencyMonths=\(frequencyMonths) — dialog will\(isDue ? "" : " NOT") be shown")
        formDueStatus = (isDue: isDue, frequencyMonths: frequencyMonths)
    }

    func dismissFormDue() {
        formDueStatus = nil
    }

    func signOut() {
        isEnabledListener?.remove()
        isEnabledListener = nil
        occupantListener?.remove()
        occupantListener = nil
        noticesListener?.remove()
        noticesListener = nil
        notificationsListener?.remove()
        notificationsListener = nil
        // Run the full cleanup (clear fcm_token → revoke device token → auth
        // sign-out) in a detached Task so it completes even after navigation
        // tears this view model down.
        let repo = authRepository
        Task.detached { await repo.signOutAndClearFcm() }
    }

    private func startObservingNotifications(occupantId: String) {
        notificationsListener?.remove()
        notificationsListener = notificationRepository.observeNotifications(occupantId: occupantId) { [weak self] notifications in
            Task { @MainActor in
                self?.notifications = notifications
            }
        }
    }

    func markNotificationRead(notificationId: String) {
        Task {
            try? await notificationRepository.markAsRead(notificationId: notificationId)
        }
    }

    private func greeting() -> String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 { return "Good Morning" }
        if hour < 17 { return "Good Afternoon" }
        return "Good Evening"
    }

    private func loadRevisedFormAmenities(flatId: String) async {
        if flatId.isEmpty {
            // Defensive: callers should already guard, but never let an empty
            // flatId hit Firestore — it produces an "even segments" error.
            revisedFormState = .hidden
            return
        }
        revisedFormState = .loading
        do {
            let (common, room) = try await formRepository.getFlatAmenities(flatId: flatId)
            revisedFormState = .ready(commonAmenities: common, roomAmenities: room, selectedAmenities: [], isSubmitting: false)
        } catch {
            revisedFormState = .error(error.localizedDescription)
        }
    }

    func toggleRevisedAmenity(_ amenity: String) {
        guard case .ready(let common, let room, var selected, let submitting) = revisedFormState else { return }
        if selected.contains(amenity) { selected.remove(amenity) } else { selected.insert(amenity) }
        revisedFormState = .ready(commonAmenities: common, roomAmenities: room, selectedAmenities: selected, isSubmitting: submitting)
    }

    func submitRevisedForm() {
        guard case .ready(let common, let room, let selected, _) = revisedFormState,
              !selected.isEmpty,
              case .ready(let name, _, _, _, let empId, let flatNumber, _, _, let occupantDocId, let flatId) = state else { return }
        revisedFormState = .ready(commonAmenities: common, roomAmenities: room, selectedAmenities: selected, isSubmitting: true)
        Task {
            do {
                try await formRepository.submitRevisedAgreement(
                    occupantDocId: occupantDocId,
                    occupantName: name,
                    empId: empId,
                    flatNumber: flatNumber,
                    flatId: flatId,
                    selectedAmenities: Array(selected)
                )
                self.revisedFormState = .hidden
            } catch {
                self.revisedFormState = .ready(commonAmenities: common, roomAmenities: room, selectedAmenities: selected, isSubmitting: false)
            }
        }
    }
}
