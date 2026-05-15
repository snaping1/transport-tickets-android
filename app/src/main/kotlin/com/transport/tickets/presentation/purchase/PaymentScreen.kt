package com.transport.tickets.presentation.purchase

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.transport.tickets.presentation.routes.transportIcon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private object CardVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val spaced = digits.chunked(4).joinToString(" ")
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                (offset + offset / 4).coerceAtMost(spaced.length)
            override fun transformedToOriginal(offset: Int): Int =
                (offset - offset / 5).coerceAtMost(digits.length)
        }
        return TransformedText(AnnotatedString(spaced), mapping)
    }
}

private object ExpiryVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = if (digits.length >= 2) "${digits.take(2)}/${digits.drop(2)}" else digits
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 2) offset else (offset + 1).coerceAtMost(formatted.length)
            override fun transformedToOriginal(offset: Int): Int =
                if (offset <= 2) offset else (offset - 1).coerceAtMost(digits.length)
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onPaymentSuccess: (ticketId: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val paymentState by viewModel.paymentState.collectAsState()
    val route = viewModel.route
    val seats = viewModel.seats
    val totalPrice = viewModel.totalPrice

    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    val isProcessing = paymentState is PaymentUiState.Checking || paymentState is PaymentUiState.Processing

    LaunchedEffect(paymentState) {
        if (paymentState is PaymentUiState.Success) {
            onPaymentSuccess((paymentState as PaymentUiState.Success).ticketId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Оплата") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isProcessing) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = isProcessing,
            label = "payment_content"
        ) { processing ->
            if (processing) {
                ProcessingAnimation(state = paymentState)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (paymentState is PaymentUiState.Error) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                Column {
                                    Text("Ошибка оплаты", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text(
                                        (paymentState as PaymentUiState.Error).message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    if (route != null) {
                        OrderSummaryCard(route = route, seats = seats, totalPrice = totalPrice)
                    }

                    FakeCardForm(
                        cardNumber = cardNumber,
                        expiry = expiry,
                        cvv = cvv,
                        onCardNumberChange = { if (it.length <= 16) cardNumber = it },
                        onExpiryChange = { if (it.length <= 4) expiry = it },
                        onCvvChange = { if (it.length <= 3) cvv = it }
                    )

                    val cardValid = cardNumber.length == 16 && expiry.length == 4 && cvv.length == 3
                    Button(
                        onClick = viewModel::pay,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = cardValid,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Lock, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Оплатить ${totalPrice.toInt()} ₽",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "Платёж защищён. Данные карты не хранятся.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProcessingAnimation(state: PaymentUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            text = when (state) {
                is PaymentUiState.Checking -> "Проверка бронирования..."
                is PaymentUiState.Processing -> "Обработка платежа..."
                else -> ""
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Пожалуйста, не закрывайте экран",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OrderSummaryCard(
    route: com.transport.tickets.domain.model.Route,
    seats: List<Int>,
    totalPrice: Double
) {
    val fmt = DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.systemDefault())
    val (tIcon, tLabel) = transportIcon(route.transportType)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Заказ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(route.originCity, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        runCatching { fmt.format(Instant.parse(route.departureTime)) }.getOrDefault(""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(tIcon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(route.destinationCity, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        runCatching { fmt.format(Instant.parse(route.arrivalTime)) }.getOrDefault(""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Мест: ${seats.size}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Места: ${seats.sorted().joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Итого к оплате", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${totalPrice.toInt()} ₽",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FakeCardForm(
    cardNumber: String,
    expiry: String,
    cvv: String,
    onCardNumberChange: (String) -> Unit,
    onExpiryChange: (String) -> Unit,
    onCvvChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CreditCard, null, tint = MaterialTheme.colorScheme.primary)
                Text("Данные карты", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }

            OutlinedTextField(
                value = cardNumber,
                onValueChange = { onCardNumberChange(it.filter(Char::isDigit).take(16)) },
                label = { Text("Номер карты") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = CardVisualTransformation,
                singleLine = true,
                placeholder = { Text("0000 0000 0000 0000") },
                leadingIcon = { Icon(Icons.Default.CreditCard, null) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = expiry,
                    onValueChange = { onExpiryChange(it.filter(Char::isDigit).take(4)) },
                    label = { Text("ММ/ГГ") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ExpiryVisualTransformation,
                    singleLine = true,
                    placeholder = { Text("ММ/ГГ") }
                )
                OutlinedTextField(
                    value = cvv,
                    onValueChange = { onCvvChange(it.filter(Char::isDigit).take(3)) },
                    label = { Text("CVV") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    placeholder = { Text("•••") }
                )
            }
        }
    }
}
