import SwiftUI
import PhotosUI
import AVKit
import AVFoundation
import Lottie

// MARK: - Navigation Route

enum ComplaintsRoute: Hashable {
    case selectCategory([ComplaintTemplate])
    case submitForm([ComplaintTemplate], ComplaintTemplate)
    case viewComplaints([Complaint])
    case complaintDetail(Complaint, [Complaint])
}

// MARK: - ComplaintsView

struct ComplaintsView: View {
    let occupantName: String
    let occupantEmail: String
    let occupantDocId: String
    let flatNumber: String
    let flatId: String
    let onMenuTap: () -> Void

    @StateObject private var viewModel = ComplaintsViewModel()
    @State private var path: [ComplaintsRoute] = []

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let bgGray = Color(red: 0.949, green: 0.957, blue: 0.973)

    private var isLoading: Bool {
        switch viewModel.state {
        case .loadingTemplates, .loadingComplaints: return true
        default: return false
        }
    }

    private var isSubmitting: Bool {
        if case .submitting = viewModel.state { return true }
        return false
    }

    var body: some View {
        NavigationStack(path: $path) {
            ComplaintsLandingView(
                navyBlue: navyBlue,
                bgGray: bgGray,
                onMenuTap: onMenuTap,
                onAddComplaint: { viewModel.onAddComplaintTapped() },
                onViewComplaints: { viewModel.onViewComplaintsTapped(occupantId: occupantDocId) }
            )
            .overlay {
                if isLoading {
                    Color.black.opacity(0.25).ignoresSafeArea()
                        .overlay { ProgressView().tint(.white) }
                }
                if case .success = viewModel.state {
                    ComplaintSuccessDialog(onDismiss: { viewModel.dismissSuccess() })
                }
                if case .error(let message) = viewModel.state {
                    ComplaintErrorDialog(message: message, onDismiss: { viewModel.dismissError() })
                }
            }
            .navigationBarHidden(true)
            .navigationDestination(for: ComplaintsRoute.self) { route in
                switch route {
                case .selectCategory(let templates):
                    CategorySelectionView(
                        navyBlue: navyBlue,
                        bgGray: bgGray,
                        templates: templates,
                        onBack: { viewModel.onBackFromCategory() },
                        onSelect: { viewModel.onCategorySelected($0) }
                    )
                    .navigationBarHidden(true)

                case .submitForm(_, let template):
                    SubmitComplaintFormView(
                        navyBlue: navyBlue,
                        bgGray: bgGray,
                        template: template,
                        isSubmitting: isSubmitting,
                        onBack: { viewModel.onBackFromForm() },
                        onSubmit: { problem, description, priority, images, videoURLs in
                            viewModel.submitComplaint(
                                problem: problem,
                                description: description,
                                priority: priority,
                                occupantName: occupantName,
                                occupantEmail: occupantEmail,
                                occupantDocId: occupantDocId,
                                flatNumber: flatNumber,
                                flatId: flatId,
                                images: images,
                                videoURLs: videoURLs
                            )
                        }
                    )
                    .navigationBarHidden(true)

                case .viewComplaints(let complaints):
                    ViewComplaintsView(
                        navyBlue: navyBlue,
                        bgGray: bgGray,
                        complaints: complaints,
                        onBack: { viewModel.onBackFromComplaints() },
                        onComplaintTap: { viewModel.onComplaintSelected($0) }
                    )
                    .navigationBarHidden(true)

                case .complaintDetail(let complaint, _):
                    ComplaintDetailView(
                        navyBlue: navyBlue,
                        complaint: complaint,
                        occupantId: occupantDocId,
                        onBack: { viewModel.onBackFromDetail() },
                        onClose: { id, oId in viewModel.closeComplaint(id: id, occupantId: oId) }
                    )
                    .navigationBarHidden(true)
                }
            }
        }
        .onChange(of: viewModel.state) { newState in
            syncPath(with: newState)
        }
    }

    private func syncPath(with state: ComplaintsViewModel.State) {
        switch state {
        case .landing, .success, .error:
            path = []
        case .loadingTemplates, .loadingComplaints, .submitting:
            break // keep current path; loading shown as overlay on root
        case .selectCategory(let templates):
            path = [.selectCategory(templates)]
        case .submitForm(let templates, let template):
            path = [.selectCategory(templates), .submitForm(templates, template)]
        case .viewComplaints(let complaints):
            path = [.viewComplaints(complaints)]
        case .complaintDetail(let complaint, let complaints):
            path = [.viewComplaints(complaints), .complaintDetail(complaint, complaints)]
        }
    }
}

// MARK: - Category icon helper

private func categoryVisuals(for category: String) -> (systemIcon: String, bgColor: Color, iconColor: Color, subtitle: String) {
    let lower = category.lowercased()
    if lower.contains("ac") {
        return ("snowflake", Color(red: 0.89, green: 0.94, blue: 1.0), Color(red: 0.13, green: 0.59, blue: 0.95), "Cooling issue")
    } else if lower.contains("electrical") {
        return ("bolt.fill", Color(red: 1.0, green: 0.95, blue: 0.88), Color(red: 1.0, green: 0.6, blue: 0.0), "Power problem")
    } else if lower.contains("kitchen") {
        return ("fork.knife", Color(red: 0.91, green: 0.96, blue: 0.91), Color(red: 0.3, green: 0.69, blue: 0.31), "Kitchen repair")
    } else if lower.contains("carpenter") || lower.contains("furniture") {
        return ("hammer", Color(red: 0.98, green: 0.91, blue: 0.91), Color(red: 0.47, green: 0.33, blue: 0.28), "Furniture repair")
    } else if lower.contains("fire") {
        return ("flame.fill", Color(red: 1.0, green: 0.92, blue: 0.92), Color(red: 0.96, green: 0.26, blue: 0.21), "Fire safety")
    } else if lower.contains("geyser") {
        return ("thermometer.medium", Color(red: 0.88, green: 0.97, blue: 0.98), Color(red: 0.0, green: 0.67, blue: 0.76), "Hot water issue")
    } else if lower.contains("mason") {
        return ("building.2", Color(red: 0.93, green: 0.93, blue: 0.94), Color(red: 0.38, green: 0.49, blue: 0.55), "Wall & floor")
    } else if lower.contains("paint") {
        return ("paintbrush.fill", Color(red: 0.95, green: 0.9, blue: 0.98), Color(red: 0.61, green: 0.15, blue: 0.69), "Painting work")
    } else if lower.contains("plumbing") {
        return ("wrench.fill", Color(red: 0.89, green: 0.95, blue: 1.0), Color(red: 0.08, green: 0.4, blue: 0.75), "Water & drainage")
    } else if lower.contains("welder") {
        return ("wrench.and.screwdriver.fill", Color(red: 1.0, green: 0.97, blue: 0.88), Color(red: 1.0, green: 0.56, blue: 0.0), "Metal work")
    } else if lower.contains("pest") || lower.contains("hygiene") {
        return ("leaf.fill", Color(red: 0.91, green: 0.97, blue: 0.91), Color(red: 0.18, green: 0.49, blue: 0.2), "Cleaning & pests")
    } else {
        return ("wrench", Color(red: 0.96, green: 0.96, blue: 0.96), Color(red: 0.62, green: 0.62, blue: 0.62), "Maintenance")
    }
}

// MARK: - Landing

private struct ComplaintsLandingView: View {
    let navyBlue: Color
    let bgGray: Color
    let onMenuTap: () -> Void
    let onAddComplaint: () -> Void
    let onViewComplaints: () -> Void

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                HStack {
                    Button(action: onMenuTap) {
                        Image(systemName: "line.3.horizontal")
                            .font(.system(size: 20))
                            .foregroundColor(.white)
                    }
                    Spacer()
                    Text("Complaints")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                    Spacer()
                    Image(systemName: "person")
                        .font(.system(size: 20))
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 16)
                .background(navyBlue.ignoresSafeArea(edges: .top))

                VStack(spacing: 14) {
                    Spacer().frame(height: 4)
                    ComplaintActionCard(
                        systemIcon: "doc.text",
                        iconBg: Color(red: 0.933, green: 0.949, blue: 1.0),
                        iconColor: navyBlue,
                        title: "View Complaints",
                        subtitle: "Track and manage your submitted complaints",
                        action: onViewComplaints
                    )
                    ComplaintActionCard(
                        systemIcon: "plus.circle.fill",
                        iconBg: navyBlue,
                        iconColor: .white,
                        title: "Add Complaint",
                        subtitle: "Submit a new maintenance or service request",
                        action: onAddComplaint
                    )
                    Spacer()
                }
                .padding(.horizontal, 16)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(bgGray)
            }

            Button(action: onAddComplaint) {
                Circle()
                    .fill(navyBlue)
                    .frame(width: 56, height: 56)
                    .overlay {
                        Image(systemName: "plus")
                            .font(.system(size: 24, weight: .light))
                            .foregroundColor(.white)
                    }
            }
            .padding(.trailing, 16)
            .padding(.bottom, 24)
        }
    }
}

private struct ComplaintActionCard: View {
    let systemIcon: String
    let iconBg: Color
    let iconColor: Color
    let title: String
    let subtitle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                RoundedRectangle(cornerRadius: 14)
                    .fill(iconBg)
                    .frame(width: 56, height: 56)
                    .overlay {
                        Image(systemName: systemIcon)
                            .font(.system(size: 24))
                            .foregroundColor(iconColor)
                    }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                    Text(subtitle)
                        .font(.system(size: 13))
                        .foregroundColor(Color(white: 0.53))
                }
                Spacer()
            }
            .padding(16)
            .background(Color.white)
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
        }
    }
}

// MARK: - Category Selection

private struct CategorySelectionView: View {
    let navyBlue: Color
    let bgGray: Color
    let templates: [ComplaintTemplate]
    let onBack: () -> Void
    let onSelect: (ComplaintTemplate) -> Void

    private let columns = [GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 12) {
                    Button(action: onBack) {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 18, weight: .medium))
                            .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                    }
                    Text("Select Issue Category")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                }
                Text("    Choose the category that best describes your complaint")
                    .font(.system(size: 13))
                    .foregroundColor(Color(white: 0.53))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)

            ScrollView(showsIndicators: false) {
                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(templates, id: \.category) { template in
                        let v = categoryVisuals(for: template.category)
                        CategoryCardView(
                            systemIcon: v.systemIcon,
                            iconBg: v.bgColor,
                            iconColor: v.iconColor,
                            category: template.category,
                            subtitle: v.subtitle,
                            onTap: { onSelect(template) }
                        )
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 16)
            }
            .background(bgGray)
        }
    }
}

private struct CategoryCardView: View {
    let systemIcon: String
    let iconBg: Color
    let iconColor: Color
    let category: String
    let subtitle: String
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .center, spacing: 10) {
                RoundedRectangle(cornerRadius: 16)
                    .fill(iconBg)
                    .frame(width: 60, height: 60)
                    .overlay {
                        Image(systemName: systemIcon)
                            .font(.system(size: 26))
                            .foregroundColor(iconColor)
                    }
                Text(category)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                    .multilineTextAlignment(.center)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundColor(Color(white: 0.53))
                    .multilineTextAlignment(.center)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .center)
            .background(Color.white)
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
        }
    }
}

// MARK: - Submit Form

private struct SubmitComplaintFormView: View {
    let navyBlue: Color
    let bgGray: Color
    let template: ComplaintTemplate
    let isSubmitting: Bool
    let onBack: () -> Void
    let onSubmit: (String, String, String, [UIImage], [URL]) -> Void

    @State private var selectedProblem = ""
    @State private var description = ""
    @State private var selectedPriority = ""

    // Media
    @State private var pickedImages: [UIImage] = []
    @State private var cameraVideoURLs: [URL] = []
    @State private var photoPickerItems: [PhotosPickerItem] = []
    @State private var videoPickerItems: [PhotosPickerItem] = []
    @State private var showPhotoSourceSheet = false
    @State private var showVideoSourceSheet = false
    @State private var showCameraPhoto = false
    @State private var showCameraVideo = false
    @State private var showPhotoLibraryPicker = false
    @State private var showVideoLibraryPicker = false
    @State private var videoThumbnails: [URL: UIImage] = [:]
    @State private var previewImage: UIImage? = nil
    @State private var previewVideoURL: URL? = nil

    private var canSubmit: Bool {
        !selectedProblem.isEmpty && !selectedPriority.isEmpty && !isSubmitting
    }

    var body: some View {
        let v = categoryVisuals(for: template.category)
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                }
                Text("Submit Complaint")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .background(Color.white)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 16) {
                    // Selected category
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Selected Issue Type")
                            .font(.system(size: 12))
                            .foregroundColor(Color(white: 0.53))
                        HStack(spacing: 12) {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(v.bgColor)
                                .frame(width: 44, height: 44)
                                .overlay {
                                    Image(systemName: v.systemIcon)
                                        .font(.system(size: 20))
                                        .foregroundColor(v.iconColor)
                                }
                            Text(template.category)
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white)
                    .cornerRadius(12)
                    .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)

                    // Problem selection
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Select Problem")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        FlexWrapView(items: template.problems, spacing: 8) { problem in
                            let selected = selectedProblem == problem
                            Text(problem)
                                .font(.system(size: 13))
                                .foregroundColor(selected ? .white : Color(white: 0.27))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(selected ? navyBlue : Color.white)
                                .cornerRadius(20)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 20)
                                        .stroke(selected ? navyBlue : Color(white: 0.87), lineWidth: 1)
                                )
                                .onTapGesture {
                                    selectedProblem = selected ? "" : problem
                                }
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white)
                    .cornerRadius(12)
                    .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)

                    // Description
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Description")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        ZStack(alignment: .topLeading) {
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color(white: 0.9), lineWidth: 1)
                            if description.isEmpty {
                                Text("Describe your complaint in detail...")
                                    .font(.system(size: 13))
                                    .foregroundColor(Color(white: 0.67))
                                    .padding(12)
                            }
                            TextEditor(text: $description)
                                .font(.system(size: 13))
                                .frame(height: 100)
                                .padding(8)
                                .scrollContentBackground(.hidden)
                        }
                        .frame(height: 110)
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white)
                    .cornerRadius(12)
                    .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)

                    // Priority
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Priority Level")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        VStack(spacing: 10) {
                            ForEach([["Low", "Medium"], ["High", "Emergency"]], id: \.first) { row in
                                HStack(spacing: 10) {
                                    ForEach(row, id: \.self) { p in
                                        PriorityButton(
                                            label: p,
                                            isSelected: selectedPriority == p
                                        ) {
                                            selectedPriority = selectedPriority == p ? "" : p
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white)
                    .cornerRadius(12)
                    .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)

                    // Media
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Add Media (Optional)")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        HStack(spacing: 12) {
                            Button { showPhotoSourceSheet = true } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: "camera")
                                        .font(.system(size: 16))
                                        .foregroundColor(Color(white: 0.4))
                                    Text("Add Photo")
                                        .font(.system(size: 13))
                                        .foregroundColor(Color(white: 0.27))
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(Color(white: 0.96))
                                .cornerRadius(10)
                            }
                            Button { showVideoSourceSheet = true } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: "video")
                                        .font(.system(size: 16))
                                        .foregroundColor(Color(white: 0.4))
                                    Text("Add Video")
                                        .font(.system(size: 13))
                                        .foregroundColor(Color(white: 0.27))
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(Color(white: 0.96))
                                .cornerRadius(10)
                            }
                        }
                        if !pickedImages.isEmpty || !cameraVideoURLs.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(Array(pickedImages.enumerated()), id: \.offset) { i, img in
                                        ZStack(alignment: .topTrailing) {
                                            Image(uiImage: img)
                                                .resizable()
                                                .scaledToFill()
                                                .frame(width: 72, height: 72)
                                                .clipShape(RoundedRectangle(cornerRadius: 8))
                                                .onTapGesture { previewImage = img }
                                            Button {
                                                if i < pickedImages.count { pickedImages.remove(at: i) }
                                            } label: {
                                                Image(systemName: "xmark.circle.fill")
                                                    .font(.system(size: 18))
                                                    .foregroundColor(.white)
                                                    .shadow(radius: 2)
                                            }
                                            .offset(x: 6, y: -6)
                                        }
                                    }
                                    ForEach(Array(cameraVideoURLs.enumerated()), id: \.offset) { i, url in
                                        ZStack(alignment: .topTrailing) {
                                            ZStack {
                                                if let thumb = videoThumbnails[url] {
                                                    Image(uiImage: thumb)
                                                        .resizable()
                                                        .scaledToFill()
                                                        .frame(width: 72, height: 72)
                                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                                } else {
                                                    RoundedRectangle(cornerRadius: 8)
                                                        .fill(Color(white: 0.85))
                                                        .frame(width: 72, height: 72)
                                                }
                                                // Play button overlay
                                                RoundedRectangle(cornerRadius: 8)
                                                    .fill(Color.black.opacity(0.28))
                                                    .frame(width: 72, height: 72)
                                                Image(systemName: "play.circle.fill")
                                                    .font(.system(size: 28))
                                                    .foregroundColor(.white)
                                                    .shadow(radius: 2)
                                            }
                                            .onTapGesture { previewVideoURL = url }
                                            Button {
                                                if i < cameraVideoURLs.count { cameraVideoURLs.remove(at: i) }
                                            } label: {
                                                Image(systemName: "xmark.circle.fill")
                                                    .font(.system(size: 18))
                                                    .foregroundColor(.white)
                                                    .shadow(radius: 2)
                                            }
                                            .offset(x: 6, y: -6)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white)
                    .cornerRadius(12)
                    .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)

                    Spacer().frame(height: 8)
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, 24)
            }
            .background(bgGray)

            Divider()
            Button(action: {
                if canSubmit {
                    onSubmit(selectedProblem, description, selectedPriority, pickedImages, cameraVideoURLs)
                }
            }) {
                ZStack {
                    Text("Submit Complaint")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(canSubmit ? navyBlue : Color(white: 0.8))
                .cornerRadius(12)
            }
            .disabled(!canSubmit)
            .padding(16)
            .background(Color.white)
        }
        .confirmationDialog("Add Photo", isPresented: $showPhotoSourceSheet) {
            if UIImagePickerController.isSourceTypeAvailable(.camera) {
                Button("Camera") { showCameraPhoto = true }
            }
            Button("Photo Library") { showPhotoLibraryPicker = true }
            Button("Cancel", role: .cancel) {}
        }
        .confirmationDialog("Add Video", isPresented: $showVideoSourceSheet) {
            if UIImagePickerController.isSourceTypeAvailable(.camera) {
                Button("Camera") { showCameraVideo = true }
            }
            Button("Video Library") { showVideoLibraryPicker = true }
            Button("Cancel", role: .cancel) {}
        }
        .photosPicker(isPresented: $showPhotoLibraryPicker, selection: $photoPickerItems, matching: .images)
        .photosPicker(isPresented: $showVideoLibraryPicker, selection: $videoPickerItems, matching: .videos)
        .sheet(isPresented: $showCameraPhoto) {
            CameraPickerView(mediaType: .photo) { image in
                pickedImages.append(image)
            } onVideoCapture: { _ in } onDismiss: { showCameraPhoto = false }
        }
        .sheet(isPresented: $showCameraVideo) {
            CameraPickerView(mediaType: .video) { _ in } onVideoCapture: { url in
                cameraVideoURLs.append(url)
            } onDismiss: { showCameraVideo = false }
        }
        .onChange(of: photoPickerItems) { items in
            Task {
                for item in items {
                    if let data = try? await item.loadTransferable(type: Data.self),
                       let image = UIImage(data: data) {
                        pickedImages.append(image)
                    }
                }
            }
        }
        .onChange(of: videoPickerItems) { items in
            Task {
                for item in items {
                    if let data = try? await item.loadTransferable(type: Data.self) {
                        let url = FileManager.default.temporaryDirectory
                            .appendingPathComponent(UUID().uuidString + ".mp4")
                        try? data.write(to: url)
                        cameraVideoURLs.append(url)
                    }
                }
            }
        }
        .onChange(of: cameraVideoURLs) { urls in
            for url in urls where videoThumbnails[url] == nil {
                Task {
                    if let thumb = await generateVideoThumbnail(for: url) {
                        videoThumbnails[url] = thumb
                    }
                }
            }
        }
        .fullScreenCover(isPresented: Binding(
            get: { previewImage != nil },
            set: { if !$0 { previewImage = nil } }
        )) {
            if let img = previewImage {
                ImagePreviewView(image: img) { previewImage = nil }
            }
        }
        .sheet(isPresented: Binding(
            get: { previewVideoURL != nil },
            set: { if !$0 { previewVideoURL = nil } }
        )) {
            if let url = previewVideoURL {
                VideoPlayerView(url: url)
            }
        }
        .overlay {
            if isSubmitting {
                ZStack {
                    Color.black.opacity(0.4).ignoresSafeArea()
                    LottieView(animation: .named("loader"))
                        .playing(loopMode: .loop)
                        .frame(width: 200, height: 200)
                }
            }
        }
    }

    private func generateVideoThumbnail(for url: URL) async -> UIImage? {
        await withCheckedContinuation { continuation in
            let asset = AVAsset(url: url)
            let generator = AVAssetImageGenerator(asset: asset)
            generator.appliesPreferredTrackTransform = true
            generator.maximumSize = CGSize(width: 200, height: 200)
            generator.generateCGImagesAsynchronously(
                forTimes: [NSValue(time: .zero)]
            ) { _, cgImage, _, _, _ in
                if let cgImage = cgImage {
                    continuation.resume(returning: UIImage(cgImage: cgImage))
                } else {
                    continuation.resume(returning: nil)
                }
            }
        }
    }
}

private struct PriorityButton: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    private var activeColor: Color {
        switch label {
        case "Low":       return Color(red: 0.298, green: 0.686, blue: 0.314)
        case "Medium":    return Color(red: 1.0,   green: 0.757, blue: 0.027)
        case "High":      return Color(red: 1.0,   green: 0.596, blue: 0.0)
        case "Emergency": return Color(red: 0.957, green: 0.263, blue: 0.212)
        default:          return Color(red: 0.118, green: 0.176, blue: 0.42)
        }
    }

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(isSelected ? .white : Color(white: 0.27))
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(isSelected ? activeColor : Color(white: 0.96))
                .cornerRadius(10)
        }
    }
}

// MARK: - Flex Wrap (using Layout protocol, requires iOS 16+)

private struct FlexLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? 0
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if currentX + size.width > maxWidth && currentX > 0 {
                currentY += rowHeight + spacing
                currentX = 0
                rowHeight = 0
            }
            currentX += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        currentY += rowHeight

        return CGSize(width: maxWidth, height: currentY)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var currentX = bounds.minX
        var currentY = bounds.minY
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if currentX + size.width > bounds.maxX && currentX > bounds.minX {
                currentY += rowHeight + spacing
                currentX = bounds.minX
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: currentX, y: currentY), proposal: ProposedViewSize(size))
            currentX += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

private struct FlexWrapView<Item: Hashable, Content: View>: View {
    let items: [Item]
    var spacing: CGFloat = 8
    let content: (Item) -> Content

    init(items: [Item], spacing: CGFloat = 8, @ViewBuilder content: @escaping (Item) -> Content) {
        self.items = items
        self.spacing = spacing
        self.content = content
    }

    var body: some View {
        FlexLayout(spacing: spacing) {
            ForEach(items, id: \.self) { item in
                content(item)
            }
        }
    }
}

// MARK: - Dialogs

private struct ComplaintSuccessDialog: View {
    let onDismiss: () -> Void

    var body: some View {
        Color.black.opacity(0.4)
            .ignoresSafeArea()
            .overlay {
                VStack(spacing: 20) {
                    Circle()
                        .fill(Color(red: 0.91, green: 0.96, blue: 0.91))
                        .frame(width: 72, height: 72)
                        .overlay {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 40))
                                .foregroundColor(Color(red: 0.3, green: 0.69, blue: 0.31))
                        }
                    Text("Complaint Submitted!")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                    Text("Your complaint has been registered successfully. You'll be notified once it's assigned to staff.")
                        .font(.system(size: 14))
                        .foregroundColor(Color(white: 0.4))
                        .multilineTextAlignment(.center)
                    Button(action: onDismiss) {
                        Text("Done")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(Color(red: 0.118, green: 0.176, blue: 0.42))
                            .cornerRadius(12)
                    }
                }
                .padding(28)
                .background(Color.white)
                .cornerRadius(20)
                .padding(.horizontal, 32)
            }
    }
}

private struct ComplaintErrorDialog: View {
    let message: String
    let onDismiss: () -> Void

    var body: some View {
        Color.black.opacity(0.4)
            .ignoresSafeArea()
            .overlay {
                VStack(spacing: 16) {
                    Text("Submission Failed")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                    Text(message)
                        .font(.system(size: 14))
                        .foregroundColor(Color(white: 0.4))
                        .multilineTextAlignment(.center)
                    Button(action: onDismiss) {
                        Text("OK")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(Color(red: 0.898, green: 0.224, blue: 0.208))
                            .cornerRadius(12)
                    }
                }
                .padding(24)
                .background(Color.white)
                .cornerRadius(20)
                .padding(.horizontal, 32)
            }
    }
}

// MARK: - Media Preview

private struct ImagePreviewView: View {
    let image: UIImage
    let onDismiss: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            Image(uiImage: image)
                .resizable()
                .scaledToFit()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            Button(action: onDismiss) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 30))
                    .foregroundColor(.white)
                    .shadow(radius: 4)
                    .padding(16)
            }
        }
    }
}

// MARK: - Video Thumbnail

private struct VideoThumbnailView: View {
    let url: String
    @State private var thumbnail: UIImage? = nil

    var body: some View {
        ZStack {
            if let thumb = thumbnail {
                Image(uiImage: thumb)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 100, height: 100)
                    .clipped()
            } else {
                Color(white: 0.85).frame(width: 100, height: 100)
            }
            Color.black.opacity(0.35)
            Image(systemName: "play.circle.fill")
                .font(.system(size: 34))
                .foregroundColor(.white)
        }
        .frame(width: 100, height: 100)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .task { thumbnail = await loadThumbnail() }
    }

    private func loadThumbnail() async -> UIImage? {
        guard let videoURL = URL(string: url) else { return nil }
        let asset = AVURLAsset(url: videoURL)
        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        generator.maximumSize = CGSize(width: 200, height: 200)
        return await withCheckedContinuation { continuation in
            generator.generateCGImagesAsynchronously(
                forTimes: [NSValue(time: .zero)]
            ) { _, cgImage, _, _, _ in
                if let cg = cgImage {
                    continuation.resume(returning: UIImage(cgImage: cg))
                } else {
                    continuation.resume(returning: nil)
                }
            }
        }
    }
}

private struct VideoPlayerView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let player = AVPlayer(url: url)
        let controller = AVPlayerViewController()
        controller.player = player
        player.play()
        return controller
    }

    func updateUIViewController(_ uiViewController: AVPlayerViewController, context: Context) {}
}

// MARK: - Camera Picker

private struct CameraPickerView: UIViewControllerRepresentable {
    enum MediaType { case photo, video }

    let mediaType: MediaType
    let onPhotoCapture: (UIImage) -> Void
    let onVideoCapture: (URL) -> Void
    let onDismiss: () -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        if mediaType == .video {
            picker.mediaTypes = ["public.movie"]
            picker.videoQuality = .typeMedium
        }
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: CameraPickerView
        init(_ parent: CameraPickerView) { self.parent = parent }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            if parent.mediaType == .photo, let image = info[.originalImage] as? UIImage {
                parent.onPhotoCapture(image)
            } else if parent.mediaType == .video, let url = info[.mediaURL] as? URL {
                parent.onVideoCapture(url)
            }
            parent.onDismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.onDismiss()
        }
    }
}

// MARK: - View Complaints

private func statusColor(for status: String) -> (bg: Color, fg: Color) {
    switch status.uppercased() {
    case "OPEN":        return (Color(red: 1.0, green: 0.95, blue: 0.88), Color(red: 0.9, green: 0.40, blue: 0.0))
    case "IN_PROGRESS": return (Color(red: 0.89, green: 0.95, blue: 1.0), Color(red: 0.08, green: 0.40, blue: 0.75))
    case "COMPLETED":   return (Color(red: 0.91, green: 0.97, blue: 0.91), Color(red: 0.18, green: 0.49, blue: 0.20))
    case "CLOSED":      return (Color(red: 0.96, green: 0.96, blue: 0.96), Color(red: 0.46, green: 0.46, blue: 0.46))
    default:            return (Color(red: 0.96, green: 0.96, blue: 0.96), Color(red: 0.46, green: 0.46, blue: 0.46))
    }
}

private func statusLabel(for status: String) -> String {
    switch status.uppercased() {
    case "OPEN":        return "Open"
    case "IN_PROGRESS": return "In Progress"
    case "COMPLETED":   return "Completed"
    case "CLOSED":      return "Closed"
    default:            return status
    }
}

private func priorityColor(for priority: String) -> (bg: Color, fg: Color) {
    switch priority.lowercased() {
    case "low":       return (Color(red: 0.91, green: 0.97, blue: 0.91), Color(red: 0.18, green: 0.49, blue: 0.20))
    case "medium":    return (Color(red: 1.0, green: 0.98, blue: 0.88), Color(red: 0.96, green: 0.50, blue: 0.09))
    case "high":      return (Color(red: 1.0, green: 0.95, blue: 0.88), Color(red: 0.9, green: 0.40, blue: 0.0))
    case "emergency": return (Color(red: 1.0, green: 0.93, blue: 0.93), Color(red: 0.78, green: 0.16, blue: 0.16))
    default:          return (Color(red: 0.96, green: 0.96, blue: 0.96), Color(red: 0.46, green: 0.46, blue: 0.46))
    }
}

private func formatDate(_ date: Date) -> String {
    let f = DateFormatter()
    f.dateFormat = "dd MMM yyyy"
    return f.string(from: date)
}

private struct StatusChip: View {
    let status: String
    var body: some View {
        let c = statusColor(for: status)
        Text(statusLabel(for: status))
            .font(.system(size: 11, weight: .medium))
            .foregroundColor(c.fg)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(c.bg, in: RoundedRectangle(cornerRadius: 6))
    }
}

private struct PriorityBadge: View {
    let priority: String
    var body: some View {
        let c = priorityColor(for: priority)
        let label = priority.isEmpty ? "—" : priority.prefix(1).uppercased() + priority.dropFirst()
        Text(label)
            .font(.system(size: 11, weight: .medium))
            .foregroundColor(c.fg)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(c.bg, in: RoundedRectangle(cornerRadius: 6))
    }
}

private struct ViewComplaintsView: View {
    let navyBlue: Color
    let bgGray: Color
    let complaints: [Complaint]
    let onBack: () -> Void
    let onComplaintTap: (Complaint) -> Void

    @State private var selectedTab = 0

    private var ongoing: [Complaint] { complaints.filter { $0.status.uppercased() != "CLOSED" } }
    private var closed: [Complaint]  { complaints.filter { $0.status.uppercased() == "CLOSED" } }

    var body: some View {
        VStack(spacing: 0) {
            // App bar
            HStack(spacing: 12) {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(.white)
                }
                Text("My Complaints")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .background(navyBlue.ignoresSafeArea(edges: .top))

            // Tab bar
            HStack(spacing: 0) {
                ForEach(["Ongoing", "Closed"].indices, id: \.self) { i in
                    let title = ["Ongoing", "Closed"][i]
                    Button(action: { selectedTab = i }) {
                        VStack(spacing: 0) {
                            Text(title)
                                .font(.system(size: 14, weight: selectedTab == i ? .semibold : .regular))
                                .foregroundColor(selectedTab == i ? navyBlue : Color(white: 0.5))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                            Rectangle()
                                .fill(selectedTab == i ? navyBlue : Color.clear)
                                .frame(height: 2)
                        }
                    }
                }
            }
            .background(Color.white)

            let currentList = selectedTab == 0 ? ongoing : closed

            if currentList.isEmpty {
                Spacer()
                Text(selectedTab == 0 ? "No ongoing complaints" : "No closed complaints")
                    .font(.system(size: 15))
                    .foregroundColor(Color(white: 0.53))
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(currentList) { complaint in
                            ComplaintListCard(
                                navyBlue: navyBlue,
                                complaint: complaint,
                                onTap: { onComplaintTap(complaint) }
                            )
                        }
                    }
                    .padding(16)
                }
            }
        }
        .background(bgGray.ignoresSafeArea())
    }
}

private struct ComplaintListCard: View {
    let navyBlue: Color
    let complaint: Complaint
    let onTap: () -> Void

    var body: some View {
        let v = categoryVisuals(for: complaint.category)
        Button(action: onTap) {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 10)
                    .fill(v.bgColor)
                    .frame(width: 48, height: 48)
                    .overlay {
                        Image(systemName: v.systemIcon)
                            .font(.system(size: 22))
                            .foregroundColor(v.iconColor)
                    }
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(complaint.category)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                            .lineLimit(1)
                        Spacer()
                        StatusChip(status: complaint.status)
                    }
                    Text(complaint.problem)
                        .font(.system(size: 13))
                        .foregroundColor(Color(white: 0.33))
                        .lineLimit(1)
                    HStack(spacing: 8) {
                        Text(formatDate(complaint.date))
                            .font(.system(size: 12))
                            .foregroundColor(Color(white: 0.53))
                        PriorityBadge(priority: complaint.priority)
                    }
                }
                Image(systemName: "chevron.right")
                    .font(.system(size: 13))
                    .foregroundColor(Color(white: 0.8))
            }
            .padding(16)
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
        }
    }
}

// MARK: - Complaint Detail

private struct ComplaintDetailView: View {
    let navyBlue: Color
    let complaint: Complaint
    let occupantId: String
    let onBack: () -> Void
    let onClose: (String, String) -> Void

    @State private var showCloseConfirm = false
    @State private var previewImageUrl: String? = nil
    @State private var previewVideoUrl: URL? = nil

    var body: some View {
        let v = categoryVisuals(for: complaint.category)
        VStack(spacing: 0) {
            // App bar
            HStack(spacing: 12) {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                }
                Text("Complaint Details")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .background(Color.white)

            ScrollView {
                VStack(spacing: 14) {
                    // Header card
                    HStack(spacing: 12) {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(v.bgColor)
                            .frame(width: 52, height: 52)
                            .overlay {
                                Image(systemName: v.systemIcon)
                                    .font(.system(size: 24))
                                    .foregroundColor(v.iconColor)
                            }
                        VStack(alignment: .leading, spacing: 4) {
                            Text(complaint.category)
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                            Text(complaint.problem)
                                .font(.system(size: 13))
                                .foregroundColor(Color(white: 0.4))
                        }
                        Spacer()
                        StatusChip(status: complaint.status)
                    }
                    .padding(16)
                    .background(Color.white)
                    .cornerRadius(12)
                    .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)

                    // Details card
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Details")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(Color(white: 0.53))
                        DetailRowView(label: "Submitted", value: formatDate(complaint.date))
                        Divider()
                        HStack {
                            Text("Priority")
                                .font(.system(size: 13))
                                .foregroundColor(Color(white: 0.53))
                                .frame(width: 90, alignment: .leading)
                            PriorityBadge(priority: complaint.priority)
                            Spacer()
                        }
                        if !complaint.description.isEmpty {
                            Divider()
                            DetailRowView(label: "Description", value: complaint.description, multiLine: true)
                        }
                        if let resolveDate = complaint.resolveDate {
                            Divider()
                            DetailRowView(label: "Resolved On", value: formatDate(resolveDate))
                        }
                    }
                    .padding(16)
                    .background(Color.white)
                    .cornerRadius(12)
                    .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)

                    // Worker card — shown when ASSIGNED or COMPLETED
                    let showWorker = !complaint.workerName.isEmpty &&
                        (complaint.status.uppercased() == "ASSIGNED" || complaint.status.uppercased() == "COMPLETED")
                    if showWorker {
                        HStack(spacing: 12) {
                            Circle()
                                .fill(Color(red: 0.933, green: 0.949, blue: 1.0))
                                .frame(width: 42, height: 42)
                                .overlay {
                                    Image(systemName: "person")
                                        .font(.system(size: 18))
                                        .foregroundColor(navyBlue)
                                }
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Assigned Worker")
                                    .font(.system(size: 12))
                                    .foregroundColor(Color(white: 0.53))
                                Text(complaint.workerName)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                            }
                            Spacer()
                        }
                        .padding(16)
                        .background(Color.white)
                        .cornerRadius(12)
                        .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
                    }

                    // Worker remarks + media — shown only when COMPLETED
                    if complaint.status.uppercased() == "COMPLETED" {
                        if !complaint.workerRemarks.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Worker Remarks")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(Color(red: 0.18, green: 0.49, blue: 0.20))
                                Text(complaint.workerRemarks)
                                    .font(.system(size: 14))
                                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                            }
                            .padding(16)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(red: 0.97, green: 1.0, blue: 0.97))
                            .cornerRadius(12)
                            .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
                        }
                        if !complaint.workerMedia.isEmpty {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Work Done (\(complaint.workerMedia.count))")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(Color(white: 0.53))
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 10) {
                                        ForEach(complaint.workerMedia, id: \.self) { url in
                                            let isVideo = url.contains(".mp4")
                                            Group {
                                                if isVideo {
                                                    VideoThumbnailView(url: url)
                                                } else {
                                                    AsyncImage(url: URL(string: url)) { phase in
                                                        switch phase {
                                                        case .success(let img):
                                                            img.resizable()
                                                                .scaledToFill()
                                                                .frame(width: 100, height: 100)
                                                                .clipped()
                                                        default:
                                                            Color(white: 0.85)
                                                                .frame(width: 100, height: 100)
                                                                .overlay { ProgressView() }
                                                        }
                                                    }
                                                    .frame(width: 100, height: 100)
                                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                                }
                                            }
                                            .onTapGesture {
                                                if isVideo { previewVideoUrl = URL(string: url) }
                                                else { previewImageUrl = url }
                                            }
                                        }
                                    }
                                }
                            }
                            .padding(16)
                            .background(Color.white)
                            .cornerRadius(12)
                            .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
                        }
                    }

                    // Occupant media card
                    if !complaint.mediaUrls.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Attached Media (\(complaint.mediaUrls.count))")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(Color(white: 0.53))
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 10) {
                                    ForEach(complaint.mediaUrls, id: \.self) { url in
                                        let isVideo = url.contains(".mp4")
                                        Group {
                                            if isVideo {
                                                VideoThumbnailView(url: url)
                                            } else {
                                                AsyncImage(url: URL(string: url)) { phase in
                                                    switch phase {
                                                    case .success(let img):
                                                        img.resizable()
                                                            .scaledToFill()
                                                            .frame(width: 100, height: 100)
                                                            .clipped()
                                                    default:
                                                        Color(white: 0.85)
                                                            .frame(width: 100, height: 100)
                                                            .overlay { ProgressView() }
                                                    }
                                                }
                                                .frame(width: 100, height: 100)
                                                .clipShape(RoundedRectangle(cornerRadius: 8))
                                            }
                                        }
                                        .onTapGesture {
                                            if isVideo { previewVideoUrl = URL(string: url) }
                                            else { previewImageUrl = url }
                                        }
                                    }
                                }
                            }
                        }
                        .padding(16)
                        .background(Color.white)
                        .cornerRadius(12)
                        .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
                    }
                }
                .padding(16)
            }
            .background(Color(red: 0.949, green: 0.957, blue: 0.973))

            // Mark as Closed — only if COMPLETED
            if complaint.status.uppercased() == "COMPLETED" {
                Divider()
                Button(action: { showCloseConfirm = true }) {
                    Text("Mark as Closed")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color(red: 0.18, green: 0.49, blue: 0.20))
                        .cornerRadius(12)
                }
                .padding(16)
                .background(Color.white)
            }
        }
        .background(Color.white.ignoresSafeArea(edges: .top))
        .alert("Mark as Closed", isPresented: $showCloseConfirm) {
            Button("Confirm") { onClose(complaint.id, occupantId) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure this complaint has been resolved to your satisfaction?")
        }
        .fullScreenCover(isPresented: Binding(
            get: { previewImageUrl != nil },
            set: { if !$0 { previewImageUrl = nil } }
        )) {
            ZStack(alignment: .topTrailing) {
                Color.black.ignoresSafeArea()
                if let url = previewImageUrl {
                    AsyncImage(url: URL(string: url)) { phase in
                        switch phase {
                        case .success(let img):
                            img.resizable()
                                .scaledToFit()
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                        default:
                            ProgressView().tint(.white)
                        }
                    }
                }
                Button(action: { previewImageUrl = nil }) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 30))
                        .foregroundColor(.white)
                        .padding(20)
                }
            }
        }
        .fullScreenCover(isPresented: Binding(
            get: { previewVideoUrl != nil },
            set: { if !$0 { previewVideoUrl = nil } }
        )) {
            ZStack(alignment: .topLeading) {
                if let url = previewVideoUrl {
                    VideoPlayerView(url: url).ignoresSafeArea()
                }
                Button(action: { previewVideoUrl = nil }) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 30))
                        .foregroundColor(.white)
                        .padding(20)
                }
            }
        }
    }
}

private struct DetailRowView: View {
    let label: String
    let value: String
    var multiLine: Bool = false

    var body: some View {
        HStack(alignment: multiLine ? .top : .center, spacing: 0) {
            Text(label)
                .font(.system(size: 13))
                .foregroundColor(Color(white: 0.53))
                .frame(width: 90, alignment: .leading)
            Text(value)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}
