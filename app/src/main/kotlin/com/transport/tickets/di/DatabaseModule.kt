package com.transport.tickets.di

import android.content.Context
import androidx.room.Room
import com.transport.tickets.data.local.database.AppDatabase
import com.transport.tickets.data.local.database.dao.RouteDao
import com.transport.tickets.data.local.database.dao.TicketDao
import com.transport.tickets.data.local.database.dao.PassengerDao
import com.transport.tickets.data.local.database.dao.UserProfileDao
import com.transport.tickets.data.local.datasource.LocalDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "transport_tickets.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideRouteDao(db: AppDatabase): RouteDao = db.routeDao()

    @Provides
    @Singleton
    fun provideTicketDao(db: AppDatabase): TicketDao = db.ticketDao()

    @Provides
    @Singleton
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    @Singleton
    fun providePassengerDao(db: AppDatabase): PassengerDao = db.passengerDao()

    @Provides
    @Singleton
    fun provideLocalDataSource(routeDao: RouteDao, ticketDao: TicketDao): LocalDataSource =
        LocalDataSource(routeDao, ticketDao)
}
