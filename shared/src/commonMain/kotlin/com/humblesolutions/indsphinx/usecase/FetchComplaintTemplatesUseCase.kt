package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.ComplaintTemplate
import com.humblesolutions.indsphinx.repository.ComplaintTemplateRepository

class FetchComplaintTemplatesUseCase(private val repo: ComplaintTemplateRepository) {
    suspend fun execute(): List<ComplaintTemplate> = repo.getTemplates()
}
