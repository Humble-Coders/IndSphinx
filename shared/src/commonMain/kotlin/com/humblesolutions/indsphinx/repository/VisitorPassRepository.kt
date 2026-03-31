package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.VisitorPass
import kotlinx.coroutines.flow.Flow

interface VisitorPassRepository {
    suspend fun submitVisitorPass(pass: VisitorPass): String
    fun observeByOccupant(occupantId: String): Flow<List<VisitorPass>>
}
