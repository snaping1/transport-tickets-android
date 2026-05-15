package com.transport.tickets.domain.usecase

import com.transport.tickets.data.local.datasource.LocalDataSource
import javax.inject.Inject

class ClearCacheUseCase @Inject constructor(
    private val localDataSource: LocalDataSource
) {
    suspend operator fun invoke() = localDataSource.clearAllCache()
}
