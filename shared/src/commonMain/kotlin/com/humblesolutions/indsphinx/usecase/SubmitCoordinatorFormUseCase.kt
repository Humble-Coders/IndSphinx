package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.MonthlyCheckForm
import com.humblesolutions.indsphinx.repository.CoordinatorFormRepository

class SubmitCoordinatorFormUseCase(private val repository: CoordinatorFormRepository) {
    suspend fun execute(form: MonthlyCheckForm) {
        require(form.confirmed) { "Please confirm the declaration before submitting." }
        require(form.occupantId.isNotBlank()) { "Coordinator ID is required." }
        repository.submitForm(form)
    }
}
