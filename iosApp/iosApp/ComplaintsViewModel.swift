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

    func onAddComplaintTapped(flatId: String) {
        guard !flatId.isEmpty else {
            state = .error("No flat allotted. Please contact admin.")
            return
        }
        state = .loadingTemplates
        templatesListener?.remove()
        templatesListener = templateRepo.observeTemplates { [weak self] templates in
            guard let self else { return }
            Task { @MainActor in
                switch self.state {
                case .loadingTemplates, .selectCategory:
                    self.state = .selectCategory(templates)
                case .submitForm(_, let selected):
                    // Refresh the SELECTED template too, not just the list. Without
                    // this the open form keeps a stale copy, so an admin changing a
                    // problem's default priority while the occupant is filling it in
                    // would never be picked up. Falls back to the current selection
                    // if the category has since been deleted, so the form is never
                    // yanked away.
                    let refreshed = templates.first { $0.category == selected.category } ?? selected
                    self.state = .submitForm(templates: templates, selected: refreshed)
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
        // Re-derive rather than trusting the value the view passed in: the
        // template comes from a live listener, so an admin edit can land between
        // the view rendering and the occupant tapping Submit. An admin-set
        // priority always wins; otherwise the occupant's own pick is used.
        let effectivePriority = template.priority(for: problem) ?? priority
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
                    priority: effectivePriority,
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

    func openComplaintDirectly(complaint: Complaint, occupantId: String) {
        state = .complaintDetail(complaint, [complaint])
        complaintsListener?.remove()
        complaintsListener = complaintRepo.observeByOccupant(occupantId: occupantId) { [weak self] complaints in
            guard let self else { return }
            Task { @MainActor in
                let refreshed = complaints.first { $0.id == complaint.id } ?? complaint
                self.state = .complaintDetail(refreshed, complaints)
            }
        }
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

    /// Occupant marks a complaint as COMPLETED, with an optional feedback
    /// note (≤255 chars). The admin still has to CLOSE the complaint.
    func markComplaintCompleted(id: String, occupantId: String, userRemarks: String = "") {
        Task {
            do {
                try await complaintRepo.markCompletedByUser(id: id, userRemarks: userRemarks)
                // Listener auto-updates the list; navigate back to it.
                if case .complaintDetail(_, let complaints) = state {
                    state = .viewComplaints(complaints)
                }
            } catch {
                state = .error(error.localizedDescription)
            }
        }
    }

    /// Occupant CLOSES a complaint that the worker has already marked
    /// COMPLETED. Optional feedback (≤255 chars) is captured in the same
    /// UserCompletionRemarks field used by `markComplaintCompleted`.
    func closeComplaintByUser(id: String, occupantId: String, userRemarks: String = "") {
        Task {
            do {
                try await complaintRepo.closeComplaintByUser(id: id, userRemarks: userRemarks)
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
