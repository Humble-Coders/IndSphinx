package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.VisitorPass
import com.humblesolutions.indsphinx.repository.VisitorPassRepository

class SubmitVisitorPassUseCase(private val repo: VisitorPassRepository) {
    suspend fun execute(pass: VisitorPass): String {
        require(pass.visitorName.isNotBlank()) { "Visitor name is required" }
        require(pass.visitorPhone.isNotBlank()) { "Phone number is required" }
        require(pass.visitDate > 0L) { "Visit date is required" }
        return repo.submitVisitorPass(pass)
    }
}
