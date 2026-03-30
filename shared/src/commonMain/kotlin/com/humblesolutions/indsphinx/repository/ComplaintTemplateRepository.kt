package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.ComplaintTemplate

interface ComplaintTemplateRepository {
    suspend fun getTemplates(): List<ComplaintTemplate>
}
