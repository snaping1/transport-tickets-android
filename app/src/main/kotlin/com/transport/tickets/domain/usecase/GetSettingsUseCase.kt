package com.transport.tickets.domain.usecase

import com.transport.tickets.domain.model.AppSettings
import com.transport.tickets.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<AppSettings> = userRepository.getSettings()
}
