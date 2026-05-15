package com.transport.tickets.presentation.tickets

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.domain.model.Route
import com.transport.tickets.domain.model.Ticket
import com.transport.tickets.presentation.routes.transportIcon
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    onBack: () -> Unit,
    viewModel: TicketDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQr by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    val ticket = uiState.ticket

    if (showQr && ticket != null) {
        QrCodeDialog(ticket = ticket, onDismiss = { showQr = false })
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Вернуть билет?") },
            text = { Text("Вы уверены, что хотите вернуть билет? Средства будут зачислены на карту в течение 3–5 рабочих дней.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.cancelTicket(); showCancelDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Вернуть билет") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Билет") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (ticket == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TicketStatusBadge(ticket)

                ticket.route?.let { route ->
                    RouteTimelineCard(route = route)
                }

                if (ticket.seatNumbers.isNotEmpty()) {
                    SeatsPassengersCard(
                        seatNumbers = ticket.seatNumbers,
                        passengers = uiState.passengers,
                        transportType = ticket.route?.transportType ?: "train"
                    )
                }

                PriceCard(ticket = ticket)

                if (uiState.error != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            uiState.error!!,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Button(
                    onClick = { showQr = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCode2, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Показать QR-код для посадки")
                }

                if (ticket.status == "active" && !uiState.cancelled) {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.cancelling,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        if (uiState.cancelling) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Вернуть билет")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TicketStatusBadge(ticket: Ticket) {
    val isActive = ticket.status == "active"
    val isUsed = ticket.route?.let {
        runCatching { Instant.parse(it.departureTime).isBefore(Instant.now()) }.getOrDefault(false)
    } ?: false

    val (statusText, statusColor, statusBg) = when {
        ticket.status == "cancelled" -> Triple(
            "Возвращён",
            MaterialTheme.colorScheme.onErrorContainer,
            MaterialTheme.colorScheme.errorContainer
        )
        isUsed -> Triple(
            "Использован",
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant
        )
        else -> Triple(
            "Активен",
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.primaryContainer
        )
    }

    Card(colors = CardDefaults.cardColors(containerColor = statusBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (ticket.status == "active" && !isUsed) Icons.Default.ConfirmationNumber else Icons.Default.CheckCircle,
                    null,
                    tint = statusColor
                )
                Column {
                    Text("Билет #${ticket.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = statusColor)
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                }
            }
            val createdFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
            Text(
                runCatching { createdFmt.format(Instant.parse(ticket.createdAt)) }.getOrDefault(""),
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
        }
    }
}

@Composable
private fun RouteTimelineCard(route: Route) {
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val dateFmt = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru")).withZone(ZoneId.systemDefault())
    val (tIcon, tLabel) = transportIcon(route.transportType)

    val departure = runCatching { Instant.parse(route.departureTime) }.getOrNull()
    val arrival = runCatching { Instant.parse(route.arrivalTime) }.getOrNull()
    val durationStr = if (departure != null && arrival != null) {
        val dur = Duration.between(departure, arrival)
        val h = dur.toHours()
        val m = dur.toMinutes() % 60
        if (h > 0) "${h}ч ${m}м" else "${m}м"
    } else ""

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(tIcon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(tLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        departure?.let { timeFmt.format(it) } ?: "—",
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp)),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        departure?.let { dateFmt.format(it) } ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(route.originCity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        durationStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    Icon(tIcon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        arrival?.let { timeFmt.format(it) } ?: "—",
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp)),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        arrival?.let { dateFmt.format(it) } ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(route.destinationCity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SeatsPassengersCard(
    seatNumbers: List<Int>,
    passengers: List<Pair<Int, Passenger>>,
    transportType: String
) {
    val passengerMap = passengers.toMap()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Пассажиры",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            seatNumbers.sorted().forEach { seat ->
                val passenger = passengerMap[seat]
                val berthLabel = if (transportType == "train") {
                    com.transport.tickets.presentation.purchase.PricingUtils.trainBerthLabel(seat)
                } else ""

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "Место $seat",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (berthLabel.isNotEmpty()) {
                                Text(berthLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        if (passenger != null) {
                            Text(passenger.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            if (passenger.documentNumber.isNotBlank()) {
                                Text(
                                    "${passenger.documentLabel}: ${passenger.documentFull}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (passenger.birthDate.isNotBlank()) {
                                Text(
                                    "Дата рождения: ${passenger.birthDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                "Данные пассажира не указаны",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceCard(ticket: Ticket) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Стоимость", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "${ticket.seatCount} ${if (ticket.seatCount == 1) "место" else "места"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Text(
                "${ticket.totalPrice.toInt()} ₽",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun QrCodeDialog(ticket: Ticket, onDismiss: () -> Unit) {
    val content = "ticket:${ticket.id}:route:${ticket.routeId}:seats:${ticket.seatNumbers.sorted().joinToString(",")}"
    val qrBitmap = remember(ticket.id) { generateQrBitmap(content) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("QR-код для посадки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR код",
                        modifier = Modifier.size(240.dp).clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCode2, null, Modifier.size(120.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Билет #${ticket.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    ticket.route?.let {
                        Text(
                            "${it.originCity} → ${it.destinationCity}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (ticket.seatNumbers.isNotEmpty()) {
                        Text(
                            "Места: ${ticket.seatNumbers.sorted().joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    "Предъявите QR-код при посадке",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
            }
        }
    }
}

private fun generateQrBitmap(content: String): Bitmap? = runCatching {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
    val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
    for (x in 0 until 512) {
        for (y in 0 until 512) {
            bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    bmp
}.getOrNull()
