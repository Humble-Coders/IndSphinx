package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.FlatVacantRequest
import kotlinx.coroutines.flow.Flow

interface FlatVacantRequestRepository {
    suspend fun submitRequest(request: FlatVacantRequest): String
    fun observeByOccupant(occupantId: String): Flow<List<FlatVacantRequest>>
}
