package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.OccupantAsset
import com.humblesolutions.indsphinx.repository.OccupantAssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The occupant's assets, already split into what they hold now and what they
 * held before.
 */
data class OccupantAssetsSnapshot(
    val current: List<OccupantAsset> = emptyList(),
    val past: List<OccupantAsset> = emptyList(),
) {
    val isEmpty: Boolean get() = current.isEmpty() && past.isEmpty()
    val totalCount: Int get() = current.size + past.size
}

/**
 * Splits and orders the occupant's assets.
 *
 * The partition and sort live here rather than in each ViewModel so Android and
 * iOS can never drift apart on what "current" means or what order rows appear
 * in. Both lists are newest-assigned first.
 */
class ObserveOccupantAssetsUseCase(private val repo: OccupantAssetRepository) {

    fun execute(authUid: String): Flow<OccupantAssetsSnapshot> =
        repo.observeByAuthUid(authUid).map { assets ->
            val newestFirst = assets.sortedWith(
                compareByDescending<OccupantAsset> { it.assignedDate }
                    .thenBy { it.assetName.lowercase() }
                    .thenBy { it.assetNumber.lowercase() },
            )
            OccupantAssetsSnapshot(
                current = newestFirst.filter { it.isOpen },
                past    = newestFirst.filterNot { it.isOpen },
            )
        }
}
