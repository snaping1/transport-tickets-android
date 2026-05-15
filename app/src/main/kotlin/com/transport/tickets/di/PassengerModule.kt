package com.transport.tickets.di

import com.transport.tickets.data.repository.PassengerRepositoryImpl
import com.transport.tickets.domain.repository.PassengerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PassengerModule {
    @Binds @Singleton
    abstract fun bindPassengerRepository(impl: PassengerRepositoryImpl): PassengerRepository
}
