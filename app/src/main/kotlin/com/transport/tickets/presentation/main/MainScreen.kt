package com.transport.tickets.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.transport.tickets.domain.model.Route
import com.transport.tickets.presentation.profile.ProfileScreen
import com.transport.tickets.presentation.routes.RoutesScreen
import com.transport.tickets.presentation.tickets.MyTicketsScreen

@Composable
fun MainScreen(
    onRouteClick: (Route) -> Unit,
    onTicketClick: (Int) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Афиша") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
                    label = { Text("Билеты") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Профиль") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> RoutesScreen(
                    onRouteClick = onRouteClick,
                    onProfileClick = { selectedTab = 2 },
                    onSignOut = onSignOut,
                    showAccountActions = false
                )
                1 -> MyTicketsScreen(
                    onBack = {},
                    showBackButton = false,
                    onTicketClick = onTicketClick,
                    onNavigateToSearch = { selectedTab = 0 }
                )
                2 -> ProfileScreen(
                    onSignOut = onSignOut,
                    onBack = {},
                    showBackButton = false,
                    onNavigateToSearch = { selectedTab = 0 },
                    onTicketClick = onTicketClick
                )
            }
        }
    }
}
