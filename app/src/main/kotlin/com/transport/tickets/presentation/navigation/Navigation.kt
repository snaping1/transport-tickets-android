package com.transport.tickets.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.transport.tickets.data.repository.toDomain
import com.transport.tickets.domain.model.Route
import com.transport.tickets.presentation.auth.AuthScreen
import com.transport.tickets.presentation.auth.AuthViewModel
import com.transport.tickets.presentation.profile.ProfileScreen
import com.transport.tickets.presentation.purchase.PassengerInputScreen
import com.transport.tickets.presentation.purchase.PaymentScreen
import com.transport.tickets.presentation.purchase.PurchaseScreen
import com.transport.tickets.presentation.routes.RoutesScreen
import com.transport.tickets.presentation.tickets.TicketDetailScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Routes : Screen("routes")
    data object Purchase : Screen("purchase/{routeJson}") {
        fun createRoute(route: Route): String {
            val json = Json.encodeToString(
                com.transport.tickets.data.remote.dto.RouteDto(
                    id = route.id,
                    originCity = route.originCity,
                    destinationCity = route.destinationCity,
                    departureTime = route.departureTime,
                    arrivalTime = route.arrivalTime,
                    price = route.price,
                    totalSeats = route.totalSeats,
                    availableSeats = route.availableSeats,
                    transportType = route.transportType,
                    svAvailableSeats = route.svAvailableSeats,
                    coupeAvailableSeats = route.coupeAvailableSeats,
                    platzkartAvailableSeats = route.platzkartAvailableSeats,
                    seatCarAvailableSeats = route.seatCarAvailableSeats
                )
            )
            return "purchase/${java.net.URLEncoder.encode(json, "UTF-8")}"
        }
    }
    data object PassengerInput : Screen("passenger_input")
    data object Payment : Screen("payment")
    data object TicketDetail : Screen("ticket_detail/{ticketId}") {
        fun createRoute(ticketId: Int) = "ticket_detail/$ticketId"
    }
    data object Profile : Screen("profile")
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (authViewModel.isLoggedIn) Screen.Routes.route else Screen.Auth.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Routes.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        composable(Screen.Routes.route) {
            RoutesScreen(
                onRouteClick = { route -> navController.navigate(Screen.Purchase.createRoute(route)) },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Purchase.route) { backStackEntry ->
            val encodedJson = backStackEntry.arguments?.getString("routeJson") ?: ""
            val json = java.net.URLDecoder.decode(encodedJson, "UTF-8")
            val routeDto = Json { ignoreUnknownKeys = true }.decodeFromString<com.transport.tickets.data.remote.dto.RouteDto>(json)
            val route = routeDto.toDomain()
            PurchaseScreen(
                route = route,
                onContinue = { navController.navigate(Screen.PassengerInput.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PassengerInput.route) {
            PassengerInputScreen(
                onContinue = { navController.navigate(Screen.Payment.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Payment.route) {
            PaymentScreen(
                onPaymentSuccess = { ticketId ->
                    navController.navigate(Screen.TicketDetail.createRoute(ticketId)) {
                        popUpTo(Screen.Routes.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TicketDetail.route,
            arguments = listOf(navArgument("ticketId") { type = NavType.IntType })
        ) {
            TicketDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
                onNavigateToSearch = { navController.popBackStack() },
                onTicketClick = { ticketId ->
                    navController.navigate(Screen.TicketDetail.createRoute(ticketId))
                }
            )
        }
    }
}
