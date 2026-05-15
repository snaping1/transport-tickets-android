package com.transport.tickets.domain.usecase

import com.transport.tickets.domain.model.UserProfile
import com.transport.tickets.domain.repository.UserRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(profile: UserProfile) = userRepository.saveProfile(profile)
}
