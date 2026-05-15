package com.transport.tickets.domain.usecase

import com.transport.tickets.domain.model.UserProfile
import com.transport.tickets.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<UserProfile?> = userRepository.getProfile()
}
