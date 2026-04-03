import Foundation
import FirebaseFirestore

let responsibilities: [String] = [
    "I confirm that I have received the allocated accommodation.",
    "I will maintain cleanliness and hygiene of the flat.",
    "I will not allow unauthorized visitors.",
    "I will follow company housing policies.",
    "I will report maintenance issues through the app."
]

@MainActor
class ResidentialFormViewModel: ObservableObject {
    private let authRepository = IOSAuthRepository()
    private let userProfileRepository = BackendUserProfileRepository()
    private let formRepository = BackendResidentialFormRepository()

    enum State {
        case loading
        case ready(
            occupantName: String,
            empId: String,
            flatNumber: String,
            occupantFrom: Date?,
            occupantDocId: String,
            flatId: String,
            commonAmenities: [String],
            roomAmenities: [String],
            selectedAmenities: Set<String>,
            checkedResponsibilities: Set<Int>,
            termsHtml: String,
            termsAccepted: Bool,
            showTermsSheet: Bool,
            isSubmitting: Bool
        )
        case error(String)
        case submitted
    }

    @Published var state: State = .loading

    init() {
        Task { await loadForm() }
    }

    private func loadForm() async {
        guard let user = authRepository.getCurrentUser() else {
            state = .error("Session expired. Please sign in again.")
            return
        }
        do {
            let profile = try await userProfileRepository.getProfile(uid: user.uid)
            guard profile.enabled else { state = .error("Your account has been disabled."); return }
            if profile.hasAcceptedAgreement { state = .submitted; return }
            let (common, room) = try await formRepository.getFlatAmenities(flatId: profile.flatId)
            let termsHtml = try await formRepository.getTermsAndConditions()
            state = .ready(
                occupantName: profile.name,
                empId: profile.empId,
                flatNumber: profile.flatNumber,
                occupantFrom: profile.occupantFrom,
                occupantDocId: profile.occupantDocId,
                flatId: profile.flatId,
                commonAmenities: common,
                roomAmenities: room,
                selectedAmenities: [],
                checkedResponsibilities: [],
                termsHtml: termsHtml,
                termsAccepted: false,
                showTermsSheet: false,
                isSubmitting: false
            )
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    private func readyValues() -> (String, String, String, Date?, String, String, [String], [String], Set<String>, Set<Int>, String, Bool, Bool, Bool)? {
        guard case .ready(let n, let e, let fn, let of, let od, let fi, let ca, let ra, let sa, let cr, let th, let ta, let st, let sub) = state else { return nil }
        return (n, e, fn, of, od, fi, ca, ra, sa, cr, th, ta, st, sub)
    }

    func toggleAmenity(_ amenity: String) {
        guard let (n, e, fn, of, od, fi, ca, ra, sa, cr, th, ta, st, sub) = readyValues() else { return }
        var updated = sa
        if updated.contains(amenity) { updated.remove(amenity) } else { updated.insert(amenity) }
        state = .ready(occupantName: n, empId: e, flatNumber: fn, occupantFrom: of, occupantDocId: od, flatId: fi,
                       commonAmenities: ca, roomAmenities: ra, selectedAmenities: updated,
                       checkedResponsibilities: cr, termsHtml: th, termsAccepted: ta, showTermsSheet: st, isSubmitting: sub)
    }

    func toggleResponsibility(_ index: Int) {
        guard let (n, e, fn, of, od, fi, ca, ra, sa, cr, th, ta, st, sub) = readyValues() else { return }
        var updated = cr
        if updated.contains(index) { updated.remove(index) } else { updated.insert(index) }
        state = .ready(occupantName: n, empId: e, flatNumber: fn, occupantFrom: of, occupantDocId: od, flatId: fi,
                       commonAmenities: ca, roomAmenities: ra, selectedAmenities: sa,
                       checkedResponsibilities: updated, termsHtml: th, termsAccepted: ta, showTermsSheet: st, isSubmitting: sub)
    }

    func setTermsAccepted(_ accepted: Bool) {
        guard let (n, e, fn, of, od, fi, ca, ra, sa, cr, th, _, st, sub) = readyValues() else { return }
        state = .ready(occupantName: n, empId: e, flatNumber: fn, occupantFrom: of, occupantDocId: od, flatId: fi,
                       commonAmenities: ca, roomAmenities: ra, selectedAmenities: sa,
                       checkedResponsibilities: cr, termsHtml: th, termsAccepted: accepted, showTermsSheet: st, isSubmitting: sub)
    }

    func setShowTermsSheet(_ show: Bool) {
        guard let (n, e, fn, of, od, fi, ca, ra, sa, cr, th, ta, _, sub) = readyValues() else { return }
        state = .ready(occupantName: n, empId: e, flatNumber: fn, occupantFrom: of, occupantDocId: od, flatId: fi,
                       commonAmenities: ca, roomAmenities: ra, selectedAmenities: sa,
                       checkedResponsibilities: cr, termsHtml: th, termsAccepted: ta, showTermsSheet: show, isSubmitting: sub)
    }

    func submitForm() {
        guard let (n, e, fn, of, od, fi, ca, ra, sa, cr, th, ta, st, _) = readyValues() else { return }
        guard ta && cr.count == responsibilities.count else { return }
        state = .ready(occupantName: n, empId: e, flatNumber: fn, occupantFrom: of, occupantDocId: od, flatId: fi,
                       commonAmenities: ca, roomAmenities: ra, selectedAmenities: sa,
                       checkedResponsibilities: cr, termsHtml: th, termsAccepted: ta, showTermsSheet: st, isSubmitting: true)
        Task {
            do {
                try await formRepository.submitAgreement(
                    occupantDocId: od, occupantName: n, empId: e, flatNumber: fn, flatId: fi,
                    selectedAmenities: Array(sa), termsAccepted: true
                )
                state = .submitted
            } catch {
                state = .ready(occupantName: n, empId: e, flatNumber: fn, occupantFrom: of, occupantDocId: od, flatId: fi,
                               commonAmenities: ca, roomAmenities: ra, selectedAmenities: sa,
                               checkedResponsibilities: cr, termsHtml: th, termsAccepted: ta, showTermsSheet: st, isSubmitting: false)
            }
        }
    }
}
