import Foundation

@MainActor
class ComplaintsViewModel: ObservableObject {
    private let templateRepo = BackendComplaintTemplateRepository()
    private let complaintRepo = BackendComplaintRepository()

    enum State {
        case landing
        case loadingTemplates
        case selectCategory([ComplaintTemplate])
        case submitForm(templates: [ComplaintTemplate], selected: ComplaintTemplate)
        case submitting(selected: ComplaintTemplate)
        case success
        case error(String)
    }

    @Published var state: State = .landing

    func onAddComplaintTapped() {
        state = .loadingTemplates
        Task {
            do {
                let templates = try await templateRepo.getTemplates()
                state = .selectCategory(templates)
            } catch {
                state = .error(error.localizedDescription)
            }
        }
    }

    func onCategorySelected(_ template: ComplaintTemplate) {
        guard case .selectCategory(let templates) = state else { return }
        state = .submitForm(templates: templates, selected: template)
    }

    func onBackFromCategory() {
        state = .landing
    }

    func onBackFromForm() {
        guard case .submitForm(let templates, _) = state else { return }
        state = .selectCategory(templates)
    }

    func submitComplaint(
        problem: String,
        description: String,
        priority: String,
        occupantName: String,
        occupantEmail: String,
        occupantDocId: String,
        flatNumber: String,
        flatId: String
    ) {
        guard case .submitForm(_, let template) = state else { return }
        state = .submitting(selected: template)
        Task {
            do {
                _ = try await complaintRepo.submitComplaint(
                    flatNumber: flatNumber,
                    flatId: flatId,
                    occupantEmail: occupantEmail,
                    occupantName: occupantName,
                    occupantId: occupantDocId,
                    category: template.category,
                    priority: priority,
                    description: description,
                    problem: problem
                )
                state = .success
            } catch {
                state = .error(error.localizedDescription)
            }
        }
    }

    func dismissSuccess() {
        state = .landing
    }

    func dismissError() {
        state = .landing
    }
}
