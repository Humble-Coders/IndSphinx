import Foundation
import UIKit
import AVFoundation
import FirebaseFirestore

@MainActor
class ComplaintsViewModel: ObservableObject {
    private let templateRepo = BackendComplaintTemplateRepository()
    private let complaintRepo = BackendComplaintRepository()
    private let storageRepo = BackendStorageRepository()

    private var templatesListener: ListenerRegistration?
    private var complaintsListener: ListenerRegistration?

    enum State: Equatable {
        case landing
        case loadingTemplates
        case selectCategory([ComplaintTemplate])
        case submitForm(templates: [ComplaintTemplate], selected: ComplaintTemplate)
        case submitting(selected: ComplaintTemplate)
        case success
        case error(String)
        case loadingComplaints
        case viewComplaints([Complaint])
        case complaintDetail(Complaint, [Complaint])
    }

    @Published var state: State = .landing

    func onAddComplaintTapped() {
        state = .loadingTemplates
        templatesListener?.remove()
        templatesListener = templateRepo.observeTemplates { [weak self] templates in
            guard let self else { return }
            Task { @MainActor in
                switch self.state {
                case .loadingTemplates, .selectCategory:
                    self.state = .selectCategory(templates)
                case .submitForm(_, let selected):
                    self.state = .submitForm(templates: templates, selected: selected)
                default:
                    break
                }
            }
        }
    }

    func onCategorySelected(_ template: ComplaintTemplate) {
        guard case .selectCategory(let templates) = state else { return }
        state = .submitForm(templates: templates, selected: template)
    }

    func onBackFromCategory() {
        templatesListener?.remove()
        templatesListener = nil
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

                var orderedUrls = [String](repeating: "", count: images.count + videoURLs.count)

                try await withThrowingTaskGroup(of: (Int, String).self) { group in
                    for (index, image) in images.enumerated() {
                        group.addTask {
                            let data = image.compressedForUpload()
                            let path = "complaints/\(uploadId)/photo_\(index).jpg"
                            let url = try await storageRepo.uploadData(data, path: path)
                            return (index, url)
                        }
                    }
                    for (index, videoURL) in videoURLs.enumerated() {
                        let slot = images.count + index
                        group.addTask {
                            let compressed = await self.compressVideo(url: videoURL)
                            let path = "complaints/\(uploadId)/video_\(index).mp4"
                            let url = try await storageRepo.uploadFile(from: compressed, path: path)
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

    /**
     * Compresses video using AVAssetExportSession (MEDIUM quality preset).
     */
    private func compressVideo(url: URL) async -> URL {
        let fileSize = (try? url.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0
        if fileSize < 50 * 1024 * 1024 { return url }

        let outputURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString + "_compressed.mp4")

        guard let session = AVAssetExportSession(
            asset: AVURLAsset(url: url),
            presetName: AVAssetExportPresetMediumQuality
        ) else { return url }

        session.outputURL = outputURL
        session.outputFileType = .mp4
        session.shouldOptimizeForNetworkUse = true

        return await withCheckedContinuation { continuation in
            session.exportAsynchronously {
                continuation.resume(returning: session.status == .completed ? outputURL : url)
            }
        }
    }

    func dismissSuccess() {
        state = .landing
    }

    func dismissError() {
        state = .landing
    }

    func onViewComplaintsTapped(occupantId: String) {
        state = .loadingComplaints
        complaintsListener?.remove()
        complaintsListener = complaintRepo.observeByOccupant(occupantId: occupantId) { [weak self] complaints in
            guard let self else { return }
            Task { @MainActor in
                switch self.state {
                case .loadingComplaints, .viewComplaints:
                    self.state = .viewComplaints(complaints)
                case .complaintDetail(let complaint, _):
                    let refreshed = complaints.first { $0.id == complaint.id } ?? complaint
                    self.state = .complaintDetail(refreshed, complaints)
                default:
                    break
                }
            }
        }
    }

    func onComplaintSelected(_ complaint: Complaint) {
        guard case .viewComplaints(let complaints) = state else { return }
        state = .complaintDetail(complaint, complaints)
    }

    func onBackFromDetail() {
        guard case .complaintDetail(_, let complaints) = state else { return }
        state = .viewComplaints(complaints)
    }

    func onBackFromComplaints() {
        complaintsListener?.remove()
        complaintsListener = nil
        state = .landing
    }

    func closeComplaint(id: String, occupantId: String) {
        Task {
            do {
                try await complaintRepo.closeComplaint(id: id)
                // Listener will auto-update the list; navigate back to it
                if case .complaintDetail(_, let complaints) = state {
                    state = .viewComplaints(complaints)
                }
            } catch {
                state = .error(error.localizedDescription)
            }
        }
    }
}

// MARK: - Image compression helper

private extension UIImage {
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
