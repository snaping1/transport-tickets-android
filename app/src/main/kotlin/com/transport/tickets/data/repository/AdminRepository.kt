package com.transport.tickets.data.repository

import com.transport.tickets.data.remote.api.AdminApi
import com.transport.tickets.data.remote.dto.AdminLoginRequest
import com.transport.tickets.data.remote.dto.CreateRouteRequest
import com.transport.tickets.data.remote.dto.RouteDto
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(private val api: AdminApi) {

    private var token: String? = null
    val isLoggedIn: Boolean get() = token != null

    suspend fun login(email: String, password: String) {
        try {
            val response = api.login(AdminLoginRequest(email, password))
            token = response.token
        } catch (e: HttpException) {
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            throw Exception(
                when {
                    body?.contains("Invalid") == true -> "Неверный логин или пароль"
                    e.code() == 401 -> "Неверный логин или пароль"
                    e.code() == 404 -> "Сервер не найден — перезапусти бэкенд"
                    else -> "Ошибка сервера (${e.code()})"
                }
            )
        } catch (e: java.net.ConnectException) {
            throw Exception("Нет подключения к серверу")
        } catch (e: java.net.UnknownHostException) {
            throw Exception("Нет подключения к серверу")
        }
    }

    suspend fun getAllRoutes(): List<RouteDto> =
        api.getAllRoutes("Bearer ${requireToken()}")

    suspend fun createRoute(req: CreateRouteRequest): RouteDto =
        api.createRoute("Bearer ${requireToken()}", req)

    suspend fun deleteRoute(id: Int) =
        api.deleteRoute("Bearer ${requireToken()}", id)

    fun logout() { token = null }

    private fun requireToken() = token ?: error("Admin not authenticated")
}
