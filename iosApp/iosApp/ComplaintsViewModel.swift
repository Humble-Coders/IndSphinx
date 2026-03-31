import Foundation
import UIKit
import AVFoundation

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
        case loadingComplaints
        case viewComplaints([Complaint])
        case complaintDetail(Complaint, [Complaint])
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
     * Typical 50-80 MB video → 5-15 MB (5-10× smaller), much faster upload.
     * Falls back to the original URL on any failure.
     */
    private func compressVideo(url: URL) async -> URL {
        // Skip compression for videos under 50MB
        let fileSize = (try? url.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0
        if fileSize < 50 * 1024 * 1024 {
            return url
        }

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
                if session.status == .completed {
                    continuation.resume(returning: outputURL)
                } else {
                    continuation.resume(returning: url)
                }
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
        Task {
            do {
                let complaints = try await complaintRepo.fetchByOccupant(occupantId: occupantId)
                state = .viewComplaints(complaints)
            } catch {
                state = .error(error.localizedDescription)
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
        state = .landing
    }

    func closeComplaint(id: String, occupantId: String) {
        Task {
            do {
                try await complaintRepo.closeComplaint(id: id)
                let complaints = try await complaintRepo.fetchByOccupant(occupantId: occupantId)
                state = .viewComplaints(complaints)
            } catch {
                state = .error(error.localizedDescription)
            }
        }
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
