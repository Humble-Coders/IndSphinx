package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.OccupantProfile
import com.humblesolutions.indsphinx.repository.UserProfileRepository

class ValidateOccupantUseCase(private val userProfileRepository: UserProfileRepository) {
    suspend fun execute(uid: String): OccupantProfile {
        val profile = userProfileRepository.getProfile(uid)
        if (!profile.enabled) {
            throw Exception("Your account has been disabled. Please contact the admin.")
        }
        if (profile.role != "OCCUPANT" && profile.role != "COORDINATOR") {
            throw Exception("Access is restricted to occupants and coordinators only.")
        }
        return profile
    }
}
