package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.model.ResponseState
import com.humblesolutions.indsphinx.model.Survey
import com.humblesolutions.indsphinx.model.SurveyAnswer
import com.humblesolutions.indsphinx.model.SurveyQuestion
import com.humblesolutions.indsphinx.repository.NoticeboardRepository
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class SubmitSurveyResponseUseCaseTest {

    private val survey = Notice(
        id = "survey1",
        title = "Amenities",
        type = Survey.NOTICE_TYPE,
        questions = listOf(
            SurveyQuestion("q1", "Gym?", Survey.TYPE_SINGLE, listOf("Yes", "No")),
            SurveyQuestion("q2", "Which?", Survey.TYPE_MULTIPLE, listOf("A", "B", "C")),
            SurveyQuestion("q3", "Notes", Survey.TYPE_TEXT),
        ),
    )

    @Test
    fun anIncompleteSurveyIsRefusedAndNothingIsWritten() {
        val repo = FakeRepository()
        val useCase = SubmitSurveyResponseUseCase(repo)
        assertFailsWith<IllegalArgumentException> {
            runSync {
                useCase.execute(survey, "uid1", "Resident", "A101", "OCCUPANT", mapOf("q1" to SurveyAnswer(listOf("Yes"))))
            }
        }
        assertNull(repo.written)
    }

    @Test
    fun theCleanedAnswersAreWrittenForTheRightSurveyAndResident() {
        val repo = FakeRepository()
        runSync {
            SubmitSurveyResponseUseCase(repo).execute(
                survey, "uid1", "Resident", "A101", "OCCUPANT",
                mapOf(
                    "q1" to SurveyAnswer(listOf("No", "Yes")),
                    "q2" to SurveyAnswer(listOf("C", "Z", "A")),
                    "q3" to SurveyAnswer(textResponse = "  fine  "),
                    "q9" to SurveyAnswer(textResponse = "not a question here"),
                ),
            )
        }
        assertEquals("survey1", repo.noticeId)
        assertEquals("uid1", repo.uid)
        assertEquals(
            mapOf(
                "q1" to SurveyAnswer(listOf("Yes")),
                "q2" to SurveyAnswer(listOf("A", "C")),
                "q3" to SurveyAnswer(textResponse = "fine"),
            ),
            repo.written,
        )
    }

    @Test
    fun completenessIsTheSameBeforeAndAfterCleaning() {
        // The screen checks completeness on the raw answers, the use case on
        // the cleaned ones; they must agree or a resident could be stuck.
        val cases = listOf(
            mapOf("q1" to SurveyAnswer(listOf("Yes")), "q2" to SurveyAnswer(listOf("B")), "q3" to SurveyAnswer(textResponse = "x")),
            mapOf("q1" to SurveyAnswer(listOf("Maybe")), "q2" to SurveyAnswer(listOf("B")), "q3" to SurveyAnswer(textResponse = "x")),
            mapOf("q1" to SurveyAnswer(listOf("Yes")), "q2" to SurveyAnswer(listOf("B")), "q3" to SurveyAnswer(textResponse = "   ")),
            emptyMap(),
        )
        for (answers in cases) {
            assertEquals(
                Survey.isComplete(survey.questions, answers),
                Survey.isComplete(survey.questions, Survey.sanitize(survey.questions, answers)),
            )
        }
    }

    @Test
    fun aSurveyForSelectedResidentsIsAnsweredInItsOwnCollection() {
        val repo = FakeRepository()
        val complete = mapOf(
            "q1" to SurveyAnswer(listOf("Yes")),
            "q2" to SurveyAnswer(listOf("B")),
            "q3" to SurveyAnswer(textResponse = "x"),
        )
        runSync { SubmitSurveyResponseUseCase(repo).execute(survey, "uid1", "R", "A101", "OCCUPANT", complete) }
        assertEquals(false, repo.forSelected)
        runSync {
            SubmitSurveyResponseUseCase(repo).execute(
                survey.copy(forSelectedResidents = true), "uid1", "R", "A101", "OCCUPANT", complete,
            )
        }
        assertEquals(true, repo.forSelected)
        assertEquals("TargetedNotices", Survey.collectionFor(true))
        assertEquals("NoticeBoard", Survey.collectionFor(false))
    }

    private class FakeRepository : NoticeboardRepository {
        var noticeId: String? = null
        var uid: String? = null
        var written: Map<String, SurveyAnswer>? = null
        var forSelected: Boolean? = null

        override fun observeNotices(): Flow<List<Notice>> = emptyFlow()
        override suspend fun fetchNotice(noticeId: String, forSelectedResidents: Boolean): Notice? = null
        override fun observeNotice(noticeId: String, forSelectedResidents: Boolean): Flow<Notice?> = emptyFlow()
        override fun observeResponseExists(noticeId: String, uid: String, forSelectedResidents: Boolean): Flow<Boolean> = emptyFlow()
        override fun observeResponseState(noticeId: String, uid: String, forSelectedResidents: Boolean): Flow<ResponseState> = emptyFlow()
        override suspend fun submitResponse(
            noticeId: String, uid: String, displayName: String, flatNo: String,
            recipientType: String, selectedOptions: List<String>, textResponse: String,
        ) = Unit

        override suspend fun submitSurveyResponse(
            noticeId: String, uid: String, displayName: String, flatNo: String,
            recipientType: String, answers: Map<String, SurveyAnswer>, forSelectedResidents: Boolean,
        ) {
            this.noticeId = noticeId
            this.uid = uid
            this.written = answers
            this.forSelected = forSelectedResidents
        }
    }
}

/** Runs a suspend block that never actually suspends (the fake above), in common code. */
private fun <T> runSync(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
    return outcome!!.getOrThrow()
}
