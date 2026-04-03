import SwiftUI
import WebKit

struct ResidentialFormView: View {
    let onFormComplete: () -> Void
    @StateObject private var viewModel = ResidentialFormViewModel()

    private let primaryBlue = Color(red: 0.165, green: 0.188, blue: 0.502)
    private let lightBlue  = Color(red: 0.933, green: 0.941, blue: 0.98)
    private let textGray   = Color(red: 0.42, green: 0.447, blue: 0.502)
    private let borderColor = Color(red: 0.898, green: 0.918, blue: 0.922)
    private let textDark   = Color(red: 0.122, green: 0.161, blue: 0.216)
    private let textBody   = Color(red: 0.216, green: 0.259, blue: 0.318)
    private let bgColor    = Color(red: 0.97, green: 0.976, blue: 0.98)

    var body: some View {
        switch viewModel.state {
        case .loading:
            ZStack { bgColor.ignoresSafeArea(); ProgressView().tint(primaryBlue) }
        case .error(let msg):
            ZStack { bgColor.ignoresSafeArea(); Text(msg).foregroundColor(.red).padding() }
        case .submitted:
            ZStack { bgColor.ignoresSafeArea(); ProgressView().tint(primaryBlue) }
                .onAppear { onFormComplete() }
        case .ready(let name, let empId, let flatNumber, let occupantFrom, _, _,
                    let common, let room, let selected, let checked,
                    let termsHtml, let termsAccepted, let showTermsSheet, let isSubmitting):
            readyView(
                occupantName: name, empId: empId, flatNumber: flatNumber,
                occupantFrom: occupantFrom, common: common, room: room,
                selected: selected, checked: checked,
                termsHtml: termsHtml, termsAccepted: termsAccepted,
                showTermsSheet: showTermsSheet, isSubmitting: isSubmitting
            )
        }
    }

    @ViewBuilder
    private func readyView(
        occupantName: String, empId: String, flatNumber: String,
        occupantFrom: Date?, common: [String], room: [String],
        selected: Set<String>, checked: Set<Int>,
        termsHtml: String, termsAccepted: Bool,
        showTermsSheet: Bool, isSubmitting: Bool
    ) -> some View {
        let allAmenities = room + common
        let canSubmit = checked.count == responsibilities.count && termsAccepted && !isSubmitting

        VStack(spacing: 0) {
            // Top bar: laid out in safe area; color extends under status bar / notch.
            ZStack {
                Text("Residential Acceptance Form")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.bottom, 2)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background {
                primaryBlue
                    .ignoresSafeArea(edges: .top)
            }

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {

                    // Header
                    card {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Residential Acceptance Form")
                                .font(.system(size: 17, weight: .bold)).foregroundColor(textDark)
                            Text("Please review and confirm your accommodation details")
                                .font(.system(size: 13)).foregroundColor(textGray)
                        }
                    }

                    // Flat Allocation Info
                    sectionCard(title: "Flat Allocation Information") {
                        VStack(spacing: 0) {
                            infoRow(icon: "person", label: "Resident Name", value: occupantName)
                            divider
                            infoRow(icon: "rectangle.and.pencil.and.ellipsis", label: "Employee ID", value: empId)
                            divider
                            infoRow(icon: "house", label: "Flat Number", value: flatNumber)
                            divider
                            infoRow(icon: "calendar", label: "Move-In Date",
                                    value: occupantFrom.map { formatDate($0) } ?? "—")
                        }
                    }

                    // Items Received
                    if !allAmenities.isEmpty {
                        sectionCard(title: "Items Received in Flat") {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Please confirm the items you have received in your allocated accommodation.")
                                    .font(.system(size: 13)).foregroundColor(textGray)
                                LazyVGrid(
                                    columns: [GridItem(.flexible()), GridItem(.flexible())],
                                    spacing: 10
                                ) {
                                    ForEach(allAmenities, id: \.self) { amenity in
                                        amenityItem(
                                            name: amenity,
                                            selected: selected.contains(amenity)
                                        ) { viewModel.toggleAmenity(amenity) }
                                    }
                                }
                            }
                        }
                    }

                    // Resident Responsibilities (checkboxes)
                    sectionCard(title: "Resident Responsibilities") {
                        VStack(alignment: .leading, spacing: 0) {
                            Text("Please confirm each responsibility by checking the boxes below.")
                                .font(.system(size: 13)).foregroundColor(textGray)
                                .padding(.bottom, 8)
                            ForEach(Array(responsibilities.enumerated()), id: \.offset) { index, text in
                                Button(action: { viewModel.toggleResponsibility(index) }) {
                                    HStack(alignment: .top, spacing: 10) {
                                        Image(systemName: checked.contains(index) ? "checkmark.square.fill" : "square")
                                            .foregroundColor(checked.contains(index) ? primaryBlue : textGray)
                                            .font(.system(size: 22))
                                            .padding(.top, 1)
                                        Text(text)
                                            .font(.system(size: 14))
                                            .foregroundColor(textBody)
                                            .multilineTextAlignment(.leading)
                                        Spacer()
                                    }
                                    .padding(.vertical, 8)
                                }
                                .buttonStyle(.plain)
                                if index < responsibilities.count - 1 {
                                    Divider().padding(.leading, 34)
                                }
                            }
                        }
                    }

                    // Terms & Conditions
                    sectionCard(title: "Terms & Conditions") {
                        VStack(alignment: .leading, spacing: 10) {
                            if !termsHtml.isEmpty {
                                Button(action: { viewModel.setShowTermsSheet(true) }) {
                                    HStack {
                                        Image(systemName: "doc.text")
                                        Text("View Terms & Conditions")
                                            .font(.system(size: 15, weight: .medium))
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .foregroundColor(primaryBlue)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(primaryBlue, lineWidth: 1)
                                    )
                                }
                                .buttonStyle(.plain)
                            }

                            Button(action: { viewModel.setTermsAccepted(!termsAccepted) }) {
                                HStack(alignment: .top, spacing: 10) {
                                    Image(systemName: termsAccepted ? "checkmark.square.fill" : "square")
                                        .foregroundColor(termsAccepted ? primaryBlue : textGray)
                                        .font(.system(size: 22))
                                        .padding(.top, 1)
                                    Text("I accept the terms and conditions of the company residential accommodation.")
                                        .font(.system(size: 14))
                                        .foregroundColor(textBody)
                                        .multilineTextAlignment(.leading)
                                    Spacer()
                                }
                                .padding(12)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(termsAccepted ? lightBlue : Color(red: 0.98, green: 0.98, blue: 0.98))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(termsAccepted ? primaryBlue : borderColor, lineWidth: 1)
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    Spacer().frame(height: 8)
                }
                .padding(16)
            }

            // Submit button
            VStack(spacing: 6) {
                Button(action: viewModel.submitForm) {
                    ZStack {
                        if isSubmitting {
                            ProgressView().tint(.white)
                        } else {
                            HStack(spacing: 8) {
                                Image(systemName: "checkmark.circle")
                                Text("Submit Agreement").font(.system(size: 16, weight: .semibold))
                            }
                        }
                    }
                    .frame(maxWidth: .infinity).frame(height: 52)
                    .background(canSubmit ? primaryBlue : primaryBlue.opacity(0.4))
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(!canSubmit)

                Text("By submitting this form, you confirm that the provided information is accurate.")
                    .font(.system(size: 11)).foregroundColor(textGray).multilineTextAlignment(.center)
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 16)
            .background(Color.white)
        }
        .background(bgColor)
        .sheet(isPresented: Binding(
            get: { showTermsSheet },
            set: { viewModel.setShowTermsSheet($0) }
        )) {
            TermsSheetView(html: termsHtml, onClose: { viewModel.setShowTermsSheet(false) })
        }
    }

    // MARK: - Helper views

    @ViewBuilder
    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        content()
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }

    @ViewBuilder
    private func sectionCard<Content: View>(title: String, @ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.system(size: 16, weight: .semibold)).foregroundColor(textDark)
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }

    @ViewBuilder
    private func infoRow(icon: String, label: String, value: String) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 8).fill(lightBlue).frame(width: 36, height: 36)
                Image(systemName: icon).font(.system(size: 15)).foregroundColor(primaryBlue)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.system(size: 12)).foregroundColor(textGray)
                Text(value.isEmpty ? "—" : value).font(.system(size: 15, weight: .medium)).foregroundColor(textDark)
            }
            Spacer()
        }
        .padding(.vertical, 2)
    }

    private var divider: some View { Divider().padding(.vertical, 8) }

    @ViewBuilder
    private func amenityItem(name: String, selected: Bool, onTap: @escaping () -> Void) -> some View {
        Button(action: onTap) {
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "square.grid.2x2")
                        .font(.system(size: 14))
                        .foregroundColor(selected ? primaryBlue : textGray)
                    Text(name)
                        .font(.system(size: 13, weight: selected ? .medium : .regular))
                        .foregroundColor(selected ? primaryBlue : textBody)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
                Spacer()
                ZStack {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(selected ? primaryBlue : Color.clear)
                        .frame(width: 18, height: 18)
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(selected ? primaryBlue : borderColor, lineWidth: 1.5)
                        .frame(width: 18, height: 18)
                    if selected {
                        Image(systemName: "checkmark").font(.system(size: 10, weight: .bold)).foregroundColor(.white)
                    }
                }
            }
            .padding(10)
            .background(selected ? lightBlue : Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(selected ? primaryBlue : borderColor, lineWidth: 1.5)
            )
        }
        .buttonStyle(.plain)
    }

    private func formatDate(_ date: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "MMMM d, yyyy"; return f.string(from: date)
    }
}

// MARK: - Terms Full-Screen Sheet

struct TermsSheetView: View {
    let html: String
    let onClose: () -> Void
    private let primaryBlue = Color(red: 0.165, green: 0.188, blue: 0.502)

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                HStack {
                    Button(action: onClose) {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.white)
                            .padding(8)
                    }
                    Spacer()
                }
                .padding(.horizontal, 8)
                Text("Terms & Conditions")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.white)
            }
            .frame(height: 52)
            .background {
                primaryBlue
                    .ignoresSafeArea(edges: .top)
            }

            HTMLView(html: html)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - WKWebView wrapper

struct HTMLView: UIViewRepresentable {
    let html: String

    func makeUIView(context: Context) -> WKWebView {
        let wv = WKWebView()
        wv.isOpaque = false
        wv.backgroundColor = .clear
        wv.clipsToBounds = true
        wv.scrollView.clipsToBounds = true
        wv.scrollView.contentInsetAdjustmentBehavior = .automatic
        wv.scrollView.backgroundColor = .clear
        return wv
    }

    func updateUIView(_ wv: WKWebView, context: Context) {
        let styled = """
        <html><head>
        <meta name='viewport' content='width=device-width,initial-scale=1'>
        <style>body{font-family:-apple-system,sans-serif;font-size:15px;color:#374151;line-height:1.7;margin:16px;padding:0;}b{color:#1F2937;}</style>
        </head><body>\(html)</body></html>
        """
        wv.loadHTMLString(styled, baseURL: nil)
    }
}
