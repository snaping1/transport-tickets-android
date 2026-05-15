package com.transport.tickets.di

import com.transport.tickets.data.repository.RouteRepositoryImpl
import com.transport.tickets.data.repository.TicketRepositoryImpl
import com.transport.tickets.domain.repository.RouteRepository
import com.transport.tickets.domain.repository.TicketRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRouteRepository(impl: RouteRepositoryImpl): RouteRepository

    @Binds
    @Singleton
    abstract fun bindTicketRepository(impl: TicketRepositoryImpl): TicketRepository
}
