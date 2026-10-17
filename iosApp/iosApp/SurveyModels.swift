import Foundation

/// Notice-board surveys: one notice that asks residents several questions,
/// answered together on one screen and submitted once.
///
/// The admin portal stores a survey as a `NoticeBoard` document with
/// `type: "survey"`, its introduction in `surveyIntro`, and
/// `questions: [{ id: "q1", text, type, options }]`, 1 to 20 of them. Its
/// `description` holds a fixed "please update the app" message meant for app
/// versions that predate surveys, so this app reads `surveyIntro` instead.
///
/// A resident's answers go on their response document as
/// `answers: { q1: { selectedOptions, textResponse }, ... }`, keyed by
/// question id so every answer says which question it belongs to.
///
/// Mirrors `Survey` in the shared Kotlin module, which this app does not
/// import, and reads questions the way the admin portal does, so residents
/// answer exactly the questions the admin sees results for.
struct SurveyQuestion: Identifiable, Hashable {
    let id: String
    let text: String
    let type: String
    let options: [String]

    var isChoice: Bool { type == SurveyRules.typeSingle || type == SurveyRules.typeMultiple }
    var allowsMultiple: Bool { type == SurveyRules.typeMultiple }
}

/// One answer: `selectedOptions` for a choice question, `textResponse` for free text.
struct SurveyAnswer: Hashable {
    var selectedOptions: [String] = []
    var textResponse: String = ""
}

enum SurveyRules {
    static let noticeType = "survey"

    /// Where notice-board notices live; every resident can read them.
    static let noticeBoardCollection = "NoticeBoard"

    /// Surveys the admin sent to chosen residents only. A resident can read
    /// one only while their uid is in its `audienceUids`, so the app lists
    /// them with `whereField("audienceUids", arrayContains: uid)` and shows
    /// them on the notice board with everything else. Mirrors Survey.kt.
    static let selectedCollection = "TargetedNotices"

    /// The collection a notice lives in.
    static func collection(forSelected: Bool) -> String {
        forSelected ? selectedCollection : noticeBoardCollection
    }

    /// Push `noticeType` for a survey sent to selected residents.
    static let pushNoticeTypeSelected = "selected_survey"

    /// The notice-board features this app supports, reported at sign-in and
    /// launch in `Users/{uid}.appNoticeBoardVersion`. 2 means it can show
    /// surveys for selected residents; residents below that are asked by
    /// push to update the app instead.
    static let appNoticeBoardVersion = 2

    static let typeText = "text"
    static let typeSingle = "single"
    static let typeMultiple = "multiple"

    private static let minOptions = 2

    /// The only question ids a survey can have: q1 to q20, as the portal
    /// assigns them. The Firestore rules accept answers only under these keys.
    static func isValidQuestionId(_ id: String) -> Bool {
        guard id.hasPrefix("q"), let number = Int(id.dropFirst()), (1...20).contains(number) else { return false }
        return id == "q\(number)"
    }

    /// The stored `questions` value, read defensively. An entry without a
    /// valid id (q1 to q20, once each) or text is skipped. An unknown type is
    /// answered as free text, and so is a choice question with fewer than two
    /// options, since nothing could be chosen. Repeated options are shown once.
    static func parseQuestions(_ raw: Any?) -> [SurveyQuestion] {
        guard let entries = raw as? [Any] else { return [] }
        var seenIds = Set<String>()
        return entries.compactMap { entry in
            guard let map = entry as? [String: Any] else { return nil }
            let id = (map["id"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let text = (map["text"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            guard isValidQuestionId(id), !seenIds.contains(id), !text.isEmpty else { return nil }
            seenIds.insert(id)

            var options: [String] = []
            for value in (map["options"] as? [Any]) ?? [] {
                guard let option = (value as? String)?.trimmingCharacters(in: .whitespacesAndNewlines),
                      !option.isEmpty, !options.contains(option) else { continue }
                options.append(option)
            }
            let declared = map["type"] as? String
            let isChoice = (declared == typeSingle || declared == typeMultiple) && options.count >= minOptions
            let type = isChoice ? declared! : typeText
            return SurveyQuestion(id: id, text: text, type: type, options: isChoice ? options : [])
        }
    }

    /// `answer` after the resident taps `option`. A single choice moves to the
    /// tapped option; a multiple choice adds or removes it. A tap on something
    /// that is not one of the question's options changes nothing.
    static func toggle(_ question: SurveyQuestion, _ answer: SurveyAnswer?, _ option: String) -> SurveyAnswer {
        var next = answer ?? SurveyAnswer()
        guard question.isChoice, question.options.contains(option) else { return next }
        if !question.allowsMultiple {
            next.selectedOptions = [option]
        } else if let index = next.selectedOptions.firstIndex(of: option) {
            next.selectedOptions.remove(at: index)
        } else {
            next.selectedOptions.append(option)
        }
        return next
    }

    static func isAnswered(_ question: SurveyQuestion, _ answer: SurveyAnswer?) -> Bool {
        guard let answer else { return false }
        if question.isChoice {
            return answer.selectedOptions.contains { question.options.contains($0) }
        }
        return !answer.textResponse.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    static func answeredCount(_ questions: [SurveyQuestion], _ answers: [String: SurveyAnswer]) -> Int {
        questions.filter { isAnswered($0, answers[$0.id]) }.count
    }

    /// Every question is required. A survey with no questions is never complete.
    static func isComplete(_ questions: [SurveyQuestion], _ answers: [String: SurveyAnswer]) -> Bool {
        !questions.isEmpty && questions.allSatisfy { isAnswered($0, answers[$0.id]) }
    }

    /// The answers exactly as they are written: one entry per question and
    /// nothing for questions the survey does not have. A choice keeps only the
    /// question's own options, in the question's order, and a single choice
    /// keeps one. A typed answer is trimmed and capped at the same limit as a
    /// single question.
    static func sanitize(_ questions: [SurveyQuestion], _ answers: [String: SurveyAnswer]) -> [String: SurveyAnswer] {
        var out: [String: SurveyAnswer] = [:]
        for question in questions {
            let answer = answers[question.id] ?? SurveyAnswer()
            if question.isChoice {
                let picked = question.options.filter { answer.selectedOptions.contains($0) }
                out[question.id] = SurveyAnswer(selectedOptions: question.allowsMultiple ? picked : Array(picked.prefix(1)))
            } else {
                let typed = answer.textResponse.trimmingCharacters(in: .whitespacesAndNewlines)
                out[question.id] = SurveyAnswer(textResponse: QuestionAnswer.cap(typed))
            }
        }
        return out
    }

    /// `answers` in the shape stored on the response document.
    static func storedAnswers(_ answers: [String: SurveyAnswer]) -> [String: [String: Any]] {
        answers.mapValues { ["selectedOptions": $0.selectedOptions, "textResponse": $0.textResponse] }
    }
}
