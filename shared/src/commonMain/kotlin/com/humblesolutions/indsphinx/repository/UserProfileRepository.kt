package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.OccupantProfile

interface UserProfileRepository {
    suspend fun getProfile(uid: String): OccupantProfile
}
