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

// MARK: - List

private struct PassListView: View {
    let passes: [VisitorPass]
    let navyBlue: Color
    let backgroundGray: Color
    let onRequestTapped: () -> Void
    let onPassSelected: (VisitorPass) -> Void

    private var grouped: [(String, [VisitorPass])] {
        [("PENDING", passes.filter { $0.status == "PENDING" }),
         ("ACCEPTED", passes.filter { $0.status == "ACCEPTED" }),
         ("REJECTED", passes.filter { $0.status == "REJECTED" })]
        .filter { !$1.isEmpty }
    }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
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
                    .padding(.vertical, 16)
                    .background(navyBlue)
                    .cornerRadius(14)
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)

                if passes.isEmpty {
                    Text("No visitor passes yet")
                        .font(.system(size: 14))
                        .foregroundColor(Color(white: 0.6))
                        .frame(maxWidth: .infinity)
                        .padding(.top, 48)
                } else {
                    ForEach(grouped, id: \.0) { status, list in
                        Text(status.prefix(1) + status.dropFirst().lowercased())
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(Color(white: 0.33))
                            .padding(.horizontal, 16)
                            .padding(.top, 20)
                            .padding(.bottom, 8)

                        VStack(spacing: 10) {
                            ForEach(list) { pass in
                                PassCardView(pass: pass, navyBlue: navyBlue, onTap: { onPassSelected(pass) })
                                    .padding(.horizontal, 16)
                            }
                        }
                    }
                }
            }
            .padding(.bottom, 24)
        }
        .background(backgroundGray)
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
