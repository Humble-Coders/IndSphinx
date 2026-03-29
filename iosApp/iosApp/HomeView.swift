import SwiftUI

struct HomeView: View {
    let onSignOut: () -> Void

    @StateObject private var viewModel = HomeViewModel()
    @State private var selectedTab = 0
    @State private var isDrawerOpen = false

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let backgroundGray = Color(red: 0.949, green: 0.957, blue: 0.973)

    private var ready: (name: String, greeting: String, email: String, role: String)? {
        if case .ready(let name, let greeting, let email, let role) = viewModel.state {
            return (name, greeting, email, role)
        }
        return nil
    }

    var body: some View {
        ZStack(alignment: .leading) {
            // Main content
            VStack(spacing: 0) {
                HomeHeaderView(
                    navyBlue: navyBlue,
                    name: ready?.name ?? "",
                    greeting: ready?.greeting ?? "",
                    onMenuTap: { withAnimation(.easeInOut(duration: 0.25)) { isDrawerOpen = true } }
                )

                if selectedTab == 3 {
                    ProfileContentView(
                        navyBlue: navyBlue,
                        backgroundGray: backgroundGray,
                        name: ready?.name ?? "",
                        email: ready?.email ?? "",
                        role: ready?.role ?? "",
                        onSignOut: {
                            viewModel.signOut()
                            onSignOut()
                        }
                    )
                } else {
                    ScrollView(showsIndicators: false) {
                        VStack(spacing: 20) {
                            HomeSearchBar(navyBlue: navyBlue)
                            QuickShortcutsSection()
                            NewNoticesSection(navyBlue: navyBlue)
                            RecentActivitiesSection()
                            OngoingComplaintsSection(navyBlue: navyBlue)
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 16)
                        .padding(.bottom, 24)
                    }
                    .background(backgroundGray)
                }

                HomeBottomTabBar(selectedTab: $selectedTab, navyBlue: navyBlue)
            }
            .background(backgroundGray)

            // Scrim
            if isDrawerOpen {
                Color.black.opacity(0.35)
                    .ignoresSafeArea()
                    .onTapGesture { withAnimation(.easeInOut(duration: 0.25)) { isDrawerOpen = false } }
            }

            // Drawer
            if isDrawerOpen {
                DrawerContentView(
                    navyBlue: navyBlue,
                    name: ready?.name ?? "",
                    email: ready?.email ?? "",
                    role: ready?.role ?? "",
                    onClose: { withAnimation(.easeInOut(duration: 0.25)) { isDrawerOpen = false } },
                    onSignOut: {
                        viewModel.signOut()
                        onSignOut()
                    }
                )
                .frame(width: 300)
                .transition(.move(edge: .leading))
                .zIndex(1)
            }
        }
        .onChange(of: viewModel.shouldSignOut) { denied in
            if denied { onSignOut() }
        }
    }
}

// MARK: - Header

private struct HomeHeaderView: View {
    let navyBlue: Color
    let name: String
    let greeting: String
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
                    Text("Flat A-304")
                        .font(.system(size: 13))
                        .foregroundColor(.white.opacity(0.7))
                }
            }

            Spacer()

            Circle()
                .fill(.white.opacity(0.2))
                .frame(width: 52, height: 52)
                .overlay {
                    Image(systemName: "person")
                        .font(.system(size: 24))
                        .foregroundColor(.white)
                }
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
    let onClose: () -> Void
    let onSignOut: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Navy header
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top) {
                    Circle()
                        .fill(.white.opacity(0.2))
                        .frame(width: 64, height: 64)
                        .overlay {
                            Image(systemName: "person")
                                .font(.system(size: 30))
                                .foregroundColor(.white)
                        }
                    Spacer()
                    Button(action: onClose) {
                        Circle()
                            .fill(.white.opacity(0.15))
                            .frame(width: 32, height: 32)
                            .overlay {
                                Image(systemName: "xmark")
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundColor(.white)
                            }
                    }
                }
                .padding(.bottom, 12)

                Text(name.isEmpty ? "—" : name)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                Spacer().frame(height: 4)
                HStack(spacing: 4) {
                    Image(systemName: "house")
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.7))
                    Text("Flat A-304")
                        .font(.system(size: 13))
                        .foregroundColor(.white.opacity(0.8))
                }
                Spacer().frame(height: 2)
                Text("Tower A, Phoenix Heights")
                    .font(.system(size: 12))
                    .foregroundColor(.white.opacity(0.6))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(navyBlue.ignoresSafeArea(edges: .top))

            // Menu items
            VStack(spacing: 0) {
                DrawerMenuItemView(systemIcon: "doc.text", label: "Complaint")
                DrawerMenuItemView(systemIcon: "person.badge.plus", label: "Visitor Pass")
                DrawerMenuItemView(systemIcon: "bell", label: "Notice Board")
                DrawerMenuItemView(systemIcon: "bubble.left", label: "Feedback")
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
        .ignoresSafeArea(edges: .top)
    }
}

private struct DrawerMenuItemView: View {
    let systemIcon: String
    let label: String

    var body: some View {
        Button(action: {}) {
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
    let onSignOut: () -> Void

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
                    ProfileDetailRowView(systemIcon: "envelope", label: "Email", value: email.isEmpty ? "—" : email)
                    Divider().padding(.horizontal, 16)
                    ProfileDetailRowView(systemIcon: "person.badge.key", label: "Role", value: role.isEmpty ? "—" : role)
                    Divider().padding(.horizontal, 16)
                    ProfileDetailRowView(systemIcon: "house", label: "Unit", value: "Flat A-304")
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

// MARK: - Search Bar

private struct HomeSearchBar: View {
    let navyBlue: Color

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(Color(white: 0.67))
            Text("Search complaints, feedback, support")
                .font(.system(size: 13))
                .foregroundColor(Color(white: 0.67))
            Spacer()
        }
        .padding(12)
        .background(Color.white)
        .cornerRadius(12)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(white: 0.88), lineWidth: 1))
    }
}

// MARK: - Quick Shortcuts

private struct QuickShortcutsSection: View {
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
                        iconTint: Color(red: 0.231, green: 0.31, blue: 0.847)
                    )
                    ShortcutItemView(
                        label: "Ledger",
                        systemIcon: "doc.text",
                        iconBg: Color(red: 0.9, green: 0.969, blue: 0.969),
                        iconTint: Color(red: 0.051, green: 0.584, blue: 0.533)
                    )
                }
                HStack(spacing: 12) {
                    ShortcutItemView(
                        label: "Visitor Pass",
                        systemIcon: "person.badge.plus",
                        iconBg: Color(red: 0.925, green: 0.992, blue: 0.961),
                        iconTint: Color(red: 0.024, green: 0.588, blue: 0.416)
                    )
                    ShortcutItemView(
                        label: "Feedback",
                        systemIcon: "bubble.left",
                        iconBg: Color(red: 1.0, green: 0.969, blue: 0.929),
                        iconTint: Color(red: 0.851, green: 0.467, blue: 0.024)
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

    var body: some View {
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

// MARK: - New Notices

private struct NewNoticesSection: View {
    let navyBlue: Color

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
                Text("View All >")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(navyBlue)
            }

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
                        Text("Scheduled Maintenance - Water Supply")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        Text("Water supply will be interrupted on March 5, 2026, from 10:00 AM to 2:00 PM for maintenance work in Block A.")
                            .font(.system(size: 12))
                            .foregroundColor(Color(white: 0.33))
                        Text("Mar 4, 2026")
                            .font(.system(size: 11))
                            .foregroundColor(Color(white: 0.6))
                    }
                }
                .padding(12)
            }
            .background(Color(red: 0.94, green: 0.957, blue: 1.0))
            .cornerRadius(12)
        }
    }
}

// MARK: - Recent Activities

private struct RecentActivitiesSection: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 6) {
                Image(systemName: "waveform.path.ecg")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Color(red: 0.118, green: 0.176, blue: 0.42))
                Text("Recent Activities")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
            }

            VStack(spacing: 0) {
                ActivityRowView(title: "AC Not Working - A-304", time: "2 hours ago", status: "In Progress")
                Divider().padding(.horizontal, 16)
                ActivityRowView(title: "Plumbing Issue - A-304", time: "1 day ago", status: "Assigned")
                Divider().padding(.horizontal, 16)
                ActivityRowView(title: "Positive feedback submitted", time: "2 days ago", status: "Closed")
            }
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
        }
    }
}

private struct ActivityRowView: View {
    let title: String
    let time: String
    let status: String

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                Text(time)
                    .font(.system(size: 12))
                    .foregroundColor(Color(white: 0.6))
            }
            Spacer()
            StatusBadgeView(status: status)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }
}

// MARK: - Ongoing Complaints

private struct OngoingComplaintsSection: View {
    let navyBlue: Color

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
                Text("View All >")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(navyBlue)
            }

            HStack(spacing: 12) {
                ComplaintCardView(timeOpen: "2d open", title: "AC Not Working", category: "Electrical", status: "In Progress")
                ComplaintCardView(timeOpen: "1d open", title: "Plumbing Issue", category: "Plumbing", status: "Pending")
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
        switch status {
        case "In Progress": return (Color(red: 0.933, green: 0.949, blue: 1.0), Color(red: 0.231, green: 0.31, blue: 0.847))
        case "Assigned":    return (Color(red: 0.961, green: 0.933, blue: 1.0), Color(red: 0.486, green: 0.227, blue: 0.929))
        case "Closed":      return (Color(red: 0.925, green: 0.992, blue: 0.961), Color(red: 0.024, green: 0.588, blue: 0.416))
        case "Pending":     return (Color(red: 1.0, green: 0.969, blue: 0.929), Color(red: 0.851, green: 0.467, blue: 0.024))
        default:            return (Color(white: 0.96), Color(white: 0.38))
        }
    }

    var body: some View {
        Text(status)
            .font(.system(size: 11, weight: .medium))
            .foregroundColor(colors.fg)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(colors.bg)
            .cornerRadius(20)
    }
}

// MARK: - Bottom Tab Bar

private struct HomeBottomTabBar: View {
    @Binding var selectedTab: Int
    let navyBlue: Color

    private let tabs: [(label: String, icon: String)] = [
        ("Home", "house"),
        ("Complaints", "doc.text"),
        ("Noticeboard", "bell"),
        ("Profile", "person")
    ]

    var body: some View {
        VStack(spacing: 0) {
            Divider()
            HStack(spacing: 0) {
                ForEach(tabs.indices, id: \.self) { index in
                    let tab = tabs[index]
                    Button(action: { selectedTab = index }) {
                        VStack(spacing: 4) {
                            Image(systemName: selectedTab == index ? tab.icon + ".fill" : tab.icon)
                                .font(.system(size: 20))
                                .foregroundColor(selectedTab == index ? navyBlue : Color(white: 0.62))
                            Text(tab.label)
                                .font(.system(size: 10))
                                .foregroundColor(selectedTab == index ? navyBlue : Color(white: 0.62))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                    }
                }
            }
            .background(Color.white)
            .padding(.bottom, 4)
        }
    }
}
