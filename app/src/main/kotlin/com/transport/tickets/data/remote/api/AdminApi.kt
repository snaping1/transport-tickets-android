package com.transport.tickets.data.remote.api

import com.transport.tickets.data.remote.dto.AdminLoginRequest
import com.transport.tickets.data.remote.dto.AdminLoginResponse
import com.transport.tickets.data.remote.dto.CreateRouteRequest
import com.transport.tickets.data.remote.dto.RouteDto
import retrofit2.http.*

interface AdminApi {

    @POST("admin/login")
    suspend fun login(@Body request: AdminLoginRequest): AdminLoginResponse

    @GET("admin/routes")
    suspend fun getAllRoutes(@Header("Authorization") token: String): List<RouteDto>

    @POST("admin/routes")
    suspend fun createRoute(
        @Header("Authorization") token: String,
        @Body request: CreateRouteRequest
    ): RouteDto

    @DELETE("admin/routes/{id}")
    suspend fun deleteRoute(
        @Header("Authorization") token: String,
        @Path("id") routeId: Int
    )
}
