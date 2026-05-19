package com.transport.tickets.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.transport.tickets.data.remote.dto.RouteDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onAddRoute: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminPanelViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var routeToDelete by remember { mutableStateOf<RouteDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление рейсами") },
                actions = {
                    IconButton(onClick = { viewModel.loadRoutes() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Выйти")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRoute) {
                Icon(Icons.Default.Add, contentDescription = "Добавить рейс")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error ?: "Ошибка", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadRoutes() }) { Text("Повторить") }
                }
                state.routes.isEmpty() -> Text(
                    "Рейсы не найдены",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.routes, key = { it.id }) { route ->
                        RouteAdminCard(
                            route = route,
                            onDelete = { routeToDelete = route }
                        )
                    }
                }
            }
        }
    }

    routeToDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            title = { Text("Удалить рейс?") },
            text = { Text("${route.originCity} → ${route.destinationCity}") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRoute(route.id)
                    routeToDelete = null
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { routeToDelete = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun RouteAdminCard(route: RouteDto, onDelete: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())
    val transportLabel = when (route.transportType) {
        "train" -> "Поезд"
        "plane" -> "Самолёт"
        else -> "Автобус"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${route.originCity} → ${route.destinationCity}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatter.format(Instant.parse(route.departureTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = transportLabel,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "₽${route.price.toInt()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${route.availableSeats}/${route.totalSeats} мест",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (route.availableSeats == 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
