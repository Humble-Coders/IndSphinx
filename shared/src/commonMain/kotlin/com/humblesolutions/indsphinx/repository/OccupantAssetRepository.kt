package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.OccupantAsset
import kotlinx.coroutines.flow.Flow

interface OccupantAssetRepository {
    /**
     * Live stream of every asset ever raised against this occupant, open and
     * closed. Keyed by Auth UID because that is the field the Firestore rule
     * matches on when granting an occupant access to their own records.
     */
    fun observeByAuthUid(authUid: String): Flow<List<OccupantAsset>>
}
