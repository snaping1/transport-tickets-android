package com.transport.tickets.presentation.profile

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.domain.model.Ticket
import com.transport.tickets.presentation.routes.transportIcon
import com.transport.tickets.presentation.tickets.MyTicketsUiState
import com.transport.tickets.presentation.tickets.MyTicketsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.transport.tickets.presentation.util.DateVisualTransformation
import com.transport.tickets.presentation.util.PhoneVisualTransformation
import com.transport.tickets.presentation.util.extractDateDigits
import com.transport.tickets.presentation.util.extractPhoneDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onTicketClick: (Int) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    ticketsViewModel: MyTicketsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val ticketsState by ticketsViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var qrTicket by remember { mutableStateOf<Ticket?>(null) }
    var ticketToCancel by remember { mutableStateOf<Ticket?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            viewModel.setAvatarUri(it.toString())
        }
    }

    qrTicket?.let { ticket ->
        QrDialog(ticket = ticket, onDismiss = { qrTicket = null })
    }

    ticketToCancel?.let { ticket ->
        AlertDialog(
            onDismissRequest = { ticketToCancel = null },
            title = { Text("Отмена билета") },
            text = { Text("Отменить билет? Средства будут возвращены.") },
            confirmButton = {
                Button(onClick = { ticketsViewModel.cancelTicket(ticket.id); ticketToCancel = null }) {
                    Text("Отменить билет")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { ticketToCancel = null }) { Text("Нет") }
            }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Вы уверены, что хотите выйти из аккаунта?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.signOut(); onSignOut(); showSignOutConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Выйти") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSignOutConfirm = false }) { Text("Отмена") }
            }
        )
    }

    if (uiState.passwordResetSent) {
        AlertDialog(
            onDismissRequest = viewModel::resetPasswordResetState,
            title = { Text("Письмо отправлено") },
            text = { Text("Проверьте почту ${uiState.profile.email} для сброса пароля.") },
            confirmButton = { TextButton(onClick = viewModel::resetPasswordResetState) { Text("OK") } }
        )
    }
    if (uiState.showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearCache,
            title = { Text("Очистить кэш?") },
            text = { Text("Данные маршрутов и билетов будут удалены из локального хранилища.") },
            confirmButton = {
                Button(
                    onClick = viewModel::clearCache,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Очистить") }
            },
            dismissButton = { OutlinedButton(onClick = viewModel::dismissClearCache) { Text("Отмена") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Личный кабинет") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f)
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ─── Hero Header ───
            HeroHeader(
                state = uiState,
                onAvatarClick = { avatarLauncher.launch("image/*") }
            )

            // ─── Stats Row ───
            StatsRow(
                tickets = ticketsState.tickets,
                passengersCount = uiState.savedPassengers.size
            )

            // ─── Tab Row ───
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Билеты") },
                    icon = { Icon(Icons.Default.ConfirmationNumber, null, Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Профиль") },
                    icon = { Icon(Icons.Default.Person, null, Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    text = { Text("Настройки") },
                    icon = { Icon(Icons.Default.Settings, null, Modifier.size(18.dp)) }
                )
            }

            // ─── Tab Content ───
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> TicketsTab(
                        state = ticketsState,
                        onRefresh = ticketsViewModel::refresh,
                        onCancelClick = { ticketToCancel = it },
                        onQrClick = { qrTicket = it },
                        onTicketClick = onTicketClick,
                        onNavigateToSearch = onNavigateToSearch
                    )
                    1 -> ProfileTab(
                        state = uiState,
                        viewModel = viewModel,
                        onSignOut = { showSignOutConfirm = true }
                    )
                    2 -> SettingsTab(state = uiState, viewModel = viewModel)
                }
            }
        }
    }
}

// ─── HERO HEADER ─────────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(state: ProfileUiState, onAvatarClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(primaryContainer.copy(alpha = 0.8f), surface)
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                if (state.avatarUri != null) {
                    AsyncImage(
                        model = state.avatarUri,
                        contentDescription = "Аватар",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.dp, primary, CircleShape)
                            .clickable(onClick = onAvatarClick)
                    )
                } else {
                    val initials = state.profile.displayName
                        .split(" ")
                        .filter { it.isNotEmpty() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }
                        .ifEmpty { state.profile.email.firstOrNull()?.uppercase() ?: "?" }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(primary)
                            .clickable(onClick = onAvatarClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initials,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                SmallFloatingActionButton(
                    onClick = onAvatarClick,
                    modifier = Modifier.size(26.dp),
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        Icons.Default.CameraAlt, null,
                        Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }

            // Name + email
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = state.profile.displayName.ifEmpty { "Пользователь" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (state.profile.email.isNotEmpty()) {
                    Text(
                        text = state.profile.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.profile.phone.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone, null,
                            Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            state.profile.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─── STATS ROW ───────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(tickets: List<Ticket>, passengersCount: Int) {
    val activeCount = tickets.count { it.status == "active" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ConfirmationNumber,
            value = tickets.size.toString(),
            label = "Поездок"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CheckCircle,
            value = activeCount.toString(),
            label = "Активных"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.People,
            value = passengersCount.toString(),
            label = "Пассажиров"
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── ВКЛАДКА: БИЛЕТЫ ─────────────────────────────────────────────────────────

@Composable
private fun TicketsTab(
    state: MyTicketsUiState,
    onRefresh: () -> Unit,
    onCancelClick: (Ticket) -> Unit,
    onQrClick: (Ticket) -> Unit,
    onTicketClick: (Int) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val fmt = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale("ru")).withZone(ZoneId.systemDefault())
    }
    val sortedTickets = remember(state.tickets) {
        state.tickets.sortedWith(
            compareByDescending<Ticket> { it.status == "active" }.thenByDescending { it.createdAt }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!state.isLoading && sortedTickets.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.ConfirmationNumber, null,
                    Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "У вас пока нет билетов",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Найдите маршрут и купите первый билет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onNavigateToSearch) { Text("Найти маршрут") }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedTickets, key = { it.id }) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        isCancelling = state.cancellingTicketId == ticket.id,
                        fmt = fmt,
                        onCancelClick = { onCancelClick(ticket) },
                        onQrClick = { onQrClick(ticket) },
                        onTicketClick = { onTicketClick(ticket.id) }
                    )
                }
            }
        }
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun TicketCard(
    ticket: Ticket,
    isCancelling: Boolean,
    fmt: DateTimeFormatter,
    onCancelClick: () -> Unit,
    onQrClick: () -> Unit,
    onTicketClick: () -> Unit
) {
    val isActive = ticket.status == "active"
    val isUsed = ticket.route?.let {
        runCatching { Instant.parse(it.departureTime).isBefore(Instant.now()) }.getOrDefault(false)
    } ?: false

    val statusText = when {
        ticket.status == "cancelled" -> "Отменён"
        isUsed -> "Использован"
        else -> "Активен"
    }
    val statusBg = when {
        ticket.status == "cancelled" -> MaterialTheme.colorScheme.errorContainer
        isUsed -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val statusFg = when {
        ticket.status == "cancelled" -> MaterialTheme.colorScheme.onErrorContainer
        isUsed -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val departureStr = ticket.route?.let { runCatching { fmt.format(Instant.parse(it.departureTime)) }.getOrNull() }
    val arrivalStr = ticket.route?.let { runCatching { fmt.format(Instant.parse(it.arrivalTime)) }.getOrNull() }
    val createdStr = runCatching { fmt.format(Instant.parse(ticket.createdAt)) }.getOrNull()

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTicketClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.route?.let { "${it.originCity} → ${it.destinationCity}" }
                        ?: "Маршрут #${ticket.routeId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(shape = MaterialTheme.shapes.small, color = statusBg) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusFg
                    )
                }
            }

            ticket.route?.let { route ->
                val (tIcon, tLabel) = transportIcon(route.transportType)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(tIcon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        tLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (departureStr != null) {
                Text("Отправление: $departureStr", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (arrivalStr != null) {
                Text("Прибытие: $arrivalStr", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Мест: ${ticket.seatCount}", style = MaterialTheme.typography.bodyMedium)
                    if (ticket.seatNumbers.isNotEmpty()) {
                        Text(
                            "Места: ${ticket.seatNumbers.sorted().joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    "${ticket.totalPrice.toInt()} ₽",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (createdStr != null) {
                Text(
                    "Куплен: $createdStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isActive) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        enabled = !isCancelling,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Отменить")
                        }
                    }
                    Button(onClick = onQrClick, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.QrCode, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("QR-код")
                    }
                }
            }
        }
    }
}

// ─── ВКЛАДКА: ПРОФИЛЬ ────────────────────────────────────────────────────────

@Composable
private fun ProfileTab(
    state: ProfileUiState,
    viewModel: ProfileViewModel,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ─── Personal data card ───
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Person, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Личные данные",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = state.editName,
                    onValueChange = viewModel::setEditName,
                    label = { Text("ФИО") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Badge, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                var phoneRaw by remember { mutableStateOf(extractPhoneDigits(state.editPhone)) }
                var dateRaw by remember { mutableStateOf(extractDateDigits(state.editBirthDate)) }

                LaunchedEffect(state.profile) {
                    val newPhone = extractPhoneDigits(state.editPhone)
                    val newDate = extractDateDigits(state.editBirthDate)
                    if (phoneRaw.isEmpty() && newPhone.isNotEmpty()) phoneRaw = newPhone
                    if (dateRaw.isEmpty() && newDate.isNotEmpty()) dateRaw = newDate
                }

                OutlinedTextField(
                    value = phoneRaw,
                    onValueChange = { input ->
                        val raw = extractPhoneDigits(input)
                        phoneRaw = raw
                        viewModel.setEditPhone(raw)
                    },
                    label = { Text("Телефон") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    placeholder = { Text("+7 (___) ___-__-__") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    visualTransformation = PhoneVisualTransformation,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = dateRaw,
                    onValueChange = { input ->
                        val raw = extractDateDigits(input)
                        dateRaw = raw
                        viewModel.setEditBirthDate(raw)
                    },
                    label = { Text("Дата рождения") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                    placeholder = { Text("дд.мм.гггг") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = DateVisualTransformation,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (state.profileError != null) {
                    Text(state.profileError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (state.profileSaved) {
                    Text(
                        "✓ Изменения сохранены",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = viewModel::saveProfile,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.profileSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.profileSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранить изменения")
                    }
                }
            }
        }

        // ─── Пассажиры ───
        PassengersSection(
            passengers = state.savedPassengers,
            onDelete = viewModel::deletePassenger
        )

        // ─── Account actions card ───
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ManageAccounts, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Аккаунт",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = viewModel::sendPasswordReset,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.passwordResetLoading && state.profile.email.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.passwordResetLoading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Lock, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Сменить пароль")
                    }
                }

                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Выйти из аккаунта")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ─── PASSENGERS SECTION ───────────────────────────────────────────────────────

@Composable
private fun PassengersSection(passengers: List<Passenger>, onDelete: (Passenger) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.People, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Мои пассажиры",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (passengers.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            passengers.size.toString(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider()

            if (passengers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.PersonAdd, null,
                        Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "Нет сохранённых пассажиров",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Пассажиры появятся здесь\nпосле покупки билетов",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                passengers.forEach { passenger ->
                    PassengerCard(passenger = passenger, onDelete = { onDelete(passenger) })
                }
            }
        }
    }
}

@Composable
private fun PassengerCard(passenger: Passenger, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить пассажира?") },
            text = { Text("Удалить ${passenger.fullName} из списка?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with initial
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    passenger.lastName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            // Name + document
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    passenger.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Badge, null,
                        Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${passenger.documentLabel}: ${passenger.documentFull}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (passenger.birthDate.isNotEmpty()) {
                    Text(
                        passenger.birthDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete, null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── ВКЛАДКА: НАСТРОЙКИ ──────────────────────────────────────────────────────

@Composable
private fun SettingsTab(state: ProfileUiState, viewModel: ProfileViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Appearance
        SettingsGroupCard(title = "Внешний вид", icon = Icons.Default.Palette) {
            SettingsToggleRow(
                icon = if (state.settings.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                title = "Тёмная тема",
                subtitle = if (state.settings.isDarkTheme) "Включена" else "Выключена",
                checked = state.settings.isDarkTheme,
                onToggle = viewModel::toggleDarkTheme
            )
        }

        // System
        SettingsGroupCard(title = "Система", icon = Icons.Default.Tune) {
            SettingsToggleRow(
                icon = Icons.Default.Notifications,
                title = "Уведомления",
                subtitle = if (state.settings.notificationsEnabled) "Включены" else "Выключены",
                checked = state.settings.notificationsEnabled,
                onToggle = viewModel::toggleNotifications
            )
        }

        // Data
        SettingsGroupCard(title = "Данные и хранилище", icon = Icons.Default.Storage) {
            SettingsClickRow(
                icon = Icons.Default.DeleteSweep,
                title = "Очистить кэш",
                subtitle = "Маршруты и билеты из локального хранилища",
                onClick = viewModel::confirmClearCache
            )
            if (state.cacheCleared) {
                Text(
                    "✓ Кэш очищен",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
                LaunchedEffect(Unit) { viewModel.resetCacheClearedState() }
            }
        }

        // Support
        SettingsGroupCard(title = "Поддержка", icon = Icons.Default.HelpOutline) {
            SectionInfoRow(
                icon = Icons.Default.HelpOutline,
                title = "Поддержка и ЧаВо",
                subtitle = "support@transport.app"
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionInfoRow(
                icon = Icons.Default.Info,
                title = "О приложении",
                subtitle = "Версия 1.0 • Transport Tickets"
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SettingsClickRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionInfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── QR ДИАЛОГ ───────────────────────────────────────────────────────────────

@Composable
private fun QrDialog(ticket: Ticket, onDismiss: () -> Unit) {
    val content = "ticket:${ticket.id}:route:${ticket.routeId}:seats:${ticket.seatNumbers.joinToString(",")}"
    val qrBitmap = remember(ticket.id) { generateQrBitmap(content) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Электронный билет", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR код билета",
                        modifier = Modifier.size(220.dp).clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCode, null, Modifier.size(120.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
