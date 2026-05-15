package com.transport.tickets.presentation.purchase

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.transport.tickets.domain.model.Route
import com.transport.tickets.presentation.routes.transportIcon
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

// ── Seat colours: free / selected / occupied ───────────────────────────────────
private val SeatFreeColor  = Color(0xFFA5D6A7)
private val SeatFreeFg     = Color(0xFF1B5E20)
private val SeatSelectedBg = Color(0xFFFFC107)
private val SeatSelectedFg = Color(0xFF212121)
private val SeatOccupiedBg = Color(0xFFFFCDD2)
private val SeatOccupiedFg = Color(0xFFB71C1C)

private fun planeSeatPrice(seat: Int, basePrice: Double): Int {
    return when {
        seat <= 8  -> (basePrice * 2.0).toInt()
        seat <= 56 -> (basePrice * 1.3).toInt()
        else       -> basePrice.toInt()
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PurchaseScreen
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    route: Route,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    viewModel: PurchaseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(route.id) { viewModel.loadSeats(route.id) }

    val seatRange = if (route.transportType == "train") uiState.currentSeatRange() else 1..route.totalSeats
    val totalPrice = uiState.selectedSeats.sumOf { seat ->
        route.price * PricingUtils.seatMultiplier(seat, route.totalSeats, route.transportType)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Выбор мест", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${route.originCity} → ${route.destinationCity}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        bottomBar = {
            PurchaseBottomBar(
                selectedSeats = uiState.selectedSeats,
                totalPrice    = totalPrice,
                transportType = route.transportType,
                seatRange     = seatRange,
                onRemoveSeat  = viewModel::toggleSeat,
                onContinue    = { viewModel.prepareForPassengerInput(route); onContinue() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            RouteInfoCard(route)

            if (route.transportType == "train") {
                WagonSelectorRow(
                    wagons        = TRAIN_WAGONS,
                    selectedWagon = uiState.selectedWagon,
                    occupiedSeats = uiState.occupiedSeats,
                    onSelect      = viewModel::setWagon
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            when (route.transportType) {
                                "train" -> "Схема вагона"
                                "plane" -> "Схема салона"
                                else    -> "Схема автобуса"
                            },
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.selectedSeats.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Выбрано: ${uiState.selectedSeats.size}",
                                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style      = MaterialTheme.typography.labelMedium,
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (route.transportType == "train") PriceHint(uiState.wagonType, route.price)

                    SeatLegend()

                    if (uiState.seatsLoading) {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        when (route.transportType) {
                            "plane" -> PlaneSeatMap(
                                totalSeats    = route.totalSeats,
                                occupiedSeats = uiState.occupiedSeats,
                                selectedSeats = uiState.selectedSeats,
                                basePrice     = route.price,
                                onSeatClick   = { seat ->
                                    if (seat !in uiState.occupiedSeats) {
                                        viewModel.toggleSeat(seat)
                                        val price = planeSeatPrice(seat, route.price)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Место $seat — $price ₽")
                                        }
                                    }
                                }
                            )
                            "train" -> TrainSeatMap(
                                seatRange     = seatRange,
                                occupiedSeats = uiState.occupiedSeats,
                                selectedSeats = uiState.selectedSeats,
                                wagonType     = uiState.wagonType,
                                wagonNumber   = uiState.selectedWagon,
                                basePrice     = route.price
                            ) { if (it !in uiState.occupiedSeats) viewModel.toggleSeat(it) }
                            else -> BusSeatMap(
                                totalSeats    = route.totalSeats,
                                occupiedSeats = uiState.occupiedSeats,
                                selectedSeats = uiState.selectedSeats,
                                basePrice     = route.price
                            ) { if (it !in uiState.occupiedSeats) viewModel.toggleSeat(it) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Route info card ────────────────────────────────────────────────────────────

@Composable
private fun RouteInfoCard(route: Route) {
    val zone    = ZoneId.systemDefault()
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    val dateFmt = DateTimeFormatter.ofPattern("EE, d MMM", Locale("ru")).withZone(zone)
    val dep = runCatching { Instant.parse(route.departureTime) }.getOrNull()
    val arr = runCatching { Instant.parse(route.arrivalTime) }.getOrNull()
    val dur = if (dep != null && arr != null) {
        val d = Duration.between(dep, arr)
        "${d.toHours()}ч ${d.toMinutes() % 60}м"
    } else ""
    val (tIcon, _) = transportIcon(route.transportType)
    val (minP, maxP) = PricingUtils.priceRange(route.price, route.transportType)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(0.6f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(dep?.let { timeFmt.format(it) } ?: "--:--", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(route.originCity, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    dep?.let { Text(dateFmt.format(it).replaceFirstChar { c -> c.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dur, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(1.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        Icon(tIcon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Box(Modifier.weight(1f).height(1.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("от $minP до $maxP ₽", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(arr?.let { timeFmt.format(it) } ?: "--:--", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(route.destinationCity, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    arr?.let { Text(dateFmt.format(it).replaceFirstChar { c -> c.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventSeat, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                if (route.transportType == "train" && route.svAvailableSeats != null) {
                    listOf("СВ" to route.svAvailableSeats, "Купе" to route.coupeAvailableSeats,
                        "Плц" to route.platzkartAvailableSeats, "Сид" to route.seatCarAvailableSeats).forEach { (label, count) ->
                        Surface(color = if ((count ?: 0) > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                            Text("$label: ${count ?: 0}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if ((count ?: 0) > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Text("Свободно мест: ${route.availableSeats}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                        color = if (route.availableSeats < 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Wagon selector ─────────────────────────────────────────────────────────────

@Composable
private fun WagonSelectorRow(
    wagons: List<WagonConfig>,
    selectedWagon: Int,
    occupiedSeats: List<Int>,
    onSelect: (Int) -> Unit
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(wagons) { wagon ->
            val start = run { var s = 1; for (w in TRAIN_WAGONS) { if (w.number == wagon.number) break; s += w.type.seatsPerWagon }; s }
            val end   = start + wagon.type.seatsPerWagon - 1
            val free  = wagon.type.seatsPerWagon - occupiedSeats.count { it in start..end }
            val wagonColor = when (wagon.type) {
                WagonType.SV        -> MaterialTheme.colorScheme.tertiary
                WagonType.COUPE     -> MaterialTheme.colorScheme.primary
                WagonType.PLATZKART -> MaterialTheme.colorScheme.secondary
                WagonType.SEAT      -> MaterialTheme.colorScheme.outline
            }
            FilterChip(
                selected = selectedWagon == wagon.number,
                onClick  = { onSelect(wagon.number) },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${wagon.number}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(wagon.type.shortLabel, style = MaterialTheme.typography.labelSmall)
                        Text("$free св.", style = MaterialTheme.typography.labelSmall,
                            color = if (free < 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = wagonColor.copy(0.2f),
                    selectedLabelColor     = wagonColor
                )
            )
        }
    }
}

// ── Legend ─────────────────────────────────────────────────────────────────────

@Composable
private fun SeatLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendItem(SeatFreeColor,  "Свободно")
        LegendItem(SeatSelectedBg, "Выбрано")
        LegendItem(SeatOccupiedBg, "Занято")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Price hint ─────────────────────────────────────────────────────────────────

@Composable
private fun PriceHint(wagonType: WagonType, basePrice: Double) {
    val hints = when (wagonType) {
        WagonType.SV        -> listOf("СВ" to basePrice * 2.5)
        WagonType.COUPE     -> listOf("Нижняя" to basePrice * 2.0, "Верхняя" to basePrice * 1.6)
        WagonType.PLATZKART -> listOf("Нижняя" to basePrice * 1.3, "Верхняя" to basePrice * 0.85, "Боковая" to basePrice * 0.75)
        WagonType.SEAT      -> listOf("Место" to basePrice * 0.7)
    }
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f), shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            hints.forEach { (label, price) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${price.toInt()}₽", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ── SeatButton ─────────────────────────────────────────────────────────────────

@Composable
private fun SeatButton(
    number: Int,
    label: String,
    berthLabel: String = "",
    isOccupied: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.09f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "seatScale"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            isOccupied -> SeatOccupiedBg
            isSelected -> SeatSelectedBg
            else       -> SeatFreeColor
        },
        label = "seatBg"
    )
    val contentColor = when {
        isOccupied -> SeatOccupiedFg
        isSelected -> SeatSelectedFg
        else       -> SeatFreeFg
    }

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(if (!isOccupied) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.Center)
            if (berthLabel.isNotEmpty()) {
                Text(berthLabel, fontSize = 8.sp, color = contentColor.copy(alpha = 0.75f), textAlign = TextAlign.Center)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TRAIN SEAT MAPS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TrainSeatMap(
    seatRange: IntRange,
    occupiedSeats: List<Int>,
    selectedSeats: Set<Int>,
    wagonType: WagonType,
    wagonNumber: Int,
    basePrice: Double,
    onSeatClick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WagonInfoCard(wagonNumber = wagonNumber, wagonType = wagonType, basePrice = basePrice)
        when (wagonType) {
            WagonType.COUPE     -> CoupeSeatMap(seatRange, occupiedSeats, selectedSeats, basePrice, onSeatClick)
            WagonType.PLATZKART -> PlatzkartSeatMap(seatRange, occupiedSeats, selectedSeats, basePrice, onSeatClick)
            WagonType.SV        -> SvSeatMap(seatRange, occupiedSeats, selectedSeats, basePrice, onSeatClick)
            WagonType.SEAT      -> SeatCarMap(seatRange, occupiedSeats, selectedSeats, basePrice, onSeatClick)
        }
    }
}

// ── Купе: 2 столбца, разделитель-стена ────────────────────────────────────────

@Composable
private fun CoupeSeatMap(
    seatRange: IntRange,
    occupiedSeats: List<Int>,
    selectedSeats: Set<Int>,
    basePrice: Double,
    onSeatClick: (Int) -> Unit
) {
    val firstSeat    = seatRange.first
    val offset       = firstSeat - 1
    val compartments = (seatRange.count() + 3) / 4
    val cs           = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (comp in 1..compartments) {
            val s1 = firstSeat + (comp - 1) * 4
            val s2 = firstSeat + (comp - 1) * 4 + 1
            val s3 = firstSeat + (comp - 1) * 4 + 2
            val s4 = firstSeat + (comp - 1) * 4 + 3
            val coupeFree = listOf(s1, s2, s3, s4).count { it !in occupiedSeats && it <= seatRange.last }

            ElevatedCard(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(14.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    // ── Градиентный заголовок ──────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(listOf(cs.primary.copy(0.22f), cs.primaryContainer.copy(0.3f)))
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(color = cs.primary, shape = CircleShape, modifier = Modifier.size(26.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("$comp", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                Text("Купе", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = cs.primary)
                            }
                            Surface(
                                color = if (coupeFree == 0) cs.errorContainer else cs.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (coupeFree == 0) "Нет мест" else "$coupeFree св.",
                                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = if (coupeFree == 0) cs.onErrorContainer else cs.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ── Заголовки столбцов ─────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("нижн.", modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall, color = cs.secondary, fontWeight = FontWeight.Medium)
                        Text("верхн.", modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall, color = cs.tertiary, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(2.dp))
                        Text("нижн.", modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall, color = cs.secondary, fontWeight = FontWeight.Medium)
                        Text("верхн.", modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall, color = cs.tertiary, fontWeight = FontWeight.Medium)
                    }

                    // ── Места ─────────────────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (s1 <= seatRange.last)
                            SeatButton(s1, "${s1 - offset}", "", s1 in occupiedSeats, s1 in selectedSeats, Modifier.weight(1f)) { onSeatClick(s1) }
                        else Spacer(Modifier.weight(1f))
                        if (s2 <= seatRange.last)
                            SeatButton(s2, "${s2 - offset}", "", s2 in occupiedSeats, s2 in selectedSeats, Modifier.weight(1f)) { onSeatClick(s2) }
                        else Spacer(Modifier.weight(1f))
                        Box(modifier = Modifier.width(2.dp).height(48.dp).background(cs.outlineVariant))
                        if (s3 <= seatRange.last)
                            SeatButton(s3, "${s3 - offset}", "", s3 in occupiedSeats, s3 in selectedSeats, Modifier.weight(1f)) { onSeatClick(s3) }
                        else Spacer(Modifier.weight(1f))
                        if (s4 <= seatRange.last)
                            SeatButton(s4, "${s4 - offset}", "", s4 in occupiedSeats, s4 in selectedSeats, Modifier.weight(1f)) { onSeatClick(s4) }
                        else Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Плацкарт: РЖД-схема ───────────────────────────────────────────────────────

@Composable
private fun PlatzkartSeatMap(
    seatRange: IntRange,
    occupiedSeats: List<Int>,
    selectedSeats: Set<Int>,
    basePrice: Double,
    onSeatClick: (Int) -> Unit
) {
    val firstSeat = seatRange.first
    val offset    = firstSeat - 1

    // Основные: места 1–36 в вагоне (9 секций × 4 полки)
    // Боковые:  места 37–54 в вагоне (9 пар × 2 полки)

    val cs = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

        // ── Групповые заголовки ───────────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth().padding(start = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("нижние", modifier = Modifier.weight(2f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = cs.secondary)
            Text("верхние", modifier = Modifier.weight(2f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = cs.tertiary)
            Spacer(Modifier.width(14.dp))
            Text("бок. н.", modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = cs.secondary)
            Text("бок. в.", modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = cs.tertiary)
        }

        HorizontalDivider(color = cs.outlineVariant)

        // ── Ряды (секции) ─────────────────────────────────────────────────────
        for (row in 1..9) {
            val lowerA = firstSeat + (row - 1) * 4
            val lowerB = firstSeat + (row - 1) * 4 + 2
            val upperA = firstSeat + (row - 1) * 4 + 1
            val upperB = firstSeat + (row - 1) * 4 + 3
            val sideL  = firstSeat + 36 + (row - 1) * 2
            val sideU  = firstSeat + 36 + (row - 1) * 2 + 1
            val rowBg  = if (row % 2 == 0) cs.surfaceVariant.copy(0.2f) else Color.Transparent

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(rowBg)
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(modifier = Modifier.size(22.dp), color = cs.primary.copy(0.12f), shape = CircleShape) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$row", style = MaterialTheme.typography.labelSmall, color = cs.primary, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.width(2.dp))

                if (lowerA <= seatRange.last)
                    SeatButton(lowerA, "${lowerA - offset}", "", lowerA in occupiedSeats, lowerA in selectedSeats, Modifier.weight(1f)) { onSeatClick(lowerA) }
                else Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(2.dp))
                if (lowerB <= seatRange.last)
                    SeatButton(lowerB, "${lowerB - offset}", "", lowerB in occupiedSeats, lowerB in selectedSeats, Modifier.weight(1f)) { onSeatClick(lowerB) }
                else Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(2.dp))
                if (upperA <= seatRange.last)
                    SeatButton(upperA, "${upperA - offset}", "", upperA in occupiedSeats, upperA in selectedSeats, Modifier.weight(1f)) { onSeatClick(upperA) }
                else Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(2.dp))
                if (upperB <= seatRange.last)
                    SeatButton(upperB, "${upperB - offset}", "", upperB in occupiedSeats, upperB in selectedSeats, Modifier.weight(1f)) { onSeatClick(upperB) }
                else Spacer(Modifier.weight(1f))

                Spacer(Modifier.width(3.dp))
                Box(modifier = Modifier.width(2.dp).height(36.dp).background(cs.outlineVariant))
                Spacer(Modifier.width(3.dp))

                if (sideL <= seatRange.last)
                    SeatButton(sideL, "${sideL - offset}", "", sideL in occupiedSeats, sideL in selectedSeats, Modifier.weight(1.5f)) { onSeatClick(sideL) }
                else Spacer(Modifier.weight(1.5f))
                Spacer(Modifier.width(2.dp))
                if (sideU <= seatRange.last)
                    SeatButton(sideU, "${sideU - offset}", "", sideU in occupiedSeats, sideU in selectedSeats, Modifier.weight(1.5f)) { onSeatClick(sideU) }
                else Spacer(Modifier.weight(1.5f))
            }
        }
    }
}

// ── Информационная карточка вагона (все типы) ─────────────────────────────────

@Composable
private fun WagonInfoCard(wagonNumber: Int, wagonType: WagonType, basePrice: Double) {
    data class WagonMeta(
        val className: String,
        val classNum: String,
        val minPrice: Int,
        val hasAc: Boolean,
        val hasToilet: Boolean,
        val hasAnimals: Boolean,
        val hasFood: Boolean,
        val badgeColor: Color,
        val badgeOnColor: Color
    )

    val cs = MaterialTheme.colorScheme
    val meta = when (wagonType) {
        WagonType.SV -> WagonMeta(
            className    = "СВ",
            classNum     = "Класс 1",
            minPrice     = (basePrice * 2.5).toInt(),
            hasAc        = true,
            hasToilet    = true,
            hasAnimals   = false,
            hasFood      = true,
            badgeColor   = cs.tertiaryContainer,
            badgeOnColor = cs.onTertiaryContainer
        )
        WagonType.COUPE -> WagonMeta(
            className    = "Купе",
            classNum     = "Класс 2",
            minPrice     = (basePrice * 1.6).toInt(),
            hasAc        = true,
            hasToilet    = true,
            hasAnimals   = false,
            hasFood      = false,
            badgeColor   = cs.primaryContainer,
            badgeOnColor = cs.onPrimaryContainer
        )
        WagonType.PLATZKART -> WagonMeta(
            className    = "Плацкарт",
            classNum     = "Класс 3",
            minPrice     = (basePrice * 0.75).toInt(),
            hasAc        = true,
            hasToilet    = true,
            hasAnimals   = true,
            hasFood      = false,
            badgeColor   = cs.secondaryContainer,
            badgeOnColor = cs.onSecondaryContainer
        )
        WagonType.SEAT -> WagonMeta(
            className    = "Сидячий",
            classNum     = "Класс 3",
            minPrice     = (basePrice * 0.7).toInt(),
            hasAc        = false,
            hasToilet    = true,
            hasAnimals   = true,
            hasFood      = false,
            badgeColor   = cs.surfaceVariant,
            badgeOnColor = cs.onSurfaceVariant
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(0.6f),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // ── Номер вагона + класс + цена от ────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Train, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        "Вагон $wagonNumber",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(color = meta.badgeColor, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "${meta.className} · ${meta.classNum}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = meta.badgeOnColor
                        )
                    }
                }
                Text(
                    "от ${meta.minPrice} ₽",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }

            // ── Удобства ──────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                if (meta.hasAc)      AmenityItem(Icons.Default.AcUnit,    "Кондиционер")
                if (meta.hasToilet)  AmenityItem(Icons.Default.WaterDrop, "Биотуалет")
                if (meta.hasAnimals) AmenityItem(Icons.Default.Pets,      "Животные")
                if (!meta.hasAc)     AmenityItem(Icons.Default.AirlineSeatReclineNormal, "Без кондиционера")
            }

            // ── Перевозчик + питание ───────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Business, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("ФПК", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Restaurant, null, Modifier.size(12.dp),
                        tint = if (meta.hasFood) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (meta.hasFood) "Питание включено" else "Питание: нет",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (meta.hasFood) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AmenityItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.secondary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── СВ ────────────────────────────────────────────────────────────────────────

@Composable
private fun SvSeatMap(
    seatRange: IntRange,
    occupiedSeats: List<Int>,
    selectedSeats: Set<Int>,
    basePrice: Double,
    onSeatClick: (Int) -> Unit
) {
    val firstSeat    = seatRange.first
    val compartments = (seatRange.count() + 1) / 2
    val offset       = firstSeat - 1

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color    = MaterialTheme.colorScheme.tertiaryContainer.copy(0.6f),
            shape    = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier              = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                Column {
                    Text("Люкс-класс · СВ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    Text("Два широких дивана · ${(basePrice * 2.5).toInt()} ₽ за место", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
        for (comp in 1..compartments) {
            val s1 = firstSeat + (comp - 1) * 2
            val s2 = firstSeat + (comp - 1) * 2 + 1
            ElevatedCard(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(14.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    val free = listOf(s1, s2).count { it !in occupiedSeats && it <= seatRange.last }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(0.22f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(0.3f)
                                ))
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = MaterialTheme.colorScheme.tertiary, shape = CircleShape, modifier = Modifier.size(26.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("$comp", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                Text("Купе · СВ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Surface(
                                color = if (free == 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (free == 0) "Занято" else "$free св.",
                                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = if (free == 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (s1 <= seatRange.last)
                            SeatButton(s1, "${s1 - offset}", "", s1 in occupiedSeats, s1 in selectedSeats, Modifier.weight(1f)) { onSeatClick(s1) }
                        else Spacer(Modifier.weight(1f))
                        Box(Modifier.width(1.dp).height(48.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        if (s2 <= seatRange.last)
                            SeatButton(s2, "${s2 - offset}", "", s2 in occupiedSeats, s2 in selectedSeats, Modifier.weight(1f)) { onSeatClick(s2) }
                        else Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Сидячий вагон ─────────────────────────────────────────────────────────────

@Composable
private fun SeatCarMap(
    seatRange: IntRange,
    occupiedSeats: List<Int>,
    selectedSeats: Set<Int>,
    basePrice: Double,
    onSeatClick: (Int) -> Unit
) {
    val firstSeat = seatRange.first
    val numSeats  = seatRange.count()
    val rowCount  = (numSeats + 3) / 4

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(28.dp))
            listOf("А", "Б", "", "В", "Г").forEach { label ->
                if (label.isEmpty()) Spacer(Modifier.width(18.dp))
                else Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        for (row in 1..rowCount) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$row", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                for (col in 0..3) {
                    if (col == 2) Spacer(Modifier.width(18.dp))
                    val seat = firstSeat + (row - 1) * 4 + col
                    if (seat <= seatRange.last)
                        SeatButton(seat, "${(row - 1) * 4 + col + 1}", "", seat in occupiedSeats, seat in selectedSeats, Modifier.weight(1f)) { onSeatClick(seat) }
                    else Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PLANE SEAT MAP  — бизнес 2+2 / премиум+эконом 3+3
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PlaneSeatMap(
    totalSeats: Int,
    occupiedSeats: List<Int>,
    selectedSeats: Set<Int>,
    basePrice: Double,
    onSeatClick: (Int) -> Unit
) {
    val bizRowCount  = 2
    val bizSeats     = bizRowCount * 4       // 8
    val premRowCount = 8
    val premSeats    = premRowCount * 6      // 48
    val premEnd      = bizSeats + premSeats  // 56
    val econSeats    = maxOf(0, totalSeats - premEnd)
    val econRowCount = (econSeats + 5) / 6
    val bizCols      = listOf("A", "B", "C", "D")
    val stdCols      = listOf("A", "B", "C", "D", "E", "F")
    val cs           = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ── Класс-легенда ─────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ClassBadge(cs.tertiaryContainer,           "Бизнес",  "${(basePrice * 2.0).toInt()} ₽", Modifier.weight(1f))
            ClassBadge(cs.secondaryContainer.copy(0.7f), "Премиум", "${(basePrice * 1.3).toInt()} ₽", Modifier.weight(1f))
            ClassBadge(cs.surfaceVariant.copy(0.5f),   "Эконом",  "${basePrice.toInt()} ₽",          Modifier.weight(1f))
        }

        // ── Бизнес (2+2) ─────────────────────────────────────────────────────
        Surface(color = cs.tertiaryContainer.copy(0.35f), shape = RoundedCornerShape(10.dp)) {
            Column(
                modifier            = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(28.dp))
                    bizCols.forEachIndexed { i, lbl ->
                        if (i == 2) Spacer(Modifier.width(20.dp))
                        Text(lbl, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                            color = cs.tertiary)
                    }
                }
                for (row in 1..bizRowCount) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("$row", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        for (col in 0..3) {
                            if (col == 2) Spacer(Modifier.width(20.dp))
                            val seat = (row - 1) * 4 + col + 1
                            if (seat <= totalSeats)
                                SeatButton(seat, bizCols[col], "", seat in occupiedSeats, seat in selectedSeats, Modifier.weight(1f)) { onSeatClick(seat) }
                            else
                                Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ── Премиум (3+3) ─────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(28.dp))
                stdCols.forEachIndexed { i, lbl ->
                    if (i == 3) Spacer(Modifier.width(20.dp))
                    Text(lbl, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        color = cs.secondary)
                }
            }
            for (row in 1..premRowCount) {
                val displayRow = bizRowCount + row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(cs.secondaryContainer.copy(0.2f))
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$displayRow", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    for (col in 0..5) {
                        if (col == 3) Spacer(Modifier.width(20.dp))
                        val seat = bizSeats + (row - 1) * 6 + col + 1
                        if (seat <= totalSeats)
                            SeatButton(seat, stdCols[col], "", seat in occupiedSeats, seat in selectedSeats, Modifier.weight(1f)) { onSeatClick(seat) }
                        else
                            Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Аварийный выход ───────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = cs.error.copy(0.5f), thickness = 1.dp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ExitToApp, null, Modifier.size(12.dp), tint = cs.error)
            Spacer(Modifier.width(4.dp))
            Text("Аварийный выход", style = MaterialTheme.typography.labelSmall, color = cs.error, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            HorizontalDivider(Modifier.weight(1f), color = cs.error.copy(0.5f), thickness = 1.dp)
        }

        // ── Эконом (3+3) ──────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (row in 1..econRowCount) {
                val displayRow = bizRowCount + premRowCount + row
                val isLastRow  = row == econRowCount
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$displayRow", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    for (col in 0..5) {
                        if (col == 3) Spacer(Modifier.width(20.dp))
                        val seat = premEnd + (row - 1) * 6 + col + 1
                        if (seat <= totalSeats)
                            SeatButton(seat, stdCols[col], "", seat in occupiedSeats, seat in selectedSeats, Modifier.weight(1f)) { onSeatClick(seat) }
                        else
                            Spacer(Modifier.weight(1f))
                    }
                }
                if (isLastRow) {
                    Text("Спинки не откидываются",
                        modifier  = Modifier.fillMaxWidth(),
                        style     = MaterialTheme.typography.labelSmall,
                        color     = cs.error,
                        textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun ClassBadge(color: Color, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.padding(1.dp), color = color, shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title,    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,  color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// BUS SEAT MAP  — 4 места в ряду (A B | C D)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun BusSeatMap(
    totalSeats: Int,
    occupiedSeats: List<Int>,
    selectedSeats: Set<Int>,
    basePrice: Double,
    onSeatClick: (Int) -> Unit
) {
    val rowCount = (totalSeats + 3) / 4

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

        // ── Заголовки столбцов ────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(32.dp))
            listOf("A", "B", "", "C", "D").forEach { label ->
                if (label.isEmpty()) Spacer(Modifier.width(16.dp))
                else Text(
                    label,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center,
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // ── Ряды ─────────────────────────────────────────────────────────────
        for (row in 1..rowCount) {
            val isLastRow = row == rowCount

            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "$row",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(4.dp))

                for (col in 0..3) {
                    if (col == 2) Spacer(Modifier.width(16.dp))
                    val seat = (row - 1) * 4 + col + 1
                    if (seat <= totalSeats) {
                        SeatButton(seat, "$seat", "", seat in occupiedSeats, seat in selectedSeats, Modifier.weight(1f)) { onSeatClick(seat) }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (isLastRow) {
                Text(
                    "Спинки не откидываются",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.error,
                    modifier  = Modifier.fillMaxWidth().padding(top = 2.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Bottom bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseBottomBar(
    selectedSeats: Set<Int>,
    totalPrice: Double,
    transportType: String,
    seatRange: IntRange,
    onRemoveSeat: (Int) -> Unit,
    onContinue: () -> Unit
) {
    Surface(shadowElevation = 12.dp) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedSeats.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(selectedSeats.sorted().toList()) { seat ->
                        val displayNum = if (transportType == "train") seat - seatRange.first + 1 else seat
                        InputChip(
                            selected     = true,
                            onClick      = { onRemoveSeat(seat) },
                            label        = { Text("М $displayNum", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) }
                        )
                    }
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("${selectedSeats.size} ${seatWord(selectedSeats.size)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${totalPrice.toInt()} ₽", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            val enabled     = selectedSeats.isNotEmpty()
            val buttonLabel = when {
                !enabled               -> "Выберите место"
                transportType == "bus" -> "Купить"
                else                   -> "Продолжить"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (enabled)
                            Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                        else
                            Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                    )
                    .clickable(enabled = enabled, onClick = onContinue),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (enabled) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.EventSeat,
                        null,
                        tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        buttonLabel,
                        color      = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun seatWord(count: Int) = when {
    count % 10 == 1 && count % 100 != 11 -> "место"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "места"
    else -> "мест"
}
