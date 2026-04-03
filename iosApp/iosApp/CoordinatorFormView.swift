import SwiftUI

struct CoordinatorFormView: View {
    let occupantId: String
    let flatId: String
    let coordinatorName: String
    let flatNumber: String
    let onBack: () -> Void

    @StateObject private var viewModel: CoordinatorFormViewModel

    private let navyBlue  = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let accentBlue = Color(red: 0.145, green: 0.388, blue: 0.922)
    private let bgColor    = Color(red: 0.973, green: 0.976, blue: 0.98)
    private let borderColor = Color(red: 0.898, green: 0.918, blue: 0.933)

    init(occupantId: String, flatId: String, coordinatorName: String, flatNumber: String, onBack: @escaping () -> Void) {
        self.occupantId = occupantId
        self.flatId = flatId
        self.coordinatorName = coordinatorName
        self.flatNumber = flatNumber
        self.onBack = onBack
        _viewModel = StateObject(wrappedValue: CoordinatorFormViewModel(
            occupantId: occupantId, flatId: flatId,
            coordinatorName: coordinatorName, flatNumber: flatNumber
        ))
    }

    var body: some View {
        if viewModel.isSubmitted {
            successView
        } else {
            mainForm
        }
    }

    private var mainForm: some View {
        VStack(spacing: 0) {
            // Top bar: laid out in safe area; color extends under status bar / notch.
            ZStack {
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.white).padding(8)
                    }
                    Spacer()
                }
                .padding(.horizontal, 8)
                VStack(spacing: 2) {
                    Text("Monthly Flat Self-Check")
                        .font(.system(size: 16, weight: .semibold)).foregroundColor(.white)
                    Text("Annexure – C")
                        .font(.system(size: 12)).foregroundColor(.white.opacity(0.7))
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background {
                navyBlue
                    .ignoresSafeArea(edges: .top)
            }

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {

                    // Flat Details
                    formCard(title: "Flat Details") {
                        detailRow(label: "Flat Address", value: flatNumber)
                        Divider().padding(.vertical, 6)
                        detailRow(label: "Month", value: viewModel.month)
                        Divider().padding(.vertical, 6)
                        detailRow(label: "Flat Coordinator Name", value: coordinatorName)
                    }

                    // 1. Cleanliness
                    radioSection(
                        number: "1", title: "Cleanliness Status",
                        opt1: "Good", opt2: "Needs Attention",
                        items: cleanlinessItems, dict: viewModel.cleanliness,
                        section: .cleanliness
                    )

                    // 2. Repair & Maintenance
                    radioSection(
                        number: "2", title: "Repair & Maintenance Check",
                        opt1: "OK", opt2: "Repair Needed",
                        items: repairItems, dict: viewModel.repairs,
                        section: .repairs
                    )

                    // 3. Safety & Discipline
                    radioSection(
                        number: "3", title: "Safety & Discipline",
                        opt1: "Yes", opt2: "No",
                        items: safetyItems, dict: viewModel.safety,
                        section: .safety
                    )

                    // 4. Bill Payment Status
                    radioSection(
                        number: "4", title: "Bill Payment Status",
                        opt1: "Paid", opt2: "Not Paid",
                        items: billItems, dict: viewModel.bills,
                        section: .bills
                    )

                    // 5. HR Issues
                    formCard(title: "5. Issues Reported to HR/Admin (if any)") {
                        TextEditor(text: $viewModel.hrIssues)
                            .frame(height: 110)
                            .font(.system(size: 14))
                            .foregroundColor(Color(red: 0.216, green: 0.259, blue: 0.318))
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(borderColor, lineWidth: 1)
                            )
                            .overlay(alignment: .topLeading) {
                                if viewModel.hrIssues.isEmpty {
                                    Text("Describe any issues...")
                                        .font(.system(size: 14))
                                        .foregroundColor(.gray.opacity(0.6))
                                        .padding(8)
                                        .allowsHitTesting(false)
                                }
                            }
                    }

                    // Declaration
                    formCard(title: "Coordinator Monthly Declaration") {
                        Text("I confirm that I have checked the flat and the above information is correct.")
                            .font(.system(size: 14))
                            .foregroundColor(Color(red: 0.122, green: 0.161, blue: 0.216))
                            .padding(.bottom, 10)

                        Button(action: { viewModel.confirmed.toggle() }) {
                            HStack(alignment: .top, spacing: 10) {
                                Image(systemName: viewModel.confirmed ? "checkmark.square.fill" : "square")
                                    .foregroundColor(viewModel.confirmed ? accentBlue : .gray)
                                    .font(.system(size: 22)).padding(.top, 1)
                                Text("I confirm the above information is accurate.")
                                    .font(.system(size: 14))
                                    .foregroundColor(Color(red: 0.216, green: 0.259, blue: 0.318))
                                    .multilineTextAlignment(.leading)
                                Spacer()
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(viewModel.confirmed ? accentBlue.opacity(0.08) : Color(red: 0.98, green: 0.98, blue: 0.98))
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(viewModel.confirmed ? accentBlue : borderColor, lineWidth: 1))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                        .buttonStyle(.plain)

                        if let err = viewModel.error {
                            Text(err).foregroundColor(.red).font(.system(size: 13)).padding(.top, 6)
                        }
                    }

                    Spacer().frame(height: 8)
                }
                .padding(16)
            }

            // Submit
            VStack(spacing: 0) {
                Divider()
                Button(action: viewModel.submit) {
                    ZStack {
                        if viewModel.isSubmitting {
                            ProgressView().tint(.white)
                        } else {
                            Text("Submit Form").font(.system(size: 16, weight: .semibold))
                        }
                    }
                    .frame(maxWidth: .infinity).frame(height: 52)
                    .background(viewModel.canSubmit ? navyBlue : navyBlue.opacity(0.4))
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(!viewModel.canSubmit)
                .padding(.horizontal, 16).padding(.vertical, 12)
            }
            .background(Color.white)
        }
        .background(bgColor)
    }

    private var successView: some View {
        ZStack {
            bgColor.ignoresSafeArea()
            VStack(spacing: 16) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 72)).foregroundColor(Color(red: 0.086, green: 0.643, blue: 0.247))
                Text("Form Submitted!").font(.system(size: 22, weight: .bold))
                    .foregroundColor(Color(red: 0.122, green: 0.161, blue: 0.216))
                Text("Monthly self-check form has been successfully submitted.")
                    .font(.system(size: 14)).foregroundColor(.gray)
                    .multilineTextAlignment(.center).padding(.horizontal, 24)
                Button(action: onBack) {
                    Text("Done").font(.system(size: 16, weight: .semibold))
                        .frame(maxWidth: .infinity).frame(height: 48)
                        .background(navyBlue).foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .padding(.horizontal, 32).padding(.top, 8)
            }
            .padding(24)
        }
    }

    // MARK: - Helpers

    @ViewBuilder
    private func formCard<Content: View>(title: String, @ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.system(size: 15, weight: .semibold)).foregroundColor(accentBlue)
            content()
        }
        .padding(16).frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white).clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }

    @ViewBuilder
    private func detailRow(label: String, value: String) -> some View {
        HStack {
            Text(label).font(.system(size: 13)).foregroundColor(.gray)
            Spacer()
            Text(value.isEmpty ? "—" : value).font(.system(size: 14, weight: .medium))
                .foregroundColor(Color(red: 0.122, green: 0.161, blue: 0.216))
        }
    }

    @ViewBuilder
    private func radioSection(
        number: String, title: String,
        opt1: String, opt2: String,
        items: [String], dict: [String: String?],
        section: CoordinatorFormViewModel.Section
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("\(number). \(title)").font(.system(size: 15, weight: .semibold)).foregroundColor(accentBlue)

            // Header
            HStack {
                Text("").frame(maxWidth: .infinity, alignment: .leading)
                Text(opt1).font(.system(size: 12, weight: .semibold)).foregroundColor(.gray)
                    .frame(width: 80, alignment: .center)
                Text(opt2).font(.system(size: 12, weight: .semibold)).foregroundColor(.gray)
                    .frame(width: 80, alignment: .center)
            }
            Divider()

            ForEach(Array(items.enumerated()), id: \.offset) { idx, item in
                HStack {
                    Text(item).font(.system(size: 14))
                        .foregroundColor(Color(red: 0.122, green: 0.161, blue: 0.216))
                        .frame(maxWidth: .infinity, alignment: .leading)
                    radioButton(selected: dict[item] == opt1) {
                        viewModel.select(section: section, item: item, value: opt1)
                    }
                    .frame(width: 80)
                    radioButton(selected: dict[item] == opt2) {
                        viewModel.select(section: section, item: item, value: opt2)
                    }
                    .frame(width: 80)
                }
                .padding(.vertical, 8)
                if idx < items.count - 1 { Divider() }
            }
        }
        .padding(16).frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white).clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }

    @ViewBuilder
    private func radioButton(selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(selected ? accentBlue : Color.white)
                    .frame(width: 22, height: 22)
                Circle()
                    .stroke(selected ? accentBlue : borderColor, lineWidth: 2)
                    .frame(width: 22, height: 22)
                if selected {
                    Circle().fill(Color.white).frame(width: 9, height: 9)
                }
            }
        }
        .buttonStyle(.plain)
    }
}
