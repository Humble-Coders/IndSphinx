package com.humblesolutions.indsphinx.model

data class Notice(
    val id: String = "",
    val title: String = "",
    /** For a survey, its introduction (`surveyIntro`); see [Survey]. */
    val description: String = "",
    val publishedAt: Long = 0L,
    val type: String = "normal",
    val options: List<String> = emptyList(),
    val allowMultipleChoices: Boolean = false,
    val responseDeadline: Long? = null,
    val responseCount: Int = 0,
    /** A survey's questions; empty for every other type. */
    val questions: List<SurveyQuestion> = emptyList(),
    /**
     * True for a survey the admin sent only to chosen residents. It lives in
     * [Survey.SELECTED_COLLECTION] instead of the notice board, and every
     * read and write for it goes there.
     */
    val forSelectedResidents: Boolean = false
) {
    val isSurvey: Boolean get() = type == Survey.NOTICE_TYPE
}
