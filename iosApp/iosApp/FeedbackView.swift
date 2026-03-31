import SwiftUI

struct FeedbackView: View {
    let occupantId: String
    let occupantName: String
    let onBack: () -> Void

    @StateObject private var viewModel = FeedbackViewModel()

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let backgroundGray = Color(red: 0.949, green: 0.957, blue: 0.973)

    var body: some View {
        VStack(spacing: 0) {
            switch viewModel.state {
            case .loading:
                FBHeader(title: "Feedback", navyBlue: navyBlue, onBack: onBack, showIcon: true)
                Spacer()
                ProgressView().tint(navyBlue)
                Spacer()

            case .loaded(let feedbacks):
                FBHeader(title: "Feedback", navyBlue: navyBlue, onBack: onBack, showIcon: true)
                FeedbackListView(
                    feedbacks: feedbacks,
                    navyBlue: navyBlue,
                    backgroundGray: backgroundGray,
                    onSubmitTapped: { viewModel.onSubmitTapped() },
                    onFeedbackSelected: { viewModel.onFeedbackSelected($0) }
                )

            case .submitForm:
                FBHeader(title: "Submit Feedback", navyBlue: navyBlue, onBack: { viewModel.onBackFromForm() })
                FeedbackFormView(
                    navyBlue: navyBlue,
                    backgroundGray: backgroundGray,
                    isSubmitting: false,
                    onSubmit: { title, desc in
                        viewModel.submit(occupantId: occupantId, occupantName: occupantName, title: title, description: desc)
                    }
                )

            case .submitting:
                FBHeader(title: "Submit Feedback", navyBlue: navyBlue, onBack: {})
                Spacer()
                ProgressView().tint(navyBlue)
                Spacer()

            case .detail(let feedback, _):
                FBHeader(title: "Feedback Details", navyBlue: navyBlue, onBack: { viewModel.onBackFromDetail() })
                FeedbackDetailView(feedback: feedback, navyBlue: navyBlue, backgroundGray: backgroundGray)

            case .error(let msg, _):
                FBHeader(title: "Feedback", navyBlue: navyBlue, onBack: { viewModel.dismissError() })
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

private struct FBHeader: View {
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
                    Image(systemName: "bubble.left")
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

private struct FeedbackListView: View {
    let feedbacks: [FeedbackItem]
    let navyBlue: Color
    let backgroundGray: Color
    let onSubmitTapped: () -> Void
    let onFeedbackSelected: (FeedbackItem) -> Void

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                // Subtitle
                Text("Share suggestions or ideas to improve residential facilities.")
                    .font(.system(size: 14))
                    .foregroundColor(Color(white: 0.4))
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 24)
                    .padding(.top, 20)

                // Submit button
                Button(action: onSubmitTapped) {
                    HStack(spacing: 8) {
                        Image(systemName: "plus")
                            .font(.system(size: 16, weight: .semibold))
                        Text("Submit Feedback")
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

                if !feedbacks.isEmpty {
                    Text("Previously Submitted")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        .padding(.horizontal, 16)
                        .padding(.top, 24)
                        .padding(.bottom, 12)

                    VStack(spacing: 12) {
                        ForEach(feedbacks) { fb in
                            FeedbackCardView(
                                feedback: fb,
                                navyBlue: navyBlue,
                                backgroundGray: backgroundGray,
                                onTap: { onFeedbackSelected(fb) }
                            )
                            .padding(.horizontal, 16)
                        }
                    }
                }
            }
            .padding(.bottom, 24)
        }
        .background(backgroundGray)
    }
}

private struct FeedbackCardView: View {
    let feedback: FeedbackItem
    let navyBlue: Color
    let backgroundGray: Color
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 4) {
                    Image(systemName: "calendar")
                        .font(.system(size: 11))
                        .foregroundColor(Color(white: 0.6))
                    Text(feedback.date.fbFormatted())
                        .font(.system(size: 12))
                        .foregroundColor(Color(white: 0.6))
                }
                Text(feedback.title)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                Text(feedback.description)
                    .font(.system(size: 13))
                    .foregroundColor(Color(white: 0.33))
                    .lineLimit(3)
            }
            .padding(16)

            Button(action: onTap) {
                Text("View Details")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(navyBlue)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(backgroundGray)
            }
        }
        .background(Color.white)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
    }
}

// MARK: - Form

private struct FeedbackFormView: View {
    let navyBlue: Color
    let backgroundGray: Color
    let isSubmitting: Bool
    let onSubmit: (String, String) -> Void

    @State private var title = ""
    @State private var description = ""

    private var isValid: Bool {
        !title.trimmingCharacters(in: .whitespaces).isEmpty &&
        !description.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 20) {
                    // Title
                    FBFormField(
                        label: "Title",
                        icon: "text.cursor",
                        placeholder: "Enter feedback title",
                        text: $title,
                        navyBlue: navyBlue
                    )
                    // Description
                    FBFormField(
                        label: "Description",
                        icon: "doc.text",
                        placeholder: "Describe your feedback in detail",
                        text: $description,
                        navyBlue: navyBlue,
                        multiline: true
                    )
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
                            title.trimmingCharacters(in: .whitespaces),
                            description.trimmingCharacters(in: .whitespaces)
                        )
                    }
                }) {
                    Group {
                        if isSubmitting {
                            ProgressView().tint(.white)
                        } else {
                            Text("Submit Feedback")
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

private struct FBFormField: View {
    let label: String
    let icon: String
    let placeholder: String
    @Binding var text: String
    let navyBlue: Color
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
                        .frame(minHeight: 120)
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
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .background(Color(red: 0.949, green: 0.957, blue: 0.973))
                    .cornerRadius(10)
            }
        }
    }
}

// MARK: - Detail

private struct FeedbackDetailView: View {
    let feedback: FeedbackItem
    let navyBlue: Color
    let backgroundGray: Color

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 4) {
                        Image(systemName: "calendar")
                            .font(.system(size: 12))
                            .foregroundColor(Color(white: 0.6))
                        Text(feedback.date.fbFormatted())
                            .font(.system(size: 13))
                            .foregroundColor(Color(white: 0.6))
                    }
                    Text(feedback.title)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                    Divider()
                    Text(feedback.description)
                        .font(.system(size: 14))
                        .foregroundColor(Color(red: 0.2, green: 0.2, blue: 0.2))
                        .lineSpacing(6)
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

// MARK: - Date helper

private extension Date {
    func fbFormatted() -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "MMM d, yyyy"
        return fmt.string(from: self)
    }
}
