import SwiftUI

struct VisitorPassView: View {
    let occupantId: String
    let occupantName: String
    let flatId: String
    let flatNumber: String
    let onBack: () -> Void

    @StateObject private var viewModel = VisitorPassViewModel()

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let backgroundGray = Color(red: 0.949, green: 0.957, blue: 0.973)

    var body: some View {
        VStack(spacing: 0) {
            switch viewModel.state {
            case .loading:
                VPHeader(title: "Visitor Pass", navyBlue: navyBlue, onBack: onBack)
                Spacer()
                ProgressView().tint(navyBlue)
                Spacer()

            case .loaded(let passes):
                VPHeader(title: "Visitor Pass", navyBlue: navyBlue, onBack: onBack)
                PassListView(
                    passes: passes,
                    navyBlue: navyBlue,
                    backgroundGray: backgroundGray,
                    onRequestTapped: { viewModel.onRequestPassTapped(flatId: flatId) },
                    onPassSelected: { viewModel.onPassSelected($0) }
                )

            case .requestForm:
                VPHeader(
                    title: "Visitor Entry",
                    navyBlue: navyBlue,
                    onBack: { viewModel.onBackFromForm() },
                    trailingIcon: "person.badge.plus"
                )
                PassFormView(
                    navyBlue: navyBlue,
                    backgroundGray: backgroundGray,
                    isSubmitting: false,
                    onSubmit: { vName, vPhone, purpose, rel, visitDate in
                        viewModel.submitPass(
                            occupantId: occupantId,
                            occupantName: occupantName,
                            flatId: flatId,
                            flatNumber: flatNumber,
                            visitorName: vName,
                            visitorPhone: vPhone,
                            purposeOfVisit: purpose,
                            relationshipWithVisitor: rel,
                            visitDate: visitDate
                        )
                    }
                )

            case .submitting:
                VPHeader(title: "Visitor Entry", navyBlue: navyBlue, onBack: {}, trailingIcon: "person.badge.plus")
                Spacer()
                ProgressView().tint(navyBlue)
                Spacer()

            case .detail(let pass, _):
                VPHeader(title: "Pass Details", navyBlue: navyBlue, onBack: { viewModel.onBackFromDetail() })
                PassDetailView(pass: pass, navyBlue: navyBlue, backgroundGray: backgroundGray)

            case .error(let msg, _):
                VPHeader(title: "Visitor Pass", navyBlue: navyBlue, onBack: { viewModel.dismissError() })
                Spacer()
                Text(msg).font(.system(size: 14)).foregroundColor(Color(white: 0.6)).padding()
                Spacer()
            }
        }
        .background(backgroundGray)
        .onAppear { viewModel.start(occupantId: occupantId) }
    }
}

// MARK: - Header

private struct VPHeader: View {
    let title: String
    let navyBlue: Color
    let onBack: () -> Void
    var trailingIcon: String? = nil

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 16) {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 20))
                        .foregroundColor(navyBlue)
                }
                Text(title)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                Spacer()
                if let icon = trailingIcon {
                    Image(systemName: icon)
                        .font(.system(size: 20))
                        .foregroundColor(navyBlue)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .background(Color.white)
            Divider()
        }
    }
}

// MARK: - Filter

private struct VPFilterChip: View {
    let label: String
    let count: Int
    let isSelected: Bool
    let activeColor: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                if isSelected {
                    Circle()
                        .fill(Color.white.opacity(0.85))
                        .frame(width: 6, height: 6)
                }
                Text(label)
                    .font(.system(size: 13, weight: isSelected ? .semibold : .regular))
                    .foregroundColor(isSelected ? .white : Color(white: 0.53))
                if count > 0 {
                    Text("\(count)")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(isSelected ? .white.opacity(0.85) : Color(white: 0.53))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(isSelected ? Color.white.opacity(0.25) : Color(white: 0.92))
                        .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 9)
            .background(isSelected ? activeColor : Color.white)
            .clipShape(Capsule())
            .shadow(color: isSelected ? activeColor.opacity(0.35) : .black.opacity(0.05),
                    radius: isSelected ? 6 : 2, x: 0, y: isSelected ? 3 : 1)
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isSelected)
    }
}

// MARK: - List

private struct PassListView: View {
    let passes: [VisitorPass]
    let navyBlue: Color
    let backgroundGray: Color
    let onRequestTapped: () -> Void
    let onPassSelected: (VisitorPass) -> Void

    @State private var selectedFilter = "ALL"

    private struct FilterOption {
        let key: String
        let label: String
        let color: Color
    }

    private let filterOptions: [FilterOption] = [
        .init(key: "ALL",      label: "All",      color: Color(red: 0.118, green: 0.176, blue: 0.42)),
        .init(key: "PENDING",  label: "Pending",  color: Color(red: 0.851, green: 0.467, blue: 0.024)),
        .init(key: "ACCEPTED", label: "Accepted", color: Color(red: 0.024, green: 0.588, blue: 0.416)),
        .init(key: "REJECTED", label: "Rejected", color: Color(red: 0.898, green: 0.224, blue: 0.208))
    ]

    private var displayedPasses: [VisitorPass] {
        selectedFilter == "ALL" ? passes : passes.filter { $0.status == selectedFilter }
    }

    private func count(for key: String) -> Int {
        key == "ALL" ? passes.count : passes.filter { $0.status == key }.count
    }

    var body: some View {
        VStack(spacing: 0) {
            // Sticky header: request button + filter chips
            VStack(spacing: 12) {
                // Request button
                Button(action: onRequestTapped) {
                    HStack(spacing: 8) {
                        Image(systemName: "person.badge.plus")
                            .font(.system(size: 18))
                        Text("Request Visitor Pass")
                            .font(.system(size: 15, weight: .semibold))
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 15)
                    .background(navyBlue)
                    .cornerRadius(14)
                }
                .buttonStyle(.plain)

                // Filter chips
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(filterOptions, id: \.key) { option in
                            VPFilterChip(
                                label: option.label,
                                count: count(for: option.key),
                                isSelected: selectedFilter == option.key,
                                activeColor: option.color,
                                onTap: { withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                                    selectedFilter = option.key
                                }}
                            )
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 12)
            .background(backgroundGray)

            // Scrollable pass list
            ScrollView(showsIndicators: false) {
                VStack(spacing: 10) {
                    if displayedPasses.isEmpty {
                        VStack(spacing: 8) {
                            Image(systemName: "person.crop.circle.badge.questionmark")
                                .font(.system(size: 40))
                                .foregroundColor(Color(white: 0.8))
                            Text(passes.isEmpty ? "No visitor passes yet"
                                               : "No \(selectedFilter.lowercased()) passes")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(Color(white: 0.6))
                            if !passes.isEmpty {
                                Text("Try a different filter")
                                    .font(.system(size: 12))
                                    .foregroundColor(Color(white: 0.73))
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.top, 64)
                    } else {
                        ForEach(displayedPasses) { pass in
                            PassCardView(pass: pass, navyBlue: navyBlue, onTap: { onPassSelected(pass) })
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 4)
                .padding(.bottom, 24)
            }
            .background(backgroundGray)
        }
    }
}

private struct PassCardView: View {
    let pass: VisitorPass
    let navyBlue: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(pass.visitorName)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        .lineLimit(1)

                    HStack(spacing: 10) {
                        HStack(spacing: 4) {
                            Image(systemName: "calendar")
                                .font(.system(size: 11))
                                .foregroundColor(Color(white: 0.6))
                            Text(pass.visitDate.shortFormatted())
                                .font(.system(size: 12))
                                .foregroundColor(Color(white: 0.6))
                        }
                        HStack(spacing: 4) {
                            Image(systemName: "person.2")
                                .font(.system(size: 11))
                                .foregroundColor(Color(white: 0.6))
                            Text(pass.relationshipWithVisitor)
                                .font(.system(size: 12))
                                .foregroundColor(Color(white: 0.6))
                                .lineLimit(1)
                        }
                    }

                    VPStatusBadge(status: pass.status)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14))
                    .foregroundColor(Color(white: 0.73))
            }
            .padding(16)
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
        }
    }
}

// MARK: - Form

private struct PassFormView: View {
    let navyBlue: Color
    let backgroundGray: Color
    let isSubmitting: Bool
    let onSubmit: (String, String, String, String, Date) -> Void

    @State private var visitorName = ""
    @State private var visitorPhone = ""
    @State private var purpose = ""
    @State private var relationship = ""
    @State private var visitDate = Date()
    @State private var showDatePicker = false

    private var isValid: Bool {
        !visitorName.trimmingCharacters(in: .whitespaces).isEmpty &&
        !visitorPhone.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 20) {
                    VPFormField(
                        label: "Visitor Name",
                        icon: "person.badge.plus",
                        placeholder: "Enter visitor's full name",
                        text: $visitorName,
                        navyBlue: navyBlue
                    )
                    VPFormField(
                        label: "Phone Number",
                        icon: "phone",
                        placeholder: "Enter phone number",
                        text: $visitorPhone,
                        navyBlue: navyBlue,
                        keyboardType: .phonePad
                    )
                    VPFormField(
                        label: "Purpose of Visit",
                        icon: "doc.text",
                        placeholder: "Describe the purpose of visit",
                        text: $purpose,
                        navyBlue: navyBlue,
                        multiline: true
                    )
                    VPFormField(
                        label: "Relationship With Visitor",
                        icon: "person.2",
                        placeholder: "Enter your relationship with visitor",
                        text: $relationship,
                        navyBlue: navyBlue
                    )

                    // Visit Date
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 8) {
                            Image(systemName: "calendar.badge.clock")
                                .font(.system(size: 18))
                                .foregroundColor(navyBlue)
                            Text("Visit Date")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        }
                        DatePicker(
                            "",
                            selection: $visitDate,
                            in: Date()...,
                            displayedComponents: .date
                        )
                        .datePickerStyle(.compact)
                        .labelsHidden()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .background(Color(red: 0.949, green: 0.957, blue: 0.973))
                        .cornerRadius(10)
                        .tint(navyBlue)
                    }
                }
                .padding(20)
                .background(Color.white)
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
                .padding(.horizontal, 16)
                .padding(.top, 16)

                Button(action: {
                    if isValid && !isSubmitting {
                        onSubmit(
                            visitorName.trimmingCharacters(in: .whitespaces),
                            visitorPhone.trimmingCharacters(in: .whitespaces),
                            purpose.trimmingCharacters(in: .whitespaces),
                            relationship.trimmingCharacters(in: .whitespaces),
                            visitDate
                        )
                    }
                }) {
                    Group {
                        if isSubmitting {
                            ProgressView().tint(.white)
                        } else {
                            Text("Submit Visitor Pass")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.white)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(isValid ? navyBlue : Color(white: 0.75))
                    .cornerRadius(14)
                }
                .disabled(!isValid || isSubmitting)
                .padding(.horizontal, 16)
                .padding(.top, 24)
                .padding(.bottom, 24)
            }
        }
        .background(backgroundGray)
    }
}

private struct VPFormField: View {
    let label: String
    let icon: String
    let placeholder: String
    @Binding var text: String
    let navyBlue: Color
    var keyboardType: UIKeyboardType = .default
    var multiline: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(navyBlue)
                Text(label)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
            }
            if multiline {
                ZStack(alignment: .topLeading) {
                    if text.isEmpty {
                        Text(placeholder)
                            .font(.system(size: 14))
                            .foregroundColor(Color(white: 0.67))
                            .padding(.horizontal, 16)
                            .padding(.vertical, 14)
                    }
                    TextEditor(text: $text)
                        .font(.system(size: 14))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        .frame(minHeight: 90)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .scrollContentBackground(.hidden)
                }
                .background(Color(red: 0.949, green: 0.957, blue: 0.973))
                .cornerRadius(10)
            } else {
                TextField(placeholder, text: $text)
                    .font(.system(size: 14))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                    .keyboardType(keyboardType)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .background(Color(red: 0.949, green: 0.957, blue: 0.973))
                    .cornerRadius(10)
            }
        }
    }
}

// MARK: - Detail

private struct PassDetailView: View {
    let pass: VisitorPass
    let navyBlue: Color
    let backgroundGray: Color

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text(pass.visitorName)
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        Spacer()
                        VPStatusBadge(status: pass.status)
                    }
                    Divider()
                    VPDetailRow(icon: "phone", label: "Phone", value: pass.visitorPhone, navyBlue: navyBlue)
                    VPDetailRow(icon: "person.2", label: "Relationship", value: pass.relationshipWithVisitor, navyBlue: navyBlue)
                    VPDetailRow(icon: "calendar", label: "Visit Date", value: pass.visitDate.shortFormatted(), navyBlue: navyBlue)
                    VPDetailRow(icon: "clock", label: "Requested On", value: pass.requestDate.shortFormatted(), navyBlue: navyBlue)
                    VPDetailRow(icon: "doc.text", label: "Purpose", value: pass.purposeOfVisit.isEmpty ? "—" : pass.purposeOfVisit, navyBlue: navyBlue)
                    VPDetailRow(icon: "house", label: "Flat", value: pass.flatNumber, navyBlue: navyBlue)

                    if pass.status != "PENDING" && !pass.remarks.isEmpty {
                        Divider()
                        AdminRemarksBlock(status: pass.status, remarks: pass.remarks)
                    }
                }
                .padding(20)
                .background(Color.white)
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
                .padding(16)
            }
            .padding(.bottom, 24)
        }
        .background(backgroundGray)
    }
}

private struct AdminRemarksBlock: View {
    let status: String
    let remarks: String

    private var isAccepted: Bool { status == "ACCEPTED" }

    var body: some View {
        let bg = isAccepted
            ? Color(red: 0.925, green: 0.992, blue: 0.961)
            : Color(red: 1.0,   green: 0.933, blue: 0.933)
        let titleColor = isAccepted
            ? Color(red: 0.020, green: 0.588, blue: 0.412)
            : Color(red: 0.898, green: 0.224, blue: 0.208)
        VStack(alignment: .leading, spacing: 6) {
            Text(isAccepted ? "Remarks from admin" : "Reason for rejection")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(titleColor)
            Text(remarks)
                .font(.system(size: 14))
                .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(bg)
        .cornerRadius(12)
    }
}

private struct VPDetailRow: View {
    let icon: String
    let label: String
    let value: String
    let navyBlue: Color

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(red: 0.941, green: 0.953, blue: 1.0))
                .frame(width: 36, height: 36)
                .overlay {
                    Image(systemName: icon)
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
    }
}

// MARK: - Status Badge

private struct VPStatusBadge: View {
    let status: String

    private var colors: (bg: Color, fg: Color) {
        switch status {
        case "PENDING":  return (Color(red: 1.0, green: 0.969, blue: 0.929), Color(red: 0.851, green: 0.467, blue: 0.024))
        case "ACCEPTED": return (Color(red: 0.925, green: 0.992, blue: 0.961), Color(red: 0.024, green: 0.588, blue: 0.416))
        case "REJECTED": return (Color(red: 1.0, green: 0.933, blue: 0.933), Color(red: 0.898, green: 0.224, blue: 0.208))
        default:         return (Color(white: 0.96), Color(white: 0.38))
        }
    }

    var body: some View {
        Text(status.prefix(1) + status.dropFirst().lowercased())
            .font(.system(size: 11, weight: .medium))
            .foregroundColor(colors.fg)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(colors.bg)
            .cornerRadius(20)
    }
}

// MARK: - Date helper

private extension Date {
    func shortFormatted() -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "MMM d, yyyy"
        return fmt.string(from: self)
    }
}
