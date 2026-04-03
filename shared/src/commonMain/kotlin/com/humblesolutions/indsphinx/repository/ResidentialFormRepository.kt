package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.ResidentialAgreement

interface ResidentialFormRepository {
    suspend fun getFlatAmenities(flatId: String): Pair<List<String>, List<String>>
    suspend fun getTermsAndConditions(): String
    suspend fun submitAgreement(agreement: ResidentialAgreement)
    suspend fun hasSubmittedAgreement(occupantDocId: String): Boolean
}
