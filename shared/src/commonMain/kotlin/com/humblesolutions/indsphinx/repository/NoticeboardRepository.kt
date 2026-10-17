package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.model.ResponseState
import com.humblesolutions.indsphinx.model.SurveyAnswer
import kotlinx.coroutines.flow.Flow

/**
 * Notices for the signed-in resident: the notice board, plus the surveys the
 * admin sent to them among selected residents. `forSelectedResidents` says
 * which collection a notice lives in (see [com.humblesolutions.indsphinx.model.Survey.collectionFor]).
 */
interface NoticeboardRepository {
    /** Everything this resident can see, newest first. */
    fun observeNotices(): Flow<List<Notice>>
    suspend fun fetchNotice(noticeId: String, forSelectedResidents: Boolean = false): Notice?
    /**
     * One notice, live. Emits null once it is deleted, or when this resident
     * can no longer read it. Emits nothing while it is only known from the
     * phone's cache that the notice is not there (offline, not fetched yet):
     * that is "not known yet", not "deleted". A survey for selected
     * residents is only reported once the server has confirmed it, so a
     * copy cached for someone else signed in on this phone never shows.
     */
    fun observeNotice(noticeId: String, forSelectedResidents: Boolean = false): Flow<Notice?>
    fun observeResponseExists(noticeId: String, uid: String, forSelectedResidents: Boolean = false): Flow<Boolean>
    /** The resident's response, live, telling a response still on its way apart from a saved one. */
    fun observeResponseState(noticeId: String, uid: String, forSelectedResidents: Boolean = false): Flow<ResponseState>
    suspend fun submitResponse(
        noticeId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        selectedOptions: List<String>,
        textResponse: String
    )
    /** Writes a survey response; [answers] are keyed by question id. */
    suspend fun submitSurveyResponse(
        noticeId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        answers: Map<String, SurveyAnswer>,
        forSelectedResidents: Boolean = false
    )
}
