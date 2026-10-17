package com.humblesolutions.indsphinx.model

/**
 * Notice-board surveys: one notice that asks residents several questions,
 * answered together on one screen and submitted once.
 *
 * The admin portal stores a survey as a `NoticeBoard` document with
 * `type: "survey"`, its introduction in `surveyIntro`, and
 * `questions: [{ id: "q1", text, type, options }]`, 1 to 20 of them. Its
 * `description` holds a fixed "please update the app" message meant for app
 * versions that predate surveys, so this app reads `surveyIntro` instead.
 *
 * A resident's answers go on their response document as
 * `answers: { q1: { selectedOptions, textResponse }, ... }`, keyed by question
 * id so every answer says which question it belongs to.
 *
 * Questions are read the same way the portal reads them
 * (IndSphinx-js/src/lib/survey.js), so residents answer exactly the questions
 * the admin sees results for. The iOS app does not import this module; it
 * mirrors these rules in SurveyModels.swift.
 */
data class SurveyQuestion(
    val id: String,
    val text: String,
    val type: String,
    val options: List<String> = emptyList(),
) {
    val isChoice: Boolean get() = type == Survey.TYPE_SINGLE || type == Survey.TYPE_MULTIPLE
    val allowsMultiple: Boolean get() = type == Survey.TYPE_MULTIPLE
}

/** One answer: [selectedOptions] for a choice question, [textResponse] for free text. */
data class SurveyAnswer(
    val selectedOptions: List<String> = emptyList(),
    val textResponse: String = "",
)

object Survey {
    const val NOTICE_TYPE = "survey"

    /** Where notice-board notices live; every resident can read them. */
    const val NOTICE_BOARD_COLLECTION = "NoticeBoard"

    /**
     * Surveys the admin sent to chosen residents only. A resident can read
     * one only while their uid is in its `audienceUids`, so this app lists
     * them with `whereArrayContains("audienceUids", uid)` and shows them on
     * the notice board with everything else. App versions that predate them
     * never read this collection, so they never see them.
     */
    const val SELECTED_COLLECTION = "TargetedNotices"

    /** The collection a notice lives in. */
    fun collectionFor(forSelectedResidents: Boolean): String =
        if (forSelectedResidents) SELECTED_COLLECTION else NOTICE_BOARD_COLLECTION

    /** Push `noticeType` for a survey sent to selected residents. */
    const val PUSH_NOTICE_TYPE_SELECTED = "selected_survey"

    /**
     * The notice-board features this app supports, reported at sign-in and
     * launch in `Users/{uid}.appNoticeBoardVersion`. 2 means it can show
     * surveys for selected residents; residents below that are asked by push
     * to update the app instead.
     */
    const val APP_NOTICE_BOARD_VERSION = 2

    const val TYPE_TEXT = "text"
    const val TYPE_SINGLE = "single"
    const val TYPE_MULTIPLE = "multiple"

    private const val MIN_OPTIONS = 2

    /**
     * The only question ids a survey can have, q1 to q20. The Firestore rules
     * accept answers only under these keys, so a question with any other id
     * could never be answered.
     */
    private val QUESTION_ID = Regex("^q([1-9]|1[0-9]|20)$")

    /**
     * The stored `questions` value, read defensively. An entry without a
     * valid id (q1 to q20, once each) or text is skipped. An unknown type is
     * answered as free text, and so is a choice question with fewer than two
     * options, since nothing could be chosen. Repeated options are shown once.
     * Never throws.
     */
    fun parseQuestions(raw: Any?): List<SurveyQuestion> {
        val entries = raw as? List<*> ?: return emptyList()
        val seenIds = HashSet<String>()
        return entries.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val id = (map["id"] as? String)?.trim().orEmpty()
            val text = (map["text"] as? String)?.trim().orEmpty()
            if (!QUESTION_ID.matches(id) || id in seenIds || text.isEmpty()) return@mapNotNull null
            seenIds += id

            val options = (map["options"] as? List<*>).orEmpty()
                .mapNotNull { (it as? String)?.trim()?.takeIf(String::isNotEmpty) }
                .distinct()
            val declared = map["type"] as? String
            val type = when {
                (declared == TYPE_SINGLE || declared == TYPE_MULTIPLE) && options.size >= MIN_OPTIONS -> declared
                else -> TYPE_TEXT
            }
            SurveyQuestion(id, text, type, if (type == TYPE_TEXT) emptyList() else options)
        }
    }

    /**
     * [answer] after the resident taps [option]. A single choice moves to the
     * tapped option; a multiple choice adds or removes it. A tap on something
     * that is not one of the question's options changes nothing.
     */
    fun toggle(question: SurveyQuestion, answer: SurveyAnswer?, option: String): SurveyAnswer {
        val current = answer ?: SurveyAnswer()
        if (!question.isChoice || option !in question.options) return current
        val selected = current.selectedOptions
        val next = when {
            !question.allowsMultiple -> listOf(option)
            option in selected -> selected - option
            else -> selected + option
        }
        return current.copy(selectedOptions = next)
    }

    fun isAnswered(question: SurveyQuestion, answer: SurveyAnswer?): Boolean {
        if (answer == null) return false
        return if (question.isChoice) {
            answer.selectedOptions.any { it in question.options }
        } else {
            answer.textResponse.isNotBlank()
        }
    }

    fun answeredCount(questions: List<SurveyQuestion>, answers: Map<String, SurveyAnswer>): Int =
        questions.count { isAnswered(it, answers[it.id]) }

    /** Every question is required. A survey with no questions is never complete. */
    fun isComplete(questions: List<SurveyQuestion>, answers: Map<String, SurveyAnswer>): Boolean =
        questions.isNotEmpty() && questions.all { isAnswered(it, answers[it.id]) }

    /**
     * The answers exactly as they are written: one entry per question, in
     * question order, and nothing for questions the survey does not have. A
     * choice keeps only the question's own options, in the question's order,
     * and a single choice keeps one. A typed answer is trimmed and capped at
     * [QuestionAnswer.MAX_TEXT_LENGTH], the same limit as a single question.
     */
    fun sanitize(
        questions: List<SurveyQuestion>,
        answers: Map<String, SurveyAnswer>,
    ): Map<String, SurveyAnswer> {
        val out = LinkedHashMap<String, SurveyAnswer>()
        for (question in questions) {
            val answer = answers[question.id] ?: SurveyAnswer()
            out[question.id] = if (question.isChoice) {
                val picked = question.options.filter { it in answer.selectedOptions }
                SurveyAnswer(selectedOptions = if (question.allowsMultiple) picked else picked.take(1))
            } else {
                SurveyAnswer(textResponse = QuestionAnswer.cap(answer.textResponse.trim()))
            }
        }
        return out
    }

    /** [answers] in the shape stored on the response document. */
    fun toStoredAnswers(answers: Map<String, SurveyAnswer>): Map<String, Map<String, Any>> =
        answers.mapValues { (_, answer) ->
            mapOf(
                "selectedOptions" to answer.selectedOptions,
                "textResponse" to answer.textResponse,
            )
        }
}
