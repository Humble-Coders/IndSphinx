import Foundation
import UIKit

@MainActor
class ComplaintsViewModel: ObservableObject {
    private let templateRepo = BackendComplaintTemplateRepository()
    private let complaintRepo = BackendComplaintRepository()
    private let storageRepo = BackendStorageRepository()

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
        flatId: String,
        images: [UIImage] = [],
        videoURLs: [URL] = []
    ) {
        guard case .submitForm(_, let template) = state else { return }
        state = .submitting(selected: template)

        let storageRepo = self.storageRepo
        let complaintRepo = self.complaintRepo

        Task {
            do {
                let uploadId = UUID().uuidString

                // Upload all photos and videos in parallel
                var orderedUrls = [String](repeating: "", count: images.count + videoURLs.count)

                try await withThrowingTaskGroup(of: (Int, String).self) { group in
                    for (index, image) in images.enumerated() {
                        group.addTask {
                            // Compress + resize: ~5 MB photo → ~250 KB
                            let data = image.compressedForUpload()
                            let path = "complaints/\(uploadId)/photo_\(index).jpg"
                            let url = try await storageRepo.uploadData(data, path: path)
                            return (index, url)
                        }
                    }
                    for (index, videoURL) in videoURLs.enumerated() {
                        let slot = images.count + index
                        group.addTask {
                            let path = "complaints/\(uploadId)/video_\(index).mp4"
                            let url = try await storageRepo.uploadFile(from: videoURL, path: path)
                            return (slot, url)
                        }
                    }
                    for try await (slot, url) in group {
                        if slot < orderedUrls.count {
                            orderedUrls[slot] = url
                        }
                    }
                }

                let mediaUrls = orderedUrls.filter { !$0.isEmpty }

                _ = try await complaintRepo.submitComplaint(
                    flatNumber: flatNumber,
                    flatId: flatId,
                    occupantEmail: occupantEmail,
                    occupantName: occupantName,
                    occupantId: occupantDocId,
                    category: template.category,
                    priority: priority,
                    description: description,
                    problem: problem,
                    mediaUrls: mediaUrls
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

// MARK: - Image compression helper

private extension UIImage {
    /**
     * Scales to max 1280px on longest side, then compresses at 75% JPEG quality.
     * Reduces a typical 5–8 MB phone photo to ~250–400 KB (15–20× smaller).
     */
    func compressedForUpload() -> Data {
        let maxDimension: CGFloat = 1280
        let longestSide = max(size.width, size.height)
        let scale = min(maxDimension / longestSide, 1.0)

        let targetSize = scale < 1.0
            ? CGSize(width: (size.width * scale).rounded(), height: (size.height * scale).rounded())
            : size

        let renderer = UIGraphicsImageRenderer(size: targetSize)
        let scaled = renderer.image { _ in
            self.draw(in: CGRect(origin: .zero, size: targetSize))
        }
        return scaled.jpegData(compressionQuality: 0.75) ?? Data()
    }
}
