import SwiftUI

struct NoticeboardView: View {
    let onMenuTap: () -> Void
    var initialNotice: Notice? = nil
    var displayName: String = ""
    var flatNo: String = ""
    var recipientType: String = ""

    @StateObject private var viewModel = NoticeboardViewModel()

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let backgroundGray = Color(red: 0.949, green: 0.957, blue: 0.973)

    var body: some View {
        VStack(spacing: 0) {
            if let notice = viewModel.questionNotice {
                NoticeQuestionView(
                    noticeId: notice.id,
                    displayName: displayName,
                    flatNo: flatNo,
                    recipientType: recipientType,
                    onBack: { viewModel.onBackFromDetail() }
                )
            } else if viewModel.selectedNotice != nil {
                NoticeDetailHeaderView(
                    navyBlue: navyBlue,
                    onBack: { viewModel.onBackFromDetail() }
                )
                if let notice = viewModel.selectedNotice {
                    NoticeDetailContentView(notice: notice, navyBlue: navyBlue, backgroundGray: backgroundGray)
                }
            } else {
                NoticeboardHeaderView(
                    navyBlue: navyBlue,
                    onMenuTap: onMenuTap
                )
                NoticeListContentView(
                    notices: viewModel.notices,
                    navyBlue: navyBlue,
                    backgroundGray: backgroundGray,
                    onNoticeSelect: { viewModel.onNoticeSelected($0) }
                )
            }
        }
        .background(backgroundGray)
        .onChange(of: initialNotice) { notice in
            guard let notice else { return }
            viewModel.openNoticeDirectly(notice)
        }
    }
}

// MARK: - List Header

private struct NoticeboardHeaderView: View {
    let navyBlue: Color
    let onMenuTap: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            Button(action: onMenuTap) {
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: 20))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
            }
            Spacer().frame(width: 12)
            Text("Noticeboard")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
            Spacer()
            Color.clear.frame(width: 40, height: 40)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(navyBlue.ignoresSafeArea(edges: .top))
    }
}

// MARK: - Detail Header

private struct NoticeDetailHeaderView: View {
    let navyBlue: Color
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 16) {
            Button(action: onBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 20))
                    .foregroundColor(.white)
            }
            Text("Notice Details")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
            Spacer()
            Image(systemName: "pin")
                .font(.system(size: 18))
                .foregroundColor(.white)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
        .background(navyBlue)
    }
}

// MARK: - List Content

private struct NoticeListContentView: View {
    let notices: [Notice]
    let navyBlue: Color
    let backgroundGray: Color
    let onNoticeSelect: (Notice) -> Void

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                Text("All Notices")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                    .padding(.horizontal, 16)
                    .padding(.top, 20)
                    .padding(.bottom, 12)

                if notices.isEmpty {
                    Text("No notices yet")
                        .font(.system(size: 14))
                        .foregroundColor(Color(white: 0.6))
                        .frame(maxWidth: .infinity)
                        .padding(.top, 48)
                } else {
                    VStack(spacing: 12) {
                        ForEach(notices) { notice in
                            NoticeCardView(
                                notice: notice,
                                navyBlue: navyBlue,
                                onTap: { onNoticeSelect(notice) }
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                }
            }
            .padding(.bottom, 24)
        }
        .background(backgroundGray)
    }
}

private struct NoticeCardView: View {
    let notice: Notice
    let navyBlue: Color
    let onTap: () -> Void

    private var isQuestion: Bool { notice.type == "question" }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .top) {
                    Text(notice.title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        .multilineTextAlignment(.leading)
                    Spacer()
                    if isQuestion {
                        HStack(spacing: 3) {
                            Image(systemName: "questionmark.circle")
                                .font(.system(size: 11))
                                .foregroundColor(Color(red: 0.902, green: 0.318, blue: 0.0))
                            Text("Question")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(Color(red: 0.902, green: 0.318, blue: 0.0))
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color(red: 1.0, green: 0.953, blue: 0.878))
                        .cornerRadius(20)
                    }
                }

                HStack(spacing: 4) {
                    Image(systemName: "calendar")
                        .font(.system(size: 12))
                        .foregroundColor(Color(white: 0.6))
                    Text(notice.publishedAt.formatted())
                        .font(.system(size: 12))
                        .foregroundColor(Color(white: 0.6))
                }

                Text(notice.description)
                    .font(.system(size: 13))
                    .foregroundColor(Color(white: 0.33))
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)

                HStack(spacing: 2) {
                    Text(isQuestion ? "Respond" : "View Details")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(navyBlue)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12))
                        .foregroundColor(navyBlue)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 1)
        }
    }
}

// MARK: - Detail Content

private struct NoticeDetailContentView: View {
    let notice: Notice
    let navyBlue: Color
    let backgroundGray: Color

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                VStack(alignment: .leading, spacing: 12) {
                    Text(notice.title)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))

                    HStack(spacing: 10) {
                        HStack(spacing: 6) {
                            Image(systemName: "calendar")
                                .font(.system(size: 13))
                                .foregroundColor(Color(white: 0.6))
                            Text(notice.publishedAt.formatted())
                                .font(.system(size: 13))
                                .foregroundColor(Color(white: 0.6))
                        }
                        Text("Notice")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(navyBlue)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color(red: 0.933, green: 0.949, blue: 1.0))
                            .cornerRadius(20)
                    }

                    Divider()

                    Text(notice.description)
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

// MARK: - Date formatter

private extension Date {
    func formatted() -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "MMM d, yyyy"
        return fmt.string(from: self)
    }
}
