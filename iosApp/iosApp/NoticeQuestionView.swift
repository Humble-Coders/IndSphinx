import SwiftUI

struct NoticeQuestionView: View {
    let noticeId: String
    let displayName: String
    let flatNo: String
    let recipientType: String
    let onBack: () -> Void

    @StateObject private var viewModel: NoticeQuestionViewModel

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let backgroundGray = Color(red: 0.949, green: 0.957, blue: 0.973)

    init(noticeId: String, displayName: String, flatNo: String, recipientType: String, onBack: @escaping () -> Void) {
        self.noticeId = noticeId
        self.displayName = displayName
        self.flatNo = flatNo
        self.recipientType = recipientType
        self.onBack = onBack
        _viewModel = StateObject(wrappedValue: NoticeQuestionViewModel(noticeId: noticeId))
    }

    var body: some View {
        VStack(spacing: 0) {
            QuestionHeaderView(title: "Respond to Notice", navyBlue: navyBlue, onBack: onBack)

            switch viewModel.state {
            case .loading:
                Spacer()
                ProgressView().tint(navyBlue)
                Spacer()
            case .error(let msg):
                Spacer()
                Text(msg).font(.system(size: 14)).foregroundColor(Color(white: 0.6)).padding()
                Spacer()
            case .alreadyResponded(let notice):
                AlreadyRespondedView(title: notice.title, navyBlue: navyBlue)
            case .ready(let notice):
                QuestionResponseContentView(
                    title: notice.title,
                    description: notice.description,
                    options: notice.options,
                    allowMultiple: notice.allowMultipleChoices,
                    selectedOptions: viewModel.selectedOptions,
                    textInput: $viewModel.textInput,
                    isSubmitting: viewModel.isSubmitting,
                    isDeadlinePassed: viewModel.isDeadlinePassed,
                    navyBlue: navyBlue,
                    onToggleOption: { viewModel.toggleOption($0) },
                    onSubmit: { viewModel.submit(displayName: displayName, flatNo: flatNo, recipientType: recipientType) }
                )
            }
        }
        .background(backgroundGray.ignoresSafeArea())
    }
}

// MARK: - Question Notification View

struct QuestionNotificationView: View {
    let qnId: String
    let displayName: String
    let flatNo: String
    let recipientType: String
    let onBack: () -> Void

    @StateObject private var viewModel: QuestionNotificationViewModel

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let backgroundGray = Color(red: 0.949, green: 0.957, blue: 0.973)

    init(qnId: String, displayName: String, flatNo: String, recipientType: String, onBack: @escaping () -> Void) {
        self.qnId = qnId
        self.displayName = displayName
        self.flatNo = flatNo
        self.recipientType = recipientType
        self.onBack = onBack
        _viewModel = StateObject(wrappedValue: QuestionNotificationViewModel(qnId: qnId))
    }

    var body: some View {
        VStack(spacing: 0) {
            QuestionHeaderView(title: "Question", navyBlue: navyBlue, onBack: onBack)

            switch viewModel.state {
            case .loading:
                Spacer()
                ProgressView().tint(navyBlue)
                Spacer()
            case .error(let msg):
                Spacer()
                Text(msg).font(.system(size: 14)).foregroundColor(Color(white: 0.6)).padding()
                Spacer()
            case .alreadyResponded(let qn):
                AlreadyRespondedView(title: qn.title, navyBlue: navyBlue)
            case .ready(let qn):
                QuestionResponseContentView(
                    title: qn.title,
                    description: qn.description,
                    options: qn.options,
                    allowMultiple: qn.allowMultipleChoices,
                    selectedOptions: viewModel.selectedOptions,
                    textInput: $viewModel.textInput,
                    isSubmitting: viewModel.isSubmitting,
                    isDeadlinePassed: viewModel.isDeadlinePassed,
                    navyBlue: navyBlue,
                    onToggleOption: { viewModel.toggleOption($0) },
                    onSubmit: { viewModel.submit(displayName: displayName, flatNo: flatNo, recipientType: recipientType) }
                )
            }
        }
        .background(backgroundGray.ignoresSafeArea())
    }
}

// MARK: - Shared Components

private struct QuestionHeaderView: View {
    let title: String
    let navyBlue: Color
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 16) {
            Button(action: onBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 20))
                    .foregroundColor(.white)
            }
            Text(title)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
        .background(navyBlue.ignoresSafeArea(edges: .top))
    }
}

private struct QuestionResponseContentView: View {
    let title: String
    let description: String
    let options: [String]
    let allowMultiple: Bool
    let selectedOptions: Set<String>
    @Binding var textInput: String
    let isSubmitting: Bool
    let isDeadlinePassed: Bool
    let navyBlue: Color
    let onToggleOption: (String) -> Void
    let onSubmit: () -> Void

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                VStack(alignment: .leading, spacing: 12) {
                    Text(title)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))

                    Text(description)
                        .font(.system(size: 14))
                        .foregroundColor(Color(white: 0.33))
                        .lineSpacing(6)

                    if isDeadlinePassed {
                        Text("Response deadline has passed.")
                            .font(.system(size: 13))
                            .foregroundColor(Color(red: 0.898, green: 0.224, blue: 0.208))
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(red: 1.0, green: 0.95, blue: 0.95))
                            .cornerRadius(8)
                    } else {
                        Divider().padding(.vertical, 4)

                        if !options.isEmpty {
                            Text(allowMultiple ? "Select all that apply:" : "Select one:")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(Color(white: 0.53))

                            VStack(spacing: 8) {
                                ForEach(options, id: \.self) { option in
                                    let selected = selectedOptions.contains(option)
                                    HStack {
                                        Text(option)
                                            .font(.system(size: 14, weight: selected ? .medium : .regular))
                                            .foregroundColor(selected ? navyBlue : Color(red: 0.2, green: 0.2, blue: 0.2))
                                        Spacer()
                                        if selected {
                                            Image(systemName: "checkmark.circle.fill")
                                                .foregroundColor(navyBlue)
                                                .font(.system(size: 18))
                                        }
                                    }
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 12)
                                    .background(selected ? Color(red: 0.933, green: 0.949, blue: 1.0) : Color(red: 0.973, green: 0.976, blue: 0.984))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10)
                                            .stroke(selected ? navyBlue : Color(white: 0.88), lineWidth: 1.5)
                                    )
                                    .cornerRadius(10)
                                    .onTapGesture { onToggleOption(option) }
                                }
                            }
                        } else {
                            Text("Your response:")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(Color(white: 0.53))

                            TextEditor(text: $textInput)
                                .font(.system(size: 14))
                                .frame(height: 120)
                                .padding(8)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(Color(white: 0.88), lineWidth: 1)
                                )
                                .cornerRadius(10)
                        }

                        let canSubmit = options.isEmpty ? !textInput.trimmingCharacters(in: .whitespaces).isEmpty : !selectedOptions.isEmpty
                        Button(action: onSubmit) {
                            Group {
                                if isSubmitting {
                                    ProgressView().tint(.white)
                                } else {
                                    Text("Submit Response")
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(.white)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                        }
                        .background(canSubmit && !isSubmitting ? navyBlue : Color(white: 0.8))
                        .cornerRadius(10)
                        .disabled(!canSubmit || isSubmitting)
                        .padding(.top, 8)
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
    }
}

private struct AlreadyRespondedView: View {
    let title: String
    let navyBlue: Color

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            ZStack {
                Circle()
                    .fill(Color(red: 0.910, green: 0.961, blue: 0.914))
                    .frame(width: 88, height: 88)
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 52))
                    .foregroundColor(Color(red: 0.180, green: 0.490, blue: 0.196))
            }
            VStack(spacing: 8) {
                Text("Response Recorded")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                Text("Thank you. Your response to \u{201C}\(title)\u{201D} has been saved.")
                    .font(.system(size: 14))
                    .foregroundColor(Color(white: 0.4))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                    .padding(.horizontal, 32)
            }
            Spacer()
        }
    }
}
