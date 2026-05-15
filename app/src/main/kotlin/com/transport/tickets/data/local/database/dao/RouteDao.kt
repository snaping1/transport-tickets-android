package com.transport.tickets.data.local.database.dao

import androidx.room.*
import com.transport.tickets.data.local.database.entities.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY departureTime ASC")
    fun getAllRoutes(): Flow<List<RouteEntity>>

    @Query("""
        SELECT * FROM routes
        WHERE (:origin IS NULL OR LOWER(originCity) LIKE '%' || LOWER(:origin) || '%')
          AND (:destination IS NULL OR LOWER(destinationCity) LIKE '%' || LOWER(:destination) || '%')
        ORDER BY departureTime ASC
    """)
    fun getFilteredRoutes(origin: String?, destination: String?): Flow<List<RouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routes: List<RouteEntity>)

    @Query("DELETE FROM routes")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(routes: List<RouteEntity>) {
        deleteAll()
        insertAll(routes)
    }
}
