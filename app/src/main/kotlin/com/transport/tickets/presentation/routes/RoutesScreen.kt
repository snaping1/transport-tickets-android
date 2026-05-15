package com.transport.tickets.presentation.routes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.transport.tickets.domain.model.Route
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val transportTypes = listOf(
    Triple("", "Все", Icons.Default.List),
    Triple("bus", "Автобус", Icons.Default.DirectionsBus),
    Triple("train", "Поезд", Icons.Default.Train),
    Triple("plane", "Самолёт", Icons.Default.Flight)
)

private fun accentColorFor(type: String) = when (type) {
    "train" -> Color(0xFFE65100)  // deep orange — RZhD style
    "plane" -> Color(0xFF0277BD)  // blue — sky
    else    -> Color(0xFF2E7D32)  // green — road
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    onRouteClick: (Route) -> Unit,
    onProfileClick: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: RoutesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val allCities = remember(uiState.routes) {
        uiState.routes
            .flatMap { listOf(it.originCity, it.destinationCity) }
            .distinct()
            .sorted()
    }

    val pullState = rememberPullToRefreshState()
    val isRefreshing = uiState.isLoading && uiState.routes.isNotEmpty()

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Вы уверены, что хотите выйти?") },
            confirmButton = {
                Button(
                    onClick = { onSignOut(); showSignOutConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Выйти") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSignOutConfirm = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Маршруты",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.routes.isNotEmpty()) {
                            Text(
                                "Найдено ${uiState.routes.size} рейсов",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            if (showFilters) Icons.Default.Close else Icons.Default.FilterList,
                            contentDescription = "Фильтры",
                            tint = if (showFilters) MaterialTheme.colorScheme.primary
                                   else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Профиль")
                    }
                    IconButton(onClick = { showSignOutConfirm = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Выйти")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterChipsRow(
                selected = uiState.transportTypeFilter,
                onSelect = viewModel::setTransportTypeFilter
            )

            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FilterPanel(
                    origin = uiState.originFilter,
                    destination = uiState.destinationFilter,
                    date = uiState.dateFilter,
                    onOriginChange = viewModel::setOriginFilter,
                    onDestinationChange = viewModel::setDestinationFilter,
                    onDateChange = viewModel::setDateFilter,
                    onApply = { viewModel.applyFilters(); showFilters = false },
                    availableCities = allCities
                )
            }

            AnimatedVisibility(visible = uiState.error != null) {
                uiState.error?.let { err ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.WifiOff, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                state = pullState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = isRefreshing,
                        threshold = 52.dp,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            ) {
                when {
                    uiState.isLoading && uiState.routes.isEmpty() -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            userScrollEnabled = false
                        ) {
                            items(4) { RouteCardSkeleton() }
                        }
                    }

                    uiState.routes.isEmpty() -> EmptyState()

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.routes, key = { it.id }) { route ->
                                Box(modifier = Modifier.animateItem(
                                    fadeInSpec = tween(300),
                                    placementSpec = tween(300)
                                )) {
                                    RouteCard(route = route, onClick = { onRouteClick(route) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(transportTypes) { (type, label, icon) ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun RouteCard(route: Route, onClick: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    val dateFmt = DateTimeFormatter.ofPattern("EE, d MMM yyyy", Locale("ru")).withZone(zone)

    val departure = runCatching { Instant.parse(route.departureTime) }.getOrNull()
    val arrival   = runCatching { Instant.parse(route.arrivalTime) }.getOrNull()

    val depTime = departure?.let { timeFmt.format(it) } ?: "--:--"
    val arrTime = arrival?.let { timeFmt.format(it) } ?: "--:--"
    val dateStr = departure?.let { dateFmt.format(it) }
        ?.replaceFirstChar { it.uppercase() } ?: ""

    val duration = if (departure != null && arrival != null) {
        val dur = Duration.between(departure, arrival)
        val h = dur.toHours()
        val m = dur.toMinutes() % 60
        if (h > 0) "${h}ч ${m}м" else "${m}м"
    } else ""

    val accent = accentColorFor(route.transportType)
    val (tIcon, tLabel) = transportIcon(route.transportType)
    val isTrain = route.transportType == "train"
    val minPrice = if (isTrain) (route.price * 0.7).toInt() else route.price.toInt()

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // ── Left accent strip ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Header: transport badge + price ──────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(tIcon, null, modifier = Modifier.size(15.dp), tint = accent)
                            Text(
                                tLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = accent
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "от $minPrice ₽",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Мест: ${route.availableSeats}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (route.availableSeats < 5)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Route timeline ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            depTime,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            route.originCity,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.5.dp
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Icon(
                                    tIcon, null,
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .size(16.dp),
                                    tint = accent
                                )
                            }
                            if (duration.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        duration,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            arrTime,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            route.destinationCity,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // ── Footer: date + button ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth, null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text(
                            "Выбрать",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainTariffRow(route: Route) {
    val tariffs = listOf(
        Triple("СВ",   route.svAvailableSeats ?: 0,        (route.price * 2.5).toInt()),
        Triple("Купе", route.coupeAvailableSeats ?: 0,     (route.price * 1.8).toInt()),
        Triple("Плц",  route.platzkartAvailableSeats ?: 0, route.price.toInt()),
        Triple("Сид",  route.seatCarAvailableSeats ?: 0,   (route.price * 0.7).toInt())
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tariffs.forEach { (label, available, price) ->
            TariffBadge(label, available, price, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TariffBadge(label: String, available: Int, price: Int, modifier: Modifier = Modifier) {
    val isEmpty = available <= 0
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isEmpty) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        border = if (isEmpty) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.primary
            )
            Text(
                "от ${price}₽",
                style = MaterialTheme.typography.labelSmall,
                color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (isEmpty) "нет" else "$available",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isEmpty) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ── Skeleton loading ─────────────────────────────────────────────────────────

@Composable
private fun RouteCardSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.3f))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ShimmerBox(width = 88.dp, height = 26.dp, alpha = alpha)
                    ShimmerBox(width = 72.dp, height = 26.dp, alpha = alpha)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(width = 56.dp, height = 38.dp, alpha = alpha)
                    ShimmerBox(width = 36.dp, height = 36.dp, alpha = alpha, circle = true)
                    ShimmerBox(width = 56.dp, height = 38.dp, alpha = alpha)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ShimmerBox(width = 120.dp, height = 18.dp, alpha = alpha)
                    ShimmerBox(width = 88.dp, height = 34.dp, alpha = alpha)
                }
            }
        }
    }
}

@Composable
private fun ShimmerBox(width: Dp, height: Dp, alpha: Float, circle: Boolean = false) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(if (circle) CircleShape else RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f))
    )
}

// ── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Search, null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Маршруты не найдены",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Попробуйте изменить фильтры\nили потяните вниз для обновления",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
    }
}

// ── Filter panel ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterPanel(
    origin: String,
    destination: String,
    date: String,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onApply: () -> Unit,
    availableCities: List<String>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CityTextField(
                    value = origin,
                    onValueChange = onOriginChange,
                    label = "Откуда",
                    suggestions = availableCities,
                    leadingIcon = { Icon(Icons.Default.FlightTakeoff, null, Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f)
                )
                CityTextField(
                    value = destination,
                    onValueChange = onDestinationChange,
                    label = "Куда",
                    suggestions = availableCities,
                    leadingIcon = { Icon(Icons.Default.FlightLand, null, Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            DatePickerField(
                date = date,
                onDateChange = onDateChange
            )

            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Найти рейсы", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CityTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = remember(value, suggestions) {
        if (value.length < 2) emptyList()
        else suggestions
            .filter { it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true) }
            .take(5)
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { new ->
                onValueChange(new)
                dropdownExpanded = new.length >= 2
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = leadingIcon,
            shape = RoundedCornerShape(12.dp)
        )
        DropdownMenu(
            expanded = dropdownExpanded && filtered.isNotEmpty(),
            onDismissRequest = { dropdownExpanded = false }
        ) {
            filtered.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        onValueChange(city)
                        dropdownExpanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    date: String,
    onDateChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val displayDate = remember(date) {
        if (date.isEmpty()) ""
        else runCatching {
            LocalDate.parse(date)
                .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))
        }.getOrElse { date }
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val ld = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onDateChange(ld.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    OutlinedCard(
        onClick = { showDialog = true },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.DateRange, null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Дата отправления",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    displayDate.ifEmpty { "Любая дата" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (date.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                )
            }
            if (date.isNotEmpty()) {
                IconButton(
                    onClick = { onDateChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                }
            }
        }
    }
}

fun transportIcon(type: String) = when (type) {
    "train" -> Pair(Icons.Default.Train, "Поезд")
    "plane" -> Pair(Icons.Default.Flight, "Самолёт")
    else    -> Pair(Icons.Default.DirectionsBus, "Автобус")
}
