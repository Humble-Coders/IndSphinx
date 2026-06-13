import SwiftUI

struct FlatVacantRequestView: View {
    let occupantId: String
    let occupantName: String
    let flatId: String
    let flatNumber: String
    let onBack: () -> Void

    @StateObject private var viewModel = FlatVacantRequestViewModel()

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let backgroundGray = Color(red: 0.949, green: 0.957, blue: 0.973)

    private var flatAssigned: Bool { !flatId.isEmpty && !flatNumber.isEmpty }

    var body: some View {
        VStack(spacing: 0) {
            switch viewModel.state {
            case .loading:
                FVHeader(title: "Flat Vacant Request", navyBlue: navyBlue, onBack: onBack, showIcon: true)
                Spacer()
                ProgressView().tint(navyBlue)
                Spacer()

            case .loaded(let items):
                FVHeader(title: "Flat Vacant Request", navyBlue: navyBlue, onBack: onBack, showIcon: true)
                FVListView(
                    requests: items,
                    flatAssigned: flatAssigned,
                    flatNumber: flatNumber,
                    hasPending: items.contains(where: { $0.status.uppercased() == "PENDING" }),
                    navyBlue: navyBlue,
                    backgroundGray: backgroundGray,
                    onSubmitTapped: { viewModel.onSubmitTapped() },
                    onRequestSelected: { viewModel.onRequestSelected($0) }
                )

            case .submitForm:
                FVHeader(title: "New Vacant Request", navyBlue: navyBlue, onBack: { viewModel.onBackFromForm() })
                FVFormView(
                    flatNumber: flatNumber,
                    navyBlue: navyBlue,
                    backgroundGray: backgroundGray,
                    isSubmitting: false,
                    onSubmit: { reason in
                        viewModel.submit(
                            occupantId: occupantId,
                            occupantName: occupantName,
                            flatId: flatId,
                            flatNumber: flatNumber,
                            reason: reason
                        )
                    }
                )

            case .submitting:
                FVHeader(title: "New Vacant Request", navyBlue: navyBlue, onBack: {})
                Spacer()
                ProgressView().tint(navyBlue)
                Spacer()

            case .detail(let request, _):
                FVHeader(title: "Request Details", navyBlue: navyBlue, onBack: { viewModel.onBackFromDetail() })
                FVDetailView(request: request, navyBlue: navyBlue, backgroundGray: backgroundGray)

            case .error(let msg, _):
                FVHeader(title: "Flat Vacant Request", navyBlue: navyBlue, onBack: onBack, showIcon: true)
                Spacer()
                VStack(spacing: 12) {
                    Text(msg)
                        .font(.system(size: 14))
                        .foregroundColor(Color(white: 0.4))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                    Button("OK") { viewModel.dismissError() }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(navyBlue)
                }
                Spacer()
            }
        }
        .background(backgroundGray)
        .onAppear { viewModel.start(occupantId: occupantId) }
    }
}

// MARK: - Header

private struct FVHeader: View {
    let title: String
    let navyBlue: Color
    let onBack: () -> Void
    var showIcon = false

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
                if showIcon {
                    Image(systemName: "figure.walk.departure")
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

private struct FVListView: View {
    let requests: [FlatVacantRequestItem]
    let flatAssigned: Bool
    let flatNumber: String
    let hasPending: Bool
    let navyBlue: Color
    let backgroundGray: Color
    let onSubmitTapped: () -> Void
    let onRequestSelected: (FlatVacantRequestItem) -> Void

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                Text("Request to vacate your assigned flat. Admin will review and respond.")
                    .font(.system(size: 14))
                    .foregroundColor(Color(white: 0.4))
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 24)
                    .padding(.top, 20)

                if !flatAssigned {
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "info.circle")
                            .font(.system(size: 18))
                            .foregroundColor(Color(red: 0.851, green: 0.467, blue: 0.024))
                        Text("No flat is currently assigned to you. Please contact admin.")
                            .font(.system(size: 13))
                            .foregroundColor(Color(red: 0.573, green: 0.255, blue: 0.055))
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(red: 1.0, green: 0.969, blue: 0.929))
                    .cornerRadius(14)
                    .padding(.horizontal, 16)
                    .padding(.top, 16)
                } else {
                    Button(action: onSubmitTapped) {
                        HStack(spacing: 8) {
                            Image(systemName: "plus")
                                .font(.system(size: 16, weight: .semibold))
                            Text("New Vacant Request")
                                .font(.system(size: 15, weight: .semibold))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(hasPending ? Color(white: 0.75) : navyBlue)
                        .cornerRadius(14)
                    }
                    .disabled(hasPending)
                    .padding(.horizontal, 16)
                    .padding(.top, 16)

                    if hasPending {
                        Text("You already have a pending request. Wait for admin response before submitting another.")
                            .font(.system(size: 12))
                            .foregroundColor(Color(red: 0.851, green: 0.467, blue: 0.024))
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                            .padding(.horizontal, 24)
                            .padding(.top, 8)
                    }

                    HStack(spacing: 6) {
                        Image(systemName: "house")
                            .font(.system(size: 12))
                            .foregroundColor(Color(white: 0.4))
                        Text("Assigned flat: \(flatNumber)")
                            .font(.system(size: 13))
                            .foregroundColor(Color(white: 0.4))
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                }

                if !requests.isEmpty {
                    Text("Your Requests")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        .padding(.horizontal, 16)
                        .padding(.top, 24)
                        .padding(.bottom, 12)

                    VStack(spacing: 12) {
                        ForEach(requests) { req in
                            FVRequestCard(request: req, navyBlue: navyBlue, onTap: { onRequestSelected(req) })
                                .padding(.horizontal, 16)
                        }
                    }
                } else if flatAssigned {
                    Text("No previous requests")
                        .font(.system(size: 14))
                        .foregroundColor(Color(white: 0.6))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 24)
                        .background(Color.white)
                        .cornerRadius(14)
                        .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
                        .padding(.horizontal, 16)
                        .padding(.top, 24)
                }
            }
            .padding(.bottom, 24)
        }
        .background(backgroundGray)
    }
}

private struct FVRequestCard: View {
    let request: FlatVacantRequestItem
    let navyBlue: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    HStack(spacing: 4) {
                        Image(systemName: "calendar")
                            .font(.system(size: 11))
                            .foregroundColor(Color(white: 0.6))
                        Text(request.dateCreated.fvFormatted())
                            .font(.system(size: 12))
                            .foregroundColor(Color(white: 0.6))
                    }
                    Spacer()
                    FVStatusBadge(status: request.status)
                }
                Text("Flat \(request.flatNumber.isEmpty ? "—" : request.flatNumber)")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                Text(request.reason)
                    .font(.system(size: 13))
                    .foregroundColor(Color(white: 0.33))
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .cornerRadius(14)
            .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
        }
        .buttonStyle(.plain)
    }
}

private struct FVStatusBadge: View {
    let status: String

    private var colors: (bg: Color, fg: Color, label: String) {
        switch status.uppercased() {
        case "PENDING":  return (Color(red: 1.0, green: 0.969, blue: 0.929), Color(red: 0.851, green: 0.467, blue: 0.024), "Pending")
        case "ACCEPTED": return (Color(red: 0.91, green: 0.97, blue: 0.91),  Color(red: 0.18, green: 0.49, blue: 0.20),    "Accepted")
        case "REJECTED": return (Color(red: 1.0, green: 0.92, blue: 0.92),   Color(red: 0.718, green: 0.11, blue: 0.11),   "Rejected")
        default:         return (Color(white: 0.96), Color(white: 0.38), status)
        }
    }

    var body: some View {
        Text(colors.label)
            .font(.system(size: 11, weight: .medium))
            .foregroundColor(colors.fg)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(colors.bg)
            .cornerRadius(20)
    }
}

// MARK: - Form

private struct FVFormView: View {
    let flatNumber: String
    let navyBlue: Color
    let backgroundGray: Color
    let isSubmitting: Bool
    let onSubmit: (String) -> Void

    @State private var reason: String = ""
    @State private var showConfirm = false

    private var trimmed: String { reason.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var isValid: Bool { trimmed.count >= 5 }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 20) {
                    HStack(spacing: 12) {
                        Image(systemName: "house")
                            .font(.system(size: 18))
                            .foregroundColor(navyBlue)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Flat")
                                .font(.system(size: 12))
                                .foregroundColor(Color(white: 0.6))
                            Text(flatNumber.isEmpty ? "—" : flatNumber)
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        }
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 8) {
                            Image(systemName: "doc.text")
                                .font(.system(size: 18))
                                .foregroundColor(navyBlue)
                            Text("Reason for Vacancy")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        }
                        ZStack(alignment: .topLeading) {
                            if reason.isEmpty {
                                Text("Describe why you want to vacate the flat")
                                    .font(.system(size: 14))
                                    .foregroundColor(Color(white: 0.67))
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 14)
                            }
                            TextEditor(text: $reason)
                                .font(.system(size: 14))
                                .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                                .frame(minHeight: 120)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 10)
                                .scrollContentBackground(.hidden)
                                .onChange(of: reason) { newValue in
                                    if newValue.count > 500 {
                                        reason = String(newValue.prefix(500))
                                    }
                                }
                        }
                        .background(backgroundGray)
                        .cornerRadius(10)
                        Text("\(reason.count) / 500")
                            .font(.system(size: 11))
                            .foregroundColor(Color(white: 0.6))
                            .frame(maxWidth: .infinity, alignment: .trailing)
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
                        showConfirm = true
                    }
                }) {
                    Group {
                        if isSubmitting {
                            ProgressView().tint(.white)
                        } else {
                            Text("Submit Request")
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
        .alert("Confirm Request", isPresented: $showConfirm) {
            Button("Submit") { onSubmit(trimmed) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("You are requesting to vacate Flat \(flatNumber). Continue?")
        }
    }
}

// MARK: - Detail

private struct FVDetailView: View {
    let request: FlatVacantRequestItem
    let navyBlue: Color
    let backgroundGray: Color

    private var isPending: Bool { request.status.uppercased() == "PENDING" }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        HStack(spacing: 6) {
                            Image(systemName: "calendar")
                                .font(.system(size: 12))
                                .foregroundColor(Color(white: 0.6))
                            Text(request.dateCreated.fvFormatted())
                                .font(.system(size: 13))
                                .foregroundColor(Color(white: 0.6))
                        }
                        Spacer()
                        FVStatusBadge(status: request.status)
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Flat")
                            .font(.system(size: 12))
                            .foregroundColor(Color(white: 0.6))
                        Text(request.flatNumber.isEmpty ? "—" : request.flatNumber)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                    }
                    Divider()
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Reason")
                            .font(.system(size: 12))
                            .foregroundColor(Color(white: 0.6))
                        Text(request.reason)
                            .font(.system(size: 14))
                            .foregroundColor(Color(red: 0.2, green: 0.2, blue: 0.2))
                            .lineSpacing(6)
                    }
                }
                .padding(20)
                .background(Color.white)
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)

                if !isPending {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Admin Remarks")
                            .font(.system(size: 12))
                            .foregroundColor(Color(white: 0.6))
                        Text(request.adminRemarks.isEmpty ? "No remarks provided." : request.adminRemarks)
                            .font(.system(size: 14))
                            .foregroundColor(Color(red: 0.2, green: 0.2, blue: 0.2))
                            .lineSpacing(6)
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white)
                    .cornerRadius(16)
                    .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
                }
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(backgroundGray)
    }
}

// MARK: - Date helper

private extension Date {
    func fvFormatted() -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "MMM d, yyyy"
        return fmt.string(from: self)
    }
}
