package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.model.Survey
import com.humblesolutions.indsphinx.model.SurveyAnswer
import com.humblesolutions.indsphinx.repository.NoticeboardRepository

class SubmitSurveyResponseUseCase(
    private val noticeboardRepository: NoticeboardRepository
) {
    /**
     * Writes the resident's answers to [survey]. They are first cleaned
     * against the survey's own questions (see [Survey.sanitize]), and an
     * incomplete survey is refused: the screen only enables Submit once every
     * question is answered, so this guards any other path.
     */
    suspend fun execute(
        survey: Notice,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        answers: Map<String, SurveyAnswer>
    ) {
        val clean = Survey.sanitize(survey.questions, answers)
        require(Survey.isComplete(survey.questions, clean)) { "Please answer every question." }
        noticeboardRepository.submitSurveyResponse(
            survey.id, uid, displayName, flatNo, recipientType, clean,
            forSelectedResidents = survey.forSelectedResidents,
        )
    }
}
