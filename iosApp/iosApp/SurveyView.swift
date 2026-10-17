import SwiftUI

struct SurveyView: View {
    /// From `SurveySessionStore`, so the answers outlive this screen.
    @ObservedObject var viewModel: SurveyViewModel
    let displayName: String
    let flatNo: String
    let recipientType: String
    let onBack: () -> Void

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let backgroundGray = Color(red: 0.949, green: 0.957, blue: 0.973)

    var body: some View {
        VStack(spacing: 0) {
            SurveyHeaderView(navyBlue: navyBlue, onBack: onBack)

            switch viewModel.state {
            case .loading:
                Spacer()
                ProgressView().tint(navyBlue)
                Spacer()
            case .error(let message):
                Spacer()
                VStack(spacing: 8) {
                    Text(message)
                        .font(.system(size: 14))
                        .foregroundColor(Color(white: 0.38))
                        .multilineTextAlignment(.center)
                    Button("Try again") { viewModel.retry() }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(navyBlue)
                }
                .padding()
                Spacer()
            case .alreadyResponded(let notice, let sending):
                SurveyRecordedView(title: notice.title, sending: sending)
            case .ready(let notice):
                SurveyFormView(
                    notice: notice,
                    viewModel: viewModel,
                    navyBlue: navyBlue,
                    onSubmit: {
                        viewModel.submit(displayName: displayName, flatNo: flatNo, recipientType: recipientType)
                    }
                )
            }
        }
        .background(backgroundGray.ignoresSafeArea())
        // The deadline timer stops while the phone sleeps: check again
        // whenever the app comes back to the front.
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            viewModel.recheckDeadline()
        }
    }
}

// MARK: - Header

private struct SurveyHeaderView: View {
    let navyBlue: Color
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 16) {
            Button(action: onBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 20))
                    .foregroundColor(.white)
            }
            .accessibilityLabel("Back")
            Text("Survey")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
        .background(navyBlue.ignoresSafeArea(edges: .top))
    }
}

// MARK: - Form

private let surveyInk = Color(red: 0.102, green: 0.102, blue: 0.18)

/// Scroll target for a question's answer box, apart from its card's own id.
private func surveyAnswerAnchor(_ questionId: String) -> String { "answer-\(questionId)" }
private let surveyRed = Color(red: 0.898, green: 0.224, blue: 0.208)
private let surveyGreen = Color(red: 0.180, green: 0.490, blue: 0.196)

private struct SurveyFormView: View {
    let notice: Notice
    @ObservedObject var viewModel: SurveyViewModel
    let navyBlue: Color
    let onSubmit: () -> Void

    /// Set by the first Submit tap with questions still open. From then on
    /// every unanswered question says so, until it is answered.
    @State private var showMissing = false
    @FocusState private var focusedQuestion: String?

    private static let submitAnchor = "survey-submit"

    var body: some View {
        let questions = notice.questions
        let answeredCount = SurveyRules.answeredCount(questions, viewModel.answers)
        let remaining = questions.count - answeredCount

        ScrollViewReader { proxy in
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 12) {
                    SurveyIntroCard(notice: notice, isDeadlinePassed: viewModel.isDeadlinePassed, navyBlue: navyBlue)

                    if !viewModel.isDeadlinePassed {
                        SurveyProgressView(answered: answeredCount, total: questions.count, navyBlue: navyBlue)
                            .padding(.horizontal, 4)

                        ForEach(Array(questions.enumerated()), id: \.element.id) { index, question in
                            let answer = viewModel.answers[question.id]
                            SurveyQuestionCard(
                                question: question,
                                number: index + 1,
                                total: questions.count,
                                answer: answer,
                                showMissing: showMissing && !SurveyRules.isAnswered(question, answer),
                                enabled: !viewModel.isSubmitting,
                                navyBlue: navyBlue,
                                text: Binding(
                                    get: { viewModel.answers[question.id]?.textResponse ?? "" },
                                    set: { viewModel.setText(questionId: question.id, text: $0) }
                                ),
                                focus: $focusedQuestion,
                                onToggleOption: { option in
                                    focusedQuestion = nil
                                    viewModel.toggleOption(questionId: question.id, option: option)
                                }
                            )
                            .id(question.id)
                        }

                        if showMissing && remaining > 0 {
                            Text(remaining == 1 ? "1 question still needs an answer." : "\(remaining) questions still need an answer.")
                                .font(.system(size: 13))
                                .foregroundColor(surveyRed)
                                .multilineTextAlignment(.center)
                                .frame(maxWidth: .infinity)
                        }

                        if let submitError = viewModel.submitError {
                            Text(submitError)
                                .font(.system(size: 13))
                                .foregroundColor(surveyRed)
                                .padding(12)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color(red: 1.0, green: 0.95, blue: 0.95))
                                .cornerRadius(8)
                        }

                        Button {
                            focusedQuestion = nil
                            if SurveyRules.isComplete(questions, viewModel.answers) {
                                onSubmit()
                            } else {
                                showMissing = true
                                if let first = questions.first(where: { !SurveyRules.isAnswered($0, viewModel.answers[$0.id]) }) {
                                    withAnimation { proxy.scrollTo(first.id, anchor: .top) }
                                }
                            }
                        } label: {
                            Group {
                                if viewModel.isSubmitting {
                                    ProgressView().tint(.white)
                                } else {
                                    Text("Submit Survey")
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(.white)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                        }
                        .background(viewModel.isSubmitting ? Color(white: 0.8) : navyBlue)
                        .cornerRadius(10)
                        .disabled(viewModel.isSubmitting)
                        .padding(.top, 4)
                        .id(Self.submitAnchor)
                    }
                }
                .padding(16)
                .padding(.bottom, 24)
            }
            .scrollDismissesKeyboard(.interactively)
            // After a refused submit, bring the error and the Submit button
            // into view once. Firestore shows the answers as saved for a
            // moment, which usually rebuilds this form scrolled to the top
            // (onAppear); if the form stayed, onChange catches it.
            .onAppear { revealSubmitErrorIfNeeded(proxy) }
            .onChange(of: viewModel.revealSubmitError) { _ in revealSubmitErrorIfNeeded(proxy) }
            .onChange(of: focusedQuestion) { questionId in
                guard let questionId else { return }
                // Once the keyboard has risen, scroll just enough to show the
                // answer box above it (no anchor: the minimum scroll, none at
                // all when it is already visible, so nothing moves under the
                // resident's finger).
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                    withAnimation { proxy.scrollTo(surveyAnswerAnchor(questionId)) }
                }
            }
        }
    }
}

extension SurveyFormView {
    fileprivate func revealSubmitErrorIfNeeded(_ proxy: ScrollViewProxy) {
        guard viewModel.revealSubmitError else { return }
        viewModel.revealSubmitError = false
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            withAnimation { proxy.scrollTo(Self.submitAnchor, anchor: .bottom) }
        }
    }
}

private struct SurveyIntroCard: View {
    let notice: Notice
    let isDeadlinePassed: Bool
    let navyBlue: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if notice.forSelectedResidents {
                SelectedResidentsChip()
            }

            Text(notice.title)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(surveyInk)
                .fixedSize(horizontal: false, vertical: true)

            if !notice.description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(notice.description)
                    .font(.system(size: 14))
                    .foregroundColor(Color(white: 0.33))
                    .lineSpacing(6)
                    .fixedSize(horizontal: false, vertical: true)
            }

            HStack(spacing: 5) {
                Image(systemName: "checklist")
                Text(notice.questions.count == 1 ? "1 question" : "\(notice.questions.count) questions")
            }
            .font(.system(size: 12, weight: .medium))
            .foregroundColor(navyBlue)
            .padding(.top, 4)

            if let deadline = notice.responseDeadline, !isDeadlinePassed {
                HStack(spacing: 5) {
                    Image(systemName: "clock")
                    Text("Please respond by \(SurveyDeadlineFormat.string(from: deadline))")
                }
                .font(.system(size: 12))
                .foregroundColor(Color(white: 0.53))
            }

            if isDeadlinePassed {
                Text("This survey is closed: the response deadline has passed. If the deadline is extended, you can answer it here again.")
                    .font(.system(size: 13))
                    .foregroundColor(surveyRed)
                    .lineSpacing(3)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(red: 1.0, green: 0.95, blue: 0.95))
                    .cornerRadius(8)
                    .padding(.top, 6)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
    }
}

/// "Sent to selected residents": this survey reached only the residents the admin chose.
struct SelectedResidentsChip: View {
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "person.2.fill")
                .font(.system(size: 10))
            Text("Sent to selected residents")
                .font(.system(size: 11, weight: .medium))
        }
        .foregroundColor(Color(red: 0.369, green: 0.208, blue: 0.694))
        .padding(.horizontal, 9)
        .padding(.vertical, 4)
        .background(Color(red: 0.929, green: 0.906, blue: 0.965))
        .clipShape(Capsule())
    }
}

private struct SurveyProgressView: View {
    let answered: Int
    let total: Int
    let navyBlue: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("\(answered) of \(total) answered")
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(Color(white: 0.4))
            ProgressView(value: Double(answered), total: Double(max(total, 1)))
                .tint(navyBlue)
        }
    }
}

private struct SurveyQuestionCard: View {
    let question: SurveyQuestion
    let number: Int
    let total: Int
    let answer: SurveyAnswer?
    let showMissing: Bool
    let enabled: Bool
    let navyBlue: Color
    @Binding var text: String
    var focus: FocusState<String?>.Binding
    let onToggleOption: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("QUESTION \(number) OF \(total)")
                    .font(.system(size: 11, weight: .bold))
                    .kerning(0.5)
                    .foregroundColor(navyBlue)
                Spacer()
                if SurveyRules.isAnswered(question, answer) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 16))
                        .foregroundColor(surveyGreen)
                        .accessibilityLabel("Answered")
                }
            }

            Text(question.text)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(surveyInk)
                .fixedSize(horizontal: false, vertical: true)

            if question.isChoice {
                Text(question.allowsMultiple ? "Select all that apply:" : "Select one:")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(Color(white: 0.53))
                    .padding(.top, 2)

                VStack(spacing: 8) {
                    ForEach(question.options, id: \.self) { option in
                        optionRow(option)
                    }
                }
            } else {
                Text("Your answer:")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(Color(white: 0.53))
                    .padding(.top, 2)

                // The view model caps the text as it is set, so typing or
                // pasting past the limit is trimmed here.
                ZStack(alignment: .topLeading) {
                    TextEditor(text: $text)
                        .font(.system(size: 14))
                        .frame(height: 120)
                        .focused(focus, equals: question.id)
                        .disabled(!enabled)
                        .accessibilityLabel(question.text)
                        .accessibilityHint("Your answer")
                    if text.isEmpty {
                        Text("Type your answer here…")
                            .font(.system(size: 14))
                            .foregroundColor(Color(white: 0.73))
                            .padding(.horizontal, 5)
                            .padding(.vertical, 8)
                            .allowsHitTesting(false)
                    }
                }
                .padding(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color(white: 0.88), lineWidth: 1)
                )
                .cornerRadius(10)
                .id(surveyAnswerAnchor(question.id))

                Text("\(text.utf16.count)/\(QuestionAnswer.maxTextLength)")
                    .font(.system(size: 11))
                    .foregroundColor(text.utf16.count >= QuestionAnswer.maxTextLength ? surveyRed : Color(white: 0.6))
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }

            if showMissing {
                Text("Please answer this question.")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(surveyRed)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(showMissing ? surveyRed : Color.clear, lineWidth: 1.5)
        )
        .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
    }

    /// Radio buttons for one choice, checkboxes for several, so the resident
    /// can tell before tapping whether a second tap adds or replaces.
    private func optionRow(_ option: String) -> some View {
        let selected = answer?.selectedOptions.contains(option) ?? false
        let icon: String
        if question.allowsMultiple {
            icon = selected ? "checkmark.square.fill" : "square"
        } else {
            icon = selected ? "largecircle.fill.circle" : "circle"
        }
        return Button {
            onToggleOption(option)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(selected ? navyBlue : Color(white: 0.62))
                    .accessibilityHidden(true)
                Text(option)
                    .font(.system(size: 14, weight: selected ? .medium : .regular))
                    .foregroundColor(selected ? navyBlue : Color(red: 0.2, green: 0.2, blue: 0.2))
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(selected ? Color(red: 0.933, green: 0.949, blue: 1.0) : Color(red: 0.973, green: 0.976, blue: 0.984))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(selected ? navyBlue : Color(white: 0.88), lineWidth: 1.5)
            )
            .cornerRadius(10)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

// MARK: - Recorded

private struct SurveyRecordedView: View {
    let title: String
    /// Written on this phone, not yet accepted by the server.
    var sending: Bool = false

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            ZStack {
                Circle()
                    .fill(sending ? Color(red: 0.910, green: 0.918, blue: 0.965) : Color(red: 0.910, green: 0.961, blue: 0.914))
                    .frame(width: 88, height: 88)
                Image(systemName: sending ? "icloud.and.arrow.up" : "checkmark.circle.fill")
                    .font(.system(size: sending ? 44 : 52))
                    .foregroundColor(sending ? Color(red: 0.118, green: 0.176, blue: 0.42) : surveyGreen)
                    .accessibilityHidden(true)
            }
            VStack(spacing: 8) {
                Text(sending ? "Sending Your Answers" : "Response Recorded")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(surveyInk)
                Text(sending
                     ? "Your answers to \u{201C}\(title)\u{201D} are saved on this phone and will be sent as soon as it is online."
                     : "Thank you. Your answers to \u{201C}\(title)\u{201D} have been saved.")
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

// MARK: - Deadline format

/// DD-MM-YYYY, as dates are shown across My Nest. POSIX locale so a device
/// set to 24-hour time cannot rewrite the 12-hour pattern.
private enum SurveyDeadlineFormat {
    private static let formatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "dd-MM-yyyy, hh:mm a"
        return formatter
    }()

    static func string(from date: Date) -> String { formatter.string(from: date) }
}
