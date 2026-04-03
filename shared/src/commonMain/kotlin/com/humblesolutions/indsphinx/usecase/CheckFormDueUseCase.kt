package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.repository.ConfigRepository
import com.humblesolutions.indsphinx.repository.CoordinatorFormRepository

data class FormDueStatus(
    val isDue: Boolean,
    val frequencyMonths: Int
)

class CheckFormDueUseCase(
    private val configRepository: ConfigRepository,
    private val formRepository: CoordinatorFormRepository
) {
    suspend fun execute(occupantId: String, nowMillis: Long): FormDueStatus {
        val frequencyMonths = configRepository.getFormFrequencyMonths()
        val lastSubmittedAt = formRepository.getLastFormSubmittedAt(occupantId)

        if (lastSubmittedAt == null) {
            return FormDueStatus(isDue = true, frequencyMonths = frequencyMonths)
        }

        val frequencyMillis: Long = frequencyMonths.toLong() * 30L * 24L * 60L * 60L * 1000L
        val isDue = (nowMillis - lastSubmittedAt) >= frequencyMillis
        return FormDueStatus(isDue = isDue, frequencyMonths = frequencyMonths)
    }
}
