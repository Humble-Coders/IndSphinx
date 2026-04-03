package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.Document

interface DocumentRepository {
    suspend fun getAllDocuments(): List<Document>
}
