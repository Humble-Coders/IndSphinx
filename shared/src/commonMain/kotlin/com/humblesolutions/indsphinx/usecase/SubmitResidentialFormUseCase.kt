package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.ResidentialAgreement
import com.humblesolutions.indsphinx.repository.ResidentialFormRepository

class SubmitResidentialFormUseCase(private val repository: ResidentialFormRepository) {
    suspend fun execute(agreement: ResidentialAgreement) {
        require(agreement.termsAccepted) { "You must accept the terms and conditions." }
        require(agreement.occupantId.isNotBlank()) { "Occupant ID is required." }
        repository.submitAgreement(agreement)
    }
}
