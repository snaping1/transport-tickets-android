package com.transport.tickets.presentation.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.*

private val RU_MONTHS = listOf(
    "янв", "фев", "мар", "апр", "май", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек"
)

private fun formatDate(year: Int, month: Int, day: Int) =
    "%02d %s %d".format(day, RU_MONTHS[month], year)

private fun formatTime(hour: Int, minute: Int) =
    "%02d:%02d".format(hour, minute)

private fun toIsoDate(year: Int, month: Int, day: Int) =
    "%04d-%02d-%02d".format(year, month + 1, day)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddRouteScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: AdminAddRouteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val context = LocalContext.current

    val now = remember { Calendar.getInstance() }

    var originCity by remember { mutableStateOf("") }
    var originExpanded by remember { mutableStateOf(false) }

    var destinationCity by remember { mutableStateOf("") }
    var destinationExpanded by remember { mutableStateOf(false) }

    var depYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var depMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var depDay by remember { mutableIntStateOf(now.get(Calendar.DAY_OF_MONTH)) }
    var depHour by remember { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var depMinute by remember { mutableIntStateOf(now.get(Calendar.MINUTE)) }
    var depDatePicked by remember { mutableStateOf(false) }
    var depTimePicked by remember { mutableStateOf(false) }

    var arrYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var arrMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var arrDay by remember { mutableIntStateOf(now.get(Calendar.DAY_OF_MONTH)) }
    var arrHour by remember { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY) + 2) }
    var arrMinute by remember { mutableIntStateOf(now.get(Calendar.MINUTE)) }
    var arrDatePicked by remember { mutableStateOf(false) }
    var arrTimePicked by remember { mutableStateOf(false) }

    var price by remember { mutableStateOf("") }
    var totalSeats by remember { mutableStateOf("") }
    var transportType by remember { mutableStateOf("bus") }

    LaunchedEffect(transportType) {
        if (transportType == "train") totalSeats = "654"
    }

    LaunchedEffect(state) {
        if (state is AdminAddRouteState.Success) {
            viewModel.resetState()
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый рейс") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── Маршрут ───────────────────────────────────────────────
            SectionCard(title = "Маршрут", icon = Icons.Default.Route) {
                CityDropdown(
                    label = "Откуда",
                    value = originCity,
                    onValueChange = { originCity = it },
                    suggestions = cities,
                    expanded = originExpanded,
                    onExpandedChange = { originExpanded = it }
                )
                Spacer(Modifier.height(8.dp))
                CityDropdown(
                    label = "Куда",
                    value = destinationCity,
                    onValueChange = { destinationCity = it },
                    suggestions = cities,
                    expanded = destinationExpanded,
                    onExpandedChange = { destinationExpanded = it }
                )
            }

            // ─── Транспорт ─────────────────────────────────────────────
            SectionCard(title = "Транспорт", icon = Icons.Default.DirectionsBus) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransportChip(
                        label = "Автобус",
                        icon = Icons.Default.DirectionsBus,
                        selected = transportType == "bus",
                        onClick = { transportType = "bus" },
                        modifier = Modifier.weight(1f)
                    )
                    TransportChip(
                        label = "Поезд",
                        icon = Icons.Default.Train,
                        selected = transportType == "train",
                        onClick = { transportType = "train" },
                        modifier = Modifier.weight(1f)
                    )
                    TransportChip(
                        label = "Самолёт",
                        icon = Icons.Default.Flight,
                        selected = transportType == "plane",
                        onClick = { transportType = "plane" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ─── Отправление ───────────────────────────────────────────
            SectionCard(title = "Отправление", icon = Icons.Default.FlightTakeoff) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateTimeButton(
                        label = "Дата",
                        value = if (depDatePicked) formatDate(depYear, depMonth, depDay) else "Выбрать",
                        icon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                depYear = y; depMonth = m; depDay = d; depDatePicked = true
                            }, depYear, depMonth, depDay).show()
                        }
                    )
                    DateTimeButton(
                        label = "Время",
                        value = if (depTimePicked) formatTime(depHour, depMinute) else "Выбрать",
                        icon = Icons.Default.AccessTime,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            TimePickerDialog(context, { _, h, m ->
                                depHour = h; depMinute = m; depTimePicked = true
                            }, depHour, depMinute, true).show()
                        }
                    )
                }
            }

            // ─── Прибытие ──────────────────────────────────────────────
            SectionCard(title = "Прибытие", icon = Icons.Default.FlightLand) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateTimeButton(
                        label = "Дата",
                        value = if (arrDatePicked) formatDate(arrYear, arrMonth, arrDay) else "Выбрать",
                        icon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                arrYear = y; arrMonth = m; arrDay = d; arrDatePicked = true
                            }, arrYear, arrMonth, arrDay).show()
                        }
                    )
                    DateTimeButton(
                        label = "Время",
                        value = if (arrTimePicked) formatTime(arrHour, arrMinute) else "Выбрать",
                        icon = Icons.Default.AccessTime,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            TimePickerDialog(context, { _, h, m ->
                                arrHour = h; arrMinute = m; arrTimePicked = true
                            }, arrHour, arrMinute, true).show()
                        }
                    )
                }
            }

            // ─── Детали ────────────────────────────────────────────────
            SectionCard(title = "Детали", icon = Icons.Default.Info) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Цена, ₽") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Text("₽", style = MaterialTheme.typography.bodyLarge) }
                    )
                    OutlinedTextField(
                        value = totalSeats,
                        onValueChange = { totalSeats = it },
                        label = { Text("Мест") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.EventSeat, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            if (state is AdminAddRouteState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = (state as AdminAddRouteState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.resetState()
                    viewModel.createRoute(
                        originCity, destinationCity,
                        toIsoDate(depYear, depMonth, depDay), formatTime(depHour, depMinute),
                        toIsoDate(arrYear, arrMonth, arrDay), formatTime(arrHour, arrMinute),
                        price, totalSeats, transportType
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = state !is AdminAddRouteState.Loading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (state is AdminAddRouteState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Создать рейс", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityDropdown(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val filtered = remember(value, suggestions) {
        if (value.isBlank()) suggestions
        else suggestions.filter { it.contains(value, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filtered.isNotEmpty(),
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); onExpandedChange(true) },
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить", modifier = Modifier.size(18.dp))
                    }
                }
            }
        )
        if (filtered.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                filtered.take(6).forEach { city ->
                    DropdownMenuItem(
                        text = { Text(city) },
                        onClick = { onValueChange(city); onExpandedChange(false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransportChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(2.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        },
        modifier = modifier,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun DateTimeButton(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
