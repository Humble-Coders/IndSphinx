import Foundation

let cleanlinessItems = ["Toilet & Bathroom", "Kitchen", "Living Room", "Bedrooms", "Dustbin / Garbage"]
let repairItems = ["Electrical (lights, fan, switches)", "Plumbing (tap, flush, leakage)",
                   "Kitchen sink & exhaust", "Doors / Windows / Locks", "Furniture (bed, almirah)"]
let safetyItems = ["No unauthorized guests", "No damage to property",
                   "No complaints from neighbors", "Flat maintained in good condition"]
let billItems = ["Electricity", "Water"]

@MainActor
class CoordinatorFormViewModel: ObservableObject {
    private let repository = BackendCoordinatorFormRepository()

    let occupantId: String
    let flatId: String
    let coordinatorName: String
    let flatNumber: String
    let month: String

    @Published var cleanliness: [String: String?]
    @Published var repairs: [String: String?]
    @Published var safety: [String: String?]
    @Published var bills: [String: String?]
    @Published var hrIssues: String = ""
    @Published var confirmed: Bool = false
    @Published var isSubmitting: Bool = false
    @Published var isSubmitted: Bool = false
    @Published var error: String? = nil

    var canSubmit: Bool {
        cleanliness.values.allSatisfy { $0 != nil } &&
        repairs.values.allSatisfy { $0 != nil } &&
        safety.values.allSatisfy { $0 != nil } &&
        bills.values.allSatisfy { $0 != nil } &&
        confirmed && !isSubmitting
    }

    init(occupantId: String, flatId: String, coordinatorName: String, flatNumber: String) {
        self.occupantId = occupantId
        self.flatId = flatId
        self.coordinatorName = coordinatorName
        self.flatNumber = flatNumber
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        self.month = formatter.string(from: Date())
        self.cleanliness = Dictionary(uniqueKeysWithValues: cleanlinessItems.map { ($0, nil) })
        self.repairs = Dictionary(uniqueKeysWithValues: repairItems.map { ($0, nil) })
        self.safety = Dictionary(uniqueKeysWithValues: safetyItems.map { ($0, nil) })
        self.bills = Dictionary(uniqueKeysWithValues: billItems.map { ($0, nil) })
    }

    func select(section: Section, item: String, value: String) {
        switch section {
        case .cleanliness: cleanliness[item] = value
        case .repairs: repairs[item] = value
        case .safety: safety[item] = value
        case .bills: bills[item] = value
        }
    }

    enum Section { case cleanliness, repairs, safety, bills }

    func submit() {
        guard canSubmit else { return }
        isSubmitting = true
        error = nil
        Task {
            do {
                try await repository.submitForm(
                    occupantId: occupantId,
                    flatId: flatId,
                    occupantName: coordinatorName,
                    flatNumber: flatNumber,
                    month: month,
                    cleanliness: cleanliness.compactMapValues { $0 },
                    repairs: repairs.compactMapValues { $0 },
                    safety: safety.compactMapValues { $0 },
                    bills: bills.compactMapValues { $0 },
                    hrIssues: hrIssues
                )
                isSubmitting = false
                isSubmitted = true
            } catch {
                self.isSubmitting = false
                self.error = error.localizedDescription
            }
        }
    }
}
