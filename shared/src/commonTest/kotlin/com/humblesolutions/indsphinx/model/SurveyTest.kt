package com.humblesolutions.indsphinx.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SurveyTest {

    private val single = SurveyQuestion("q1", "Rate the gym", Survey.TYPE_SINGLE, listOf("Good", "Okay", "Poor"))
    private val multiple = SurveyQuestion("q2", "Which facilities do you use?", Survey.TYPE_MULTIPLE, listOf("Gym", "Pool", "Park"))
    private val text = SurveyQuestion("q3", "Anything else?", Survey.TYPE_TEXT)
    private val questions = listOf(single, multiple, text)

    // ── Reading what the portal stores ──────────────────────────────────────

    @Test
    fun parsesEveryTypeAsStored() {
        val parsed = Survey.parseQuestions(
            listOf(
                mapOf("id" to "q1", "text" to "Rate the gym", "type" to "single", "options" to listOf("Good", "Okay", "Poor")),
                mapOf("id" to "q2", "text" to "Which facilities do you use?", "type" to "multiple", "options" to listOf("Gym", "Pool", "Park")),
                mapOf("id" to "q3", "text" to "Anything else?", "type" to "text", "options" to emptyList<String>()),
            )
        )
        assertEquals(questions, parsed)
    }

    @Test
    fun skipsEntriesWithoutIdOrText() {
        val parsed = Survey.parseQuestions(
            listOf(
                mapOf("id" to "", "text" to "No id", "type" to "text"),
                mapOf("id" to "q2", "text" to "   ", "type" to "text"),
                mapOf("text" to "Missing id"),
                "not a map",
                null,
                mapOf("id" to " q5 ", "text" to " Kept ", "type" to "text"),
            )
        )
        assertEquals(listOf(SurveyQuestion("q5", "Kept", Survey.TYPE_TEXT)), parsed)
    }

    @Test
    fun unknownTypesAndChoicesWithTooFewOptionsBecomeFreeText() {
        val parsed = Survey.parseQuestions(
            listOf(
                mapOf("id" to "q1", "text" to "Rating", "type" to "rating", "options" to listOf("1", "2")),
                mapOf("id" to "q2", "text" to "One option", "type" to "single", "options" to listOf("Only")),
                mapOf("id" to "q3", "text" to "Blank options", "type" to "multiple", "options" to listOf(" ", "", 7)),
                mapOf("id" to "q4", "text" to "No type"),
            )
        )
        assertTrue(parsed.all { it.type == Survey.TYPE_TEXT && it.options.isEmpty() })
        assertEquals(4, parsed.size)
    }

    @Test
    fun optionsAreTrimmedAndShownOnce() {
        val parsed = Survey.parseQuestions(
            listOf(mapOf("id" to "q1", "text" to "Pick", "type" to "single", "options" to listOf(" Yes ", "No", "Yes")))
        )
        assertEquals(listOf("Yes", "No"), parsed.single().options)
    }

    @Test
    fun onlyIdsQ1ToQ20AreReadAndEachOnlyOnce() {
        fun q(id: Any?, text: String = "Text") = mapOf("id" to id, "text" to text, "type" to "text")
        val parsed = Survey.parseQuestions(
            listOf(
                q("q0"), q("q21"), q("Q1"), q("x"), q(7), q("q1 x"),
                q("q2", text = " "),          // no text: skipped, does not use up the id
                q("q2", text = "Second"),
                q("q2", text = "Repeat"),     // a repeated id is skipped
                q(" q20 ", text = "Last"),
            )
        )
        assertEquals(listOf("q2" to "Second", "q20" to "Last"), parsed.map { it.id to it.text })
    }

    @Test
    fun garbageNeverThrows() {
        for (raw in listOf(null, "questions", 42, emptyMap<String, Any>(), listOf(listOf(1, 2)))) {
            assertEquals(emptyList(), Survey.parseQuestions(raw))
        }
    }

    // ── Answering ────────────────────────────────────────────────────────────

    @Test
    fun singleChoiceMovesToTheTappedOption() {
        val first = Survey.toggle(single, null, "Good")
        assertEquals(listOf("Good"), first.selectedOptions)
        assertEquals(listOf("Poor"), Survey.toggle(single, first, "Poor").selectedOptions)
        assertEquals(listOf("Poor"), Survey.toggle(single, SurveyAnswer(listOf("Poor")), "Poor").selectedOptions)
    }

    @Test
    fun multipleChoiceAddsAndRemoves() {
        var answer = Survey.toggle(multiple, null, "Pool")
        answer = Survey.toggle(multiple, answer, "Gym")
        assertEquals(listOf("Pool", "Gym"), answer.selectedOptions)
        answer = Survey.toggle(multiple, answer, "Pool")
        assertEquals(listOf("Gym"), answer.selectedOptions)
    }

    @Test
    fun tappingSomethingThatIsNotAnOptionChangesNothing() {
        val answer = SurveyAnswer(listOf("Gym"))
        assertEquals(answer, Survey.toggle(multiple, answer, "Spa"))
        assertEquals(SurveyAnswer(), Survey.toggle(text, null, "Gym"))
    }

    @Test
    fun answeredMeansAValidChoiceOrNonBlankText() {
        assertFalse(Survey.isAnswered(single, null))
        assertFalse(Survey.isAnswered(single, SurveyAnswer(listOf("Spa"))))
        assertTrue(Survey.isAnswered(single, SurveyAnswer(listOf("Okay"))))
        assertFalse(Survey.isAnswered(text, SurveyAnswer(textResponse = " \n ")))
        assertTrue(Survey.isAnswered(text, SurveyAnswer(textResponse = "Fine")))
        // A typed answer does not answer a choice question, nor a choice a text one.
        assertFalse(Survey.isAnswered(single, SurveyAnswer(textResponse = "Good")))
        assertFalse(Survey.isAnswered(text, SurveyAnswer(listOf("Good"))))
    }

    @Test
    fun everyQuestionIsRequired() {
        val partial = mapOf("q1" to SurveyAnswer(listOf("Good")), "q2" to SurveyAnswer(listOf("Gym")))
        assertEquals(2, Survey.answeredCount(questions, partial))
        assertFalse(Survey.isComplete(questions, partial))
        val full = partial + ("q3" to SurveyAnswer(textResponse = "No"))
        assertEquals(3, Survey.answeredCount(questions, full))
        assertTrue(Survey.isComplete(questions, full))
    }

    @Test
    fun aSurveyWithoutQuestionsIsNeverComplete() {
        assertFalse(Survey.isComplete(emptyList(), emptyMap()))
    }

    // ── What is written ──────────────────────────────────────────────────────

    @Test
    fun sanitizeKeepsOnlyRealOptionsInQuestionOrder() {
        val clean = Survey.sanitize(
            questions,
            mapOf(
                "q1" to SurveyAnswer(listOf("Poor", "Good"), textResponse = "stray"),
                "q2" to SurveyAnswer(listOf("Park", "Spa", "Gym", "Gym")),
                "q3" to SurveyAnswer(listOf("Gym"), textResponse = "  All good  "),
                "q9" to SurveyAnswer(textResponse = "not in the survey"),
            )
        )
        assertEquals(listOf("q1", "q2", "q3"), clean.keys.toList())
        assertEquals(SurveyAnswer(listOf("Good")), clean["q1"])
        assertEquals(SurveyAnswer(listOf("Gym", "Park")), clean["q2"])
        assertEquals(SurveyAnswer(textResponse = "All good"), clean["q3"])
    }

    @Test
    fun sanitizeCapsTypedAnswersAtTheQuestionLimit() {
        val long = "a".repeat(QuestionAnswer.MAX_TEXT_LENGTH + 40)
        val clean = Survey.sanitize(listOf(text), mapOf("q3" to SurveyAnswer(textResponse = long)))
        assertEquals(QuestionAnswer.MAX_TEXT_LENGTH, clean.getValue("q3").textResponse.length)
    }

    @Test
    fun storedShapeMatchesWhatTheRulesAndPortalRead() {
        val stored = Survey.toStoredAnswers(
            mapOf("q1" to SurveyAnswer(listOf("Good")), "q3" to SurveyAnswer(textResponse = "Hi"))
        )
        assertEquals(
            mapOf(
                "q1" to mapOf("selectedOptions" to listOf("Good"), "textResponse" to ""),
                "q3" to mapOf("selectedOptions" to emptyList<String>(), "textResponse" to "Hi"),
            ),
            stored
        )
    }
}
