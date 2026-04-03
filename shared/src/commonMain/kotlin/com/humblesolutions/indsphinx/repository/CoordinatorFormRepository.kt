package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.MonthlyCheckForm

interface CoordinatorFormRepository {
    suspend fun submitForm(form: MonthlyCheckForm)
    suspend fun getLastFormSubmittedAt(occupantId: String): Long?
}
