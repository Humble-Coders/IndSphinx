package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.Document
import com.humblesolutions.indsphinx.repository.DocumentRepository

class FetchDocumentsUseCase(private val repository: DocumentRepository) {
    suspend fun execute(): List<Document> = repository.getAllDocuments()
}
