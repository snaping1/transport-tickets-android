package com.transport.tickets

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.transport.tickets.domain.model.Route
import com.transport.tickets.presentation.routes.RouteCard
import com.transport.tickets.ui.theme.TransportTicketsTheme
import org.junit.Rule
import org.junit.Test

class RoutesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleRoute = Route(
        id = 1,
        originCity = "Москва",
        destinationCity = "Санкт-Петербург",
        departureTime = "2025-12-01T10:00:00Z",
        arrivalTime = "2025-12-01T14:00:00Z",
        price = 1500.0,
        totalSeats = 50,
        availableSeats = 30,
        transportType = "bus"
    )

    @Test
    fun routeCard_displaysOriginAndDestination() {
        composeRule.setContent {
            TransportTicketsTheme {
                RouteCard(route = sampleRoute, onClick = {})
            }
        }
        composeRule.onNodeWithText("Москва → Санкт-Петербург").assertIsDisplayed()
    }

    @Test
    fun routeCard_displaysPrice() {
        composeRule.setContent {
            TransportTicketsTheme {
                RouteCard(route = sampleRoute, onClick = {})
            }
        }
        composeRule.onNodeWithText("1500 ₽").assertIsDisplayed()
    }

    @Test
    fun routeCard_displaysAvailableSeats() {
        composeRule.setContent {
            TransportTicketsTheme {
                RouteCard(route = sampleRoute, onClick = {})
            }
        }
        composeRule.onNodeWithText("Мест: 30").assertIsDisplayed()
    }

    @Test
    fun routeCard_clickTriggesCallback() {
        var clicked = false
        composeRule.setContent {
            TransportTicketsTheme {
                RouteCard(route = sampleRoute, onClick = { clicked = true })
            }
        }
        composeRule.onNodeWithText("Москва → Санкт-Петербург").performClick()
        assert(clicked)
    }

    @Test
    fun routeCard_showsLowSeatsWarning() {
        val lowSeatsRoute = sampleRoute.copy(availableSeats = 3)
        composeRule.setContent {
            TransportTicketsTheme {
                RouteCard(route = lowSeatsRoute, onClick = {})
            }
        }
        composeRule.onNodeWithText("Мест: 3").assertIsDisplayed()
    }
}
