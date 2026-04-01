import SwiftUI

struct HomeView: View {
    let onSignOut: () -> Void

    @StateObject private var viewModel = HomeViewModel()
    @State private var selectedTab = 0
    @State private var isDrawerOpen = false
    @State private var ongoingComplaints: [Complaint] = []
    @State private var showVisitorPass = false
    @State private var showFeedback = false
    @State private var pendingComplaintAction: ComplaintStartAction? = nil
    @State private var pendingNotice: Notice? = nil
    @State private var showLogoutConfirmation = false

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let backgroundGray = Color(red: 0.949, green: 0.957, blue: 0.973)

    private var ready: (name: String, greeting: String, email: String, role: String, empId: String, flatNumber: String, occupantFrom: Date?, isCoordinator: Bool, occupantDocId: String, flatId: String)? {
        if case .ready(let name, let greeting, let email, let role, let empId, let flatNumber, let occupantFrom, let isCoordinator, let occupantDocId, let flatId) = viewModel.state {
            return (name, greeting, email, role, empId, flatNumber, occupantFrom, isCoordinator, occupantDocId, flatId)
        }
        return nil
    }

    var body: some View {
        ZStack(alignment: .leading) {
            TabView(selection: $selectedTab) {
                // Home Tab
                VStack(spacing: 0) {
                    HomeHeaderView(
                        navyBlue: navyBlue,
                        name: ready?.name ?? "",
                        greeting: ready?.greeting ?? "",
                        flatNumber: ready?.flatNumber ?? "",
                        onMenuTap: { isDrawerOpen = true }
                    )
                    ScrollView(showsIndicators: false) {
                        VStack(spacing: 20) {
                            QuickShortcutsSection(
                                onAddComplaint: {
                                    pendingComplaintAction = ComplaintStartAction(kind: .addComplaint(flatId: ready?.flatId ?? ""))
                                    selectedTab = 1
                                },
                                onNoticeboard: { selectedTab = 2 },
                                onVisitorPass: { showVisitorPass = true },
                                onFeedback: { showFeedback = true }
                            )
                            NewNoticesSection(
                                navyBlue: navyBlue,
                                notice: viewModel.latestNotice,
                                onViewAll: { selectedTab = 2 },
                                onNoticeTap: { notice in
                                    pendingNotice = notice
                                    selectedTab = 2
                                }
                            )
                            OngoingComplaintsSection(
                                complaints: ongoingComplaints,
                                navyBlue: navyBlue,
                                onViewAll: {
                                    pendingComplaintAction = ComplaintStartAction(kind: .viewComplaints(occupantId: ready?.occupantDocId ?? ""))
                                    selectedTab = 1
                                },
                                onComplaintTap: { complaint in
                                    pendingComplaintAction = ComplaintStartAction(kind: .openComplaint(complaint: complaint, occupantId: ready?.occupantDocId ?? ""))
                                    selectedTab = 1
                                }
                            )
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 16)
                        .padding(.bottom, 24)
                    }
                    .background(backgroundGray)
                }
                .background(backgroundGray)
                .task(id: ready?.occupantDocId) {
                    guard let docId = ready?.occupantDocId, !docId.isEmpty else { return }
                    do {
                        let repo = BackendComplaintRepository()
                        let all = try await repo.fetchByOccupant(occupantId: docId)
                        ongoingComplaints = Array(all.filter { $0.status != "CLOSED" }.prefix(4))
                    } catch {}
                }
                .tabItem { Label("Home", systemImage: "house") }
                .tag(0)

                // Complaints Tab
                ComplaintsView(
                    occupantName: ready?.name ?? "",
                    occupantEmail: ready?.email ?? "",
                    occupantDocId: ready?.occupantDocId ?? "",
                    flatNumber: ready?.flatNumber ?? "",
                    flatId: ready?.flatId ?? "",
                    onMenuTap: { isDrawerOpen = true },
                    startAction: pendingComplaintAction
                )
                .tabItem { Label("Complaints", systemImage: "doc.text") }
                .tag(1)

                // Noticeboard Tab
                NoticeboardView(
                    onMenuTap: { isDrawerOpen = true },
                    initialNotice: pendingNotice
                )
                .tabItem { Label("Noticeboard", systemImage: "bell") }
                .tag(2)

                // Profile Tab
                ProfileContentView(
                    navyBlue: navyBlue,
                    backgroundGray: backgroundGray,
                    name: ready?.name ?? "",
                    email: ready?.email ?? "",
                    role: ready?.role ?? "",
                    empId: ready?.empId ?? "",
                    flatNumber: ready?.flatNumber ?? "",
                    occupantFrom: ready?.occupantFrom,
                    isCoordinator: ready?.isCoordinator ?? false,
                    onSignOut: { showLogoutConfirmation = true }
                )
                .tabItem { Label("Profile", systemImage: "person") }
                .tag(3)
            }
            .tint(navyBlue)

            // Scrim
            Color.black.opacity(isDrawerOpen ? 0.35 : 0)
                .ignoresSafeArea()
                .allowsHitTesting(isDrawerOpen)
                .onTapGesture { withAnimation(.easeInOut(duration: 0.28)) { isDrawerOpen = false } }

            // Side Drawer
            DrawerContentView(
                navyBlue: navyBlue,
                name: ready?.name ?? "",
                email: ready?.email ?? "",
                role: ready?.role ?? "",
                flatNumber: ready?.flatNumber ?? "",
                onClose: { withAnimation(.easeInOut(duration: 0.28)) { isDrawerOpen = false } },
                onNavigateToComplaints: {
                    withAnimation(.easeInOut(duration: 0.28)) { isDrawerOpen = false }
                    selectedTab = 1
                },
                onNavigateToVisitorPass: {
                    withAnimation(.easeInOut(duration: 0.28)) { isDrawerOpen = false }
                    showVisitorPass = true
                },
                onNavigateToFeedback: {
                    withAnimation(.easeInOut(duration: 0.28)) { isDrawerOpen = false }
                    showFeedback = true
                },
                onNavigateToNoticeboard: {
                    withAnimation(.easeInOut(duration: 0.28)) { isDrawerOpen = false }
                    selectedTab = 2
                },
                onSignOut: {
                    withAnimation(.easeInOut(duration: 0.28)) { isDrawerOpen = false }
                    showLogoutConfirmation = true
                }
            )
            .frame(width: 300)
            .offset(x: isDrawerOpen ? 0 : -300)
            .animation(.easeInOut(duration: 0.28), value: isDrawerOpen)
        }
        .onChange(of: viewModel.shouldSignOut) { denied in
            if denied { onSignOut() }
        }
        .alert("Log Out", isPresented: $showLogoutConfirmation) {
            Button("Log Out", role: .destructive) {
                viewModel.signOut()
                onSignOut()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to log out?")
        }
        .fullScreenCover(isPresented: $showVisitorPass) {
            VisitorPassView(
                occupantId: ready?.occupantDocId ?? "",
                occupantName: ready?.name ?? "",
                flatId: ready?.flatId ?? "",
                flatNumber: ready?.flatNumber ?? "",
                onBack: { showVisitorPass = false }
            )
        }
        .fullScreenCover(isPresented: $showFeedback) {
            FeedbackView(
                occupantId: ready?.occupantDocId ?? "",
                occupantName: ready?.name ?? "",
                onBack: { showFeedback = false }
            )
        }
    }
}

// MARK: - Header

private struct HomeHeaderView: View {
    let navyBlue: Color
    let name: String
    let greeting: String
    let flatNumber: String
    let onMenuTap: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            Button(action: onMenuTap) {
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: 20))
                    .foregroundColor(.white)
            }

            VStack(alignment: .leading, spacing: 2) {
                if !greeting.isEmpty {
                    Text(greeting)
                        .font(.system(size: 13))
                        .foregroundColor(.white.opacity(0.8))
                }
                Text(name.isEmpty ? "Loading..." : name)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white)
                HStack(spacing: 4) {
                    Image(systemName: "house")
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.7))
                    Text(flatNumber.isEmpty ? "—" : flatNumber)
                        .font(.system(size: 13))
                        .foregroundColor(.white.opacity(0.7))
                }
            }

            Spacer()

        }
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
        .background(navyBlue.ignoresSafeArea(edges: .top))
    }
}

// MARK: - Drawer

private struct DrawerContentView: View {
    let navyBlue: Color
    let name: String
    let email: String
    let role: String
    let flatNumber: String
    let onClose: () -> Void
    let onNavigateToComplaints: () -> Void
    let onNavigateToVisitorPass: () -> Void
    let onNavigateToFeedback: () -> Void
    let onNavigateToNoticeboard: () -> Void
    let onSignOut: () -> Void

    private var safeAreaTop: CGFloat {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?.windows
            .first(where: { $0.isKeyWindow })?
            .safeAreaInsets.top ?? 0
    }

    var body: some View {
        VStack(spacing: 0) {
            // Navy header — top padding accounts for status bar / notch
            VStack(alignment: .leading, spacing: 0) {
                Text(name.isEmpty ? "—" : name)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                Spacer().frame(height: 4)
                HStack(spacing: 4) {
                    Image(systemName: "house")
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.7))
                    Text(flatNumber.isEmpty ? "—" : flatNumber)
                        .font(.system(size: 13))
                        .foregroundColor(.white.opacity(0.8))
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, safeAreaTop + 20)
            .padding(.bottom, 20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(navyBlue.ignoresSafeArea(edges: .top))

            // Menu items
            VStack(spacing: 0) {
                DrawerMenuItemView(systemIcon: "doc.text", label: "Complaint", action: onNavigateToComplaints)
                DrawerMenuItemView(systemIcon: "person.badge.plus", label: "Visitor Pass", action: onNavigateToVisitorPass)
                DrawerMenuItemView(systemIcon: "bell", label: "Notice Board", action: onNavigateToNoticeboard)
                DrawerMenuItemView(systemIcon: "bubble.left", label: "Feedback", action: onNavigateToFeedback)
            }
            .frame(maxHeight: .infinity, alignment: .top)
            .background(Color.white)

            // Logout
            Divider()
            Button(action: onSignOut) {
                HStack(spacing: 14) {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color(red: 1.0, green: 0.933, blue: 0.933))
                        .frame(width: 40, height: 40)
                        .overlay {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                                .font(.system(size: 18))
                                .foregroundColor(Color(red: 0.898, green: 0.224, blue: 0.208))
                        }
                    Text("Logout")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(Color(red: 0.898, green: 0.224, blue: 0.208))
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 18)
            }
            .background(Color.white)
        }
        .background(Color.white)
        .ignoresSafeArea(edges: .vertical)  // frame extends full height; content uses safeAreaTop padding
        .gesture(
            DragGesture()
                .onEnded { value in
                    if value.translation.width < -50 {
                        onClose()
                    }
                }
        )
    }
}

private struct DrawerMenuItemView: View {
    let systemIcon: String
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 10)
                    .fill(Color(white: 0.96))
                    .frame(width: 40, height: 40)
                    .overlay {
                        Image(systemName: systemIcon)
                            .font(.system(size: 18))
                            .foregroundColor(Color(white: 0.33))
                    }
                Text(label)
                    .font(.system(size: 15))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14))
                    .foregroundColor(Color(white: 0.73))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .background(Color.white)
    }
}

// MARK: - Profile

private struct ProfileContentView: View {
    let navyBlue: Color
    let backgroundGray: Color
    let name: String
    let email: String
    let role: String
    let empId: String
    let flatNumber: String
    let occupantFrom: Date?
    let isCoordinator: Bool
    let onSignOut: () -> Void

    private var occupantFromFormatted: String {
        guard let date = occupantFrom else { return "—" }
        let fmt = DateFormatter()
        fmt.dateFormat = "MMM dd, yyyy"
        return fmt.string(from: date)
    }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                Spacer().frame(height: 32)

                // Avatar
                Circle()
                    .fill(navyBlue)
                    .frame(width: 96, height: 96)
                    .overlay {
                        Image(systemName: "person")
                            .font(.system(size: 44))
                            .foregroundColor(.white)
                    }
                Spacer().frame(height: 16)
                Text(name.isEmpty ? "—" : name)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                Spacer().frame(height: 4)
                Text(role.isEmpty ? "—" : role)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(navyBlue)

                Spacer().frame(height: 28)

                // Details card
                VStack(spacing: 0) {
                    ProfileDetailRowView(systemIcon: "person", label: "Full Name", value: name.isEmpty ? "—" : name)
                    Divider().padding(.horizontal, 16)
                    ProfileDetailRowView(systemIcon: "number", label: "Employee ID", value: empId.isEmpty ? "—" : empId)
                    Divider().padding(.horizontal, 16)
                    ProfileDetailRowView(systemIcon: "envelope", label: "Email", value: email.isEmpty ? "—" : email)
                    Divider().padding(.horizontal, 16)
                    ProfileDetailRowView(systemIcon: "house", label: "Flat Number", value: flatNumber.isEmpty ? "—" : flatNumber)
                    Divider().padding(.horizontal, 16)
                    ProfileDetailRowView(systemIcon: "person.badge.key", label: "Role", value: role.isEmpty ? "—" : role)
                    Divider().padding(.horizontal, 16)
                    ProfileDetailRowView(systemIcon: "calendar", label: "Occupant Since", value: occupantFromFormatted)
                    Divider().padding(.horizontal, 16)
                    ProfileDetailRowView(systemIcon: "star", label: "Coordinator", value: isCoordinator ? "Yes" : "No")
                }
                .background(Color.white)
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
                .padding(.horizontal, 16)

                Spacer().frame(height: 24)

                // Sign out button
                Button(action: onSignOut) {
                    HStack(spacing: 8) {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                            .font(.system(size: 18))
                        Text("Sign Out")
                            .font(.system(size: 16, weight: .semibold))
                    }
                    .foregroundColor(Color(red: 0.898, green: 0.224, blue: 0.208))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color(red: 1.0, green: 0.933, blue: 0.933))
                    .cornerRadius(12)
                    .padding(.horizontal, 16)
                }

                Spacer().frame(height: 24)
            }
        }
        .background(backgroundGray)
    }
}

private struct ProfileDetailRowView: View {
    let systemIcon: String
    let label: String
    let value: String

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)

    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(red: 0.941, green: 0.953, blue: 1.0))
                .frame(width: 36, height: 36)
                .overlay {
                    Image(systemName: systemIcon)
                        .font(.system(size: 16))
                        .foregroundColor(navyBlue)
                }
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.system(size: 11))
                    .foregroundColor(Color(white: 0.6))
                Text(value)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }
}

// MARK: - Quick Shortcuts

private struct QuickShortcutsSection: View {
    let onAddComplaint: () -> Void
    var onNoticeboard: () -> Void = {}
    var onVisitorPass: () -> Void = {}
    var onFeedback: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 6) {
                Image(systemName: "bolt")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Color(red: 0.118, green: 0.176, blue: 0.42))
                Text("Quick Shortcuts")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
            }

            VStack(spacing: 12) {
                HStack(spacing: 12) {
                    ShortcutItemView(
                        label: "Add Complaint",
                        systemIcon: "plus",
                        iconBg: Color(red: 0.933, green: 0.949, blue: 1.0),
                        iconTint: Color(red: 0.231, green: 0.31, blue: 0.847),
                        action: onAddComplaint
                    )
                    ShortcutItemView(
                        label: "Notice Board",
                        systemIcon: "bell",
                        iconBg: Color(red: 0.867, green: 0.89, blue: 1.0),
                        iconTint: Color(red: 0.118, green: 0.176, blue: 0.42),
                        action: onNoticeboard
                    )
                }
                HStack(spacing: 12) {
                    ShortcutItemView(
                        label: "Visitor Pass",
                        systemIcon: "person.badge.plus",
                        iconBg: Color(red: 0.925, green: 0.992, blue: 0.961),
                        iconTint: Color(red: 0.024, green: 0.588, blue: 0.416),
                        action: onVisitorPass
                    )
                    ShortcutItemView(
                        label: "Feedback",
                        systemIcon: "bubble.left",
                        iconBg: Color(red: 1.0, green: 0.969, blue: 0.929),
                        iconTint: Color(red: 0.851, green: 0.467, blue: 0.024),
                        action: onFeedback
                    )
                }
            }
            .padding(12)
            .background(Color.white)
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
        }
    }
}

private struct ShortcutItemView: View {
    let label: String
    let systemIcon: String
    let iconBg: Color
    let iconTint: Color
    var action: () -> Void = {}

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                RoundedRectangle(cornerRadius: 14)
                    .fill(iconBg)
                    .frame(width: 60, height: 60)
                    .overlay {
                        Image(systemName: systemIcon)
                            .font(.system(size: 24))
                            .foregroundColor(iconTint)
                    }
                Text(label)
                    .font(.system(size: 13))
                    .foregroundColor(Color(white: 0.2))
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)
        }
    }
}

// MARK: - New Notices

private struct NewNoticesSection: View {
    let navyBlue: Color
    let notice: Notice?
    let onViewAll: () -> Void
    var onNoticeTap: (Notice) -> Void = { _ in }

    private func formatted(_ date: Date) -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "MMM d, yyyy"
        return fmt.string(from: date)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "bell")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(navyBlue)
                    Text("New Notices")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                }
                Spacer()
                Button(action: onViewAll) {
                    Text("View All >")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(navyBlue)
                }
            }

            if let notice {
                Button(action: { onNoticeTap(notice) }) {
                    HStack(spacing: 0) {
                        Rectangle()
                            .fill(navyBlue)
                            .frame(width: 4)
                            .cornerRadius(2)

                        HStack(spacing: 12) {
                            Circle()
                                .fill(Color(red: 0.867, green: 0.89, blue: 1.0))
                                .frame(width: 42, height: 42)
                                .overlay {
                                    Image(systemName: "bell")
                                        .font(.system(size: 18))
                                        .foregroundColor(navyBlue)
                                }

                            VStack(alignment: .leading, spacing: 4) {
                                Text(notice.title)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                                    .lineLimit(2)
                                Text(notice.description)
                                    .font(.system(size: 12))
                                    .foregroundColor(Color(white: 0.33))
                                    .lineLimit(3)
                                Text(formatted(notice.publishedAt))
                                    .font(.system(size: 11))
                                    .foregroundColor(Color(white: 0.6))
                            }
                        }
                        .padding(12)
                    }
                    .background(Color(red: 0.94, green: 0.957, blue: 1.0))
                    .cornerRadius(12)
                }
                .buttonStyle(.plain)
            } else {
                Text("No new notices")
                    .font(.system(size: 14))
                    .foregroundColor(Color(white: 0.6))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
                    .background(Color.white)
                    .cornerRadius(12)
                    .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
            }
        }
    }
}

// MARK: - Ongoing Complaints

private func daysOpen(from date: Date) -> String {
    let days = max(1, Int(Date().timeIntervalSince(date) / 86400))
    return days == 1 ? "Active since 1 day" : "Active since \(days) days"
}

private struct OngoingComplaintsSection: View {
    let complaints: [Complaint]
    let navyBlue: Color
    let onViewAll: () -> Void
    var onComplaintTap: (Complaint) -> Void = { _ in }

    private var rows: [[Complaint]] {
        stride(from: 0, to: complaints.count, by: 2).map {
            Array(complaints[$0 ..< min($0 + 2, complaints.count)])
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "info.circle")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(navyBlue)
                    Text("Ongoing Complaints")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                }
                Spacer()
                Button(action: onViewAll) {
                    Text("View All >")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(navyBlue)
                }
            }

            if complaints.isEmpty {
                Text("No ongoing complaints")
                    .font(.system(size: 14))
                    .foregroundColor(Color(white: 0.6))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                    .background(Color.white)
                    .cornerRadius(12)
                    .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
            } else {
                VStack(spacing: 12) {
                    ForEach(rows, id: \.first?.id) { row in
                        HStack(spacing: 12) {
                            ForEach(row) { complaint in
                                Button(action: { onComplaintTap(complaint) }) {
                                    ComplaintCardView(
                                        timeOpen: daysOpen(from: complaint.date),
                                        title: complaint.problem,
                                        category: complaint.category,
                                        status: complaint.status
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                            if row.count == 1 {
                                Spacer().frame(maxWidth: .infinity)
                            }
                        }
                    }
                }
            }
        }
    }
}

private struct ComplaintCardView: View {
    let timeOpen: String
    let title: String
    let category: String
    let status: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 4) {
                Image(systemName: "clock")
                    .font(.system(size: 11))
                    .foregroundColor(Color(red: 0.851, green: 0.467, blue: 0.024))
                Text(timeOpen)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(Color(red: 0.851, green: 0.467, blue: 0.024))
            }
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                .lineLimit(2)
            Text(category)
                .font(.system(size: 12))
                .foregroundColor(Color(white: 0.53))
            StatusBadgeView(status: status)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
    }
}

// MARK: - Shared Components

private struct StatusBadgeView: View {
    let status: String

    private var colors: (bg: Color, fg: Color) {
        switch status.uppercased() {
        case "OPEN":      return (Color(red: 1.0, green: 0.969, blue: 0.929), Color(red: 0.851, green: 0.467, blue: 0.024))
        case "ASSIGNED":  return (Color(red: 0.89, green: 0.95, blue: 1.0),   Color(red: 0.082, green: 0.396, blue: 0.753))
        case "COMPLETED": return (Color(red: 0.91, green: 0.97, blue: 0.91),  Color(red: 0.18, green: 0.49, blue: 0.20))
        case "CLOSED":    return (Color(red: 1.0, green: 0.92, blue: 0.92),   Color(red: 0.718, green: 0.11, blue: 0.11))
        default:          return (Color(white: 0.96), Color(white: 0.38))
        }
    }

    private var label: String {
        switch status.uppercased() {
        case "OPEN":      return "Open"
        case "ASSIGNED":  return "Assigned"
        case "COMPLETED": return "Completed"
        case "CLOSED":    return "Closed"
        default:          return status
        }
    }

    var body: some View {
        Text(label)
            .font(.system(size: 11, weight: .medium))
            .foregroundColor(colors.fg)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(colors.bg)
            .cornerRadius(20)
    }
}
