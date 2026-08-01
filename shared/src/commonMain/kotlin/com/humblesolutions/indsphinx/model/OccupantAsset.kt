package com.humblesolutions.indsphinx.model

/**
 * An asset issued to THIS occupant (almirah, key, chair) by the admin portal.
 *
 * One record per assignment episode: taking an item back closes the record and
 * it stays as history, so an occupant's list is every record ever raised
 * against them. The app is read-only here; all changes come from the portal.
 *
 * `assetNumber` is the item's identity. `remarks` holds the handover note while
 * the asset is open, and the admin's closing note once it is closed, so the UI
 * only shows it for open records.
 */
data class OccupantAsset(
    val id: String = "",
    val assetName: String = "",
    val assetNumber: String = "",
    val occupantId: String = "",
    val occupantAuthUid: String = "",
    /** Epoch millis; 0 when unset. */
    val assignedDate: Long = 0L,
    /** Epoch millis the record closed; 0 while still held. */
    val returnedDate: Long = 0L,
    /** ASSIGNED | RETURNED | DAMAGED | MISSING */
    val status: String = STATUS_ASSIGNED,
    val remarks: String = "",
) {
    /** True while the occupant still holds this item. */
    val isOpen: Boolean get() = status.uppercase() == STATUS_ASSIGNED

    /** "Almirah 45A", or just whichever part is present. */
    val displayLabel: String
        get() = listOf(assetName.trim(), assetNumber.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")

    companion object {
        const val STATUS_ASSIGNED = "ASSIGNED"
        const val STATUS_RETURNED = "RETURNED"
        const val STATUS_DAMAGED  = "DAMAGED"
        const val STATUS_MISSING  = "MISSING"

        /** Label shown on the status chip. */
        fun statusLabel(status: String): String = when (status.uppercase()) {
            STATUS_RETURNED -> "Returned"
            STATUS_DAMAGED  -> "Damaged"
            STATUS_MISSING  -> "Missing"
            else            -> "Assigned"
        }
    }
}
