package com.transport.tickets.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.presentation.util.DateVisualTransformation
import com.transport.tickets.presentation.util.PhoneVisualTransformation
import com.transport.tickets.presentation.util.extractDateDigits
import com.transport.tickets.presentation.util.extractPhoneDigits
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

// ─── Helpers ────────────────────────────────────────────────────────────────

private fun fmtPhone(d: String) = buildString {
    if (d.isEmpty()) return ""
    append("+7")
    if (d.isNotEmpty()) append(" (${d.take(3)}")
    if (d.length >= 3) append(") ${d.substring(3, minOf(6, d.length))}")
    if (d.length > 6) append("-${d.substring(6, minOf(8, d.length))}")
    if (d.length > 8) append("-${d.substring(8)}")
}

private fun fmtDate(d: String) = buildString {
    val s = d.filter { it.isDigit() }
    if (s.length >= 2) append("${s.take(2)}.") else { append(s); return@buildString }
    if (s.length >= 4) append("${s.substring(2, 4)}.") else { append(s.drop(2)); return@buildString }
    append(s.drop(4))
}

private fun fmtPassport(raw: String): String {
    val d = raw.filter { it.isDigit() }.take(10)
    return if (d.length >= 4) "${d.take(4)} ${d.drop(4)}" else d
}

private enum class EditField { Name, Phone, BirthDate, Passport }

// ─── Main Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onTicketClick: (Int) -> Unit = {},
    showBackButton: Boolean = true,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSignOut by remember { mutableStateOf(false) }
    var showAddCard by remember { mutableStateOf(false) }
    var editField by remember { mutableStateOf<EditField?>(null) }
    var editPassenger by remember { mutableStateOf<Passenger?>(null) }
    var showAddPassenger by remember { mutableStateOf(false) }
    var deletePassenger by remember { mutableStateOf<Passenger?>(null) }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try { context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: Exception) {}
            viewModel.setAvatarUri(it.toString())
        }
    }

    // Snackbar on cache clear
    LaunchedEffect(uiState.cacheCleared) {
        if (uiState.cacheCleared) {
            snackbarHostState.showSnackbar("Кэш успешно очищен")
            viewModel.resetCacheClearedState()
        }
    }

    // ─ Dialogs ─
    if (showSignOut) AlertDialog(
        onDismissRequest = { showSignOut = false },
        title = { Text("Выйти из аккаунта?") },
        confirmButton = {
            Button(onClick = { viewModel.signOut(); onSignOut(); showSignOut = false },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Выйти") }
        },
        dismissButton = { OutlinedButton(onClick = { showSignOut = false }) { Text("Отмена") } }
    )

    if (uiState.showClearCacheConfirm) AlertDialog(
        onDismissRequest = viewModel::dismissClearCache,
        title = { Text("Очистить кэш?") },
        text = { Text("Данные маршрутов и билетов будут удалены.") },
        confirmButton = {
            Button(onClick = viewModel::clearCache,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Очистить") }
        },
        dismissButton = { OutlinedButton(onClick = viewModel::dismissClearCache) { Text("Отмена") } }
    )

    editField?.let { field ->
        EditFieldDialog(field = field, state = uiState, viewModel = viewModel, onDismiss = { editField = null; viewModel.resetSaveState() })
    }

    if (showAddPassenger) PassengerEditDialog(
        passenger = Passenger(),
        title = "Добавить пассажира",
        onDismiss = { showAddPassenger = false },
        onSave = { p -> viewModel.updatePassenger(p); showAddPassenger = false }
    )

    editPassenger?.let { p ->
        PassengerEditDialog(
            passenger = p,
            title = "Изменить пассажира",
            onDismiss = { editPassenger = null },
            onSave = { updated -> viewModel.updatePassenger(updated); editPassenger = null }
        )
    }

    deletePassenger?.let { p ->
        AlertDialog(
            onDismissRequest = { deletePassenger = null },
            title = { Text("Удалить пассажира?") },
            text = { Text(p.fullName) },
            confirmButton = {
                Button(onClick = { viewModel.deletePassenger(p); deletePassenger = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Удалить") }
            },
            dismissButton = { OutlinedButton(onClick = { deletePassenger = null }) { Text("Отмена") } }
        )
    }

    if (showAddCard) AddCardDialog(
        onDismiss = { showAddCard = false },
        onAdd = { card -> viewModel.addPaymentCard(card); showAddCard = false }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    if (showBackButton) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroSection(state = uiState, onAvatarClick = { avatarLauncher.launch("image/*") })

            PersonalDataCard(state = uiState, onEdit = { editField = it })

            PassengersCard(
                passengers = uiState.savedPassengers,
                onAdd = { showAddPassenger = true },
                onEdit = { editPassenger = it },
                onDelete = { deletePassenger = it }
            )

            PaymentCard(
                cards = uiState.paymentCards,
                onAdd = { showAddCard = true },
                onRemove = viewModel::removePaymentCard
            )

            SettingsCard(state = uiState, viewModel = viewModel)

            AccountCard(state = uiState, viewModel = viewModel, onSignOut = { showSignOut = true })

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Hero ────────────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(state: ProfileUiState, onAvatarClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                if (state.avatarUri != null) {
                    AsyncImage(
                        model = state.avatarUri, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable(onClick = onAvatarClick)
                    )
                } else {
                    val initials = state.profile.displayName.split(" ").filter { it.isNotEmpty() }
                        .take(2).joinToString("") { it.first().uppercase() }
                        .ifEmpty { state.profile.email.firstOrNull()?.uppercase() ?: "?" }
                    Box(
                        Modifier.size(72.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = onAvatarClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                SmallFloatingActionButton(
                    onClick = onAvatarClick,
                    modifier = Modifier.size(24.dp),
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSecondary)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.profile.displayName.ifEmpty { "Пользователь" },
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (state.profile.email.isNotEmpty())
                    Text(state.profile.email, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.profile.registeredAt > 0) {
                    val year = remember(state.profile.registeredAt) {
                        Instant.ofEpochMilli(state.profile.registeredAt).atZone(ZoneId.systemDefault()).year
                    }
                    Text("С нами с $year", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─── Личные данные ───────────────────────────────────────────────────────────

@Composable
private fun PersonalDataCard(state: ProfileUiState, onEdit: (EditField) -> Unit) {
    ProfileCard(title = "Личные данные", icon = Icons.Default.Person) {
        FieldRow(
            icon = Icons.Default.Badge,
            label = "ФИО",
            value = state.editName,
            hint = "Не указано",
            onClick = { onEdit(EditField.Name) }
        )
        HorizontalDivider()
        FieldRow(
            icon = Icons.Default.Phone,
            label = "Телефон",
            value = fmtPhone(state.editPhone),
            hint = "Не указан",
            onClick = { onEdit(EditField.Phone) }
        )
        HorizontalDivider()
        FieldRow(
            icon = Icons.Default.CalendarMonth,
            label = "Дата рождения",
            value = fmtDate(state.editBirthDate),
            hint = "Не указана",
            onClick = { onEdit(EditField.BirthDate) }
        )
        HorizontalDivider()
        FieldRow(
            icon = Icons.Default.CreditCard,
            label = "Паспорт",
            value = fmtPassport(state.editPassport),
            hint = "Не указан",
            onClick = { onEdit(EditField.Passport) }
        )
    }
}

@Composable
private fun FieldRow(icon: ImageVector, label: String, value: String, hint: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value.ifEmpty { hint },
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(Icons.Default.Edit, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Диалог редактирования поля ──────────────────────────────────────────────

@Composable
private fun EditFieldDialog(field: EditField, state: ProfileUiState, viewModel: ProfileViewModel, onDismiss: () -> Unit) {
    when (field) {
        EditField.Name -> {
            var value by remember { mutableStateOf(state.editName) }
            SimpleEditDialog(
                title = "ФИО",
                onDismiss = onDismiss,
                onConfirm = { viewModel.setEditName(value.trim()); viewModel.saveProfile(); onDismiss() }
            ) {
                OutlinedTextField(value = value, onValueChange = { value = it },
                    label = { Text("Фамилия Имя Отчество") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Badge, null, Modifier.size(18.dp)) })
            }
        }
        EditField.Phone -> {
            var raw by remember { mutableStateOf(extractPhoneDigits(state.editPhone)) }
            SimpleEditDialog(
                title = "Телефон",
                onDismiss = onDismiss,
                onConfirm = { viewModel.setEditPhone(raw); viewModel.saveProfile(); onDismiss() }
            ) {
                OutlinedTextField(
                    value = raw,
                    onValueChange = { raw = extractPhoneDigits(it) },
                    label = { Text("Номер телефона") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("+7 (___) ___-__-__") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    visualTransformation = PhoneVisualTransformation,
                    leadingIcon = { Icon(Icons.Default.Phone, null, Modifier.size(18.dp)) }
                )
            }
        }
        EditField.BirthDate -> {
            var raw by remember { mutableStateOf(extractDateDigits(state.editBirthDate)) }
            SimpleEditDialog(
                title = "Дата рождения",
                onDismiss = onDismiss,
                onConfirm = { viewModel.setEditBirthDate(raw); viewModel.saveProfile(); onDismiss() }
            ) {
                OutlinedTextField(
                    value = raw,
                    onValueChange = { raw = extractDateDigits(it) },
                    label = { Text("Дата рождения") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("дд.мм.гггг") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = DateVisualTransformation,
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp)) }
                )
            }
        }
        EditField.Passport -> {
            val passportDigits = remember { state.editPassport.filter { it.isDigit() } }
            var series by remember { mutableStateOf(passportDigits.take(4)) }
            var number by remember { mutableStateOf(passportDigits.drop(4).take(6)) }
            val isValid = series.length == 4 && number.length == 6
            SimpleEditDialog(
                title = "Паспорт",
                onDismiss = onDismiss,
                confirmEnabled = isValid,
                onConfirm = {
                    viewModel.setEditPassport("$series $number")
                    viewModel.saveProfile()
                    onDismiss()
                }
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = series,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) series = it },
                        label = { Text("Серия") },
                        placeholder = { Text("1234") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = number,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) number = it },
                        label = { Text("Номер") },
                        placeholder = { Text("567890") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Text(
                    "Паспорт РФ: 4 цифры серии и 6 цифр номера",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SimpleEditDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = confirmEnabled) { Text("Сохранить") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

// ─── Пассажиры ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PassengersCard(
    passengers: List<Passenger>,
    onAdd: () -> Unit,
    onEdit: (Passenger) -> Unit,
    onDelete: (Passenger) -> Unit
) {
    ProfileCard(
        title = "Пассажиры",
        icon = Icons.Default.People,
        action = {
            TextButton(onClick = onAdd) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Добавить")
            }
        }
    ) {
        if (passengers.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.PersonAdd, null, Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                Text("Нет сохранённых пассажиров",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("Нажмите «Добавить» или купите билет",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), textAlign = TextAlign.Center)
            }
        } else {
            passengers.forEach { passenger ->
                PassengerMiniCard(
                    passenger = passenger,
                    onClick = { onEdit(passenger) },
                    onLongClick = { onDelete(passenger) }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PassengerMiniCard(passenger: Passenger, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    passenger.lastName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(passenger.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
                Text("${passenger.documentLabel}: ${passenger.documentFull}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Диалог редактирования пассажира ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassengerEditDialog(passenger: Passenger, title: String, onDismiss: () -> Unit, onSave: (Passenger) -> Unit) {
    var lastName by remember { mutableStateOf(passenger.lastName) }
    var firstName by remember { mutableStateOf(passenger.firstName) }
    var patronymic by remember { mutableStateOf(passenger.patronymic) }
    var docType by remember { mutableStateOf(passenger.documentType) }
    var docSeries by remember { mutableStateOf(passenger.documentSeries) }
    var docNumber by remember { mutableStateOf(passenger.documentNumber) }
    var birthDate by remember { mutableStateOf(extractDateDigits(passenger.birthDate)) }
    var gender by remember { mutableStateOf(passenger.gender) }
    var docTypeExpanded by remember { mutableStateOf(false) }

    val isValid = lastName.isNotBlank() && firstName.isNotBlank() && docNumber.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = lastName, onValueChange = { lastName = it },
                        label = { Text("Фамилия*") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = firstName, onValueChange = { firstName = it },
                        label = { Text("Имя*") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = patronymic, onValueChange = { patronymic = it },
                    label = { Text("Отчество") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = docTypeExpanded, onExpandedChange = { docTypeExpanded = it }) {
                    OutlinedTextField(
                        value = when (docType) { "foreign_passport" -> "Загранпаспорт"; "birth_cert" -> "Свидетельство"; else -> "Паспорт РФ" },
                        onValueChange = {}, readOnly = true,
                        label = { Text("Документ") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(docTypeExpanded) }
                    )
                    ExposedDropdownMenu(expanded = docTypeExpanded, onDismissRequest = { docTypeExpanded = false }) {
                        listOf("passport" to "Паспорт РФ", "foreign_passport" to "Загранпаспорт", "birth_cert" to "Свидетельство").forEach { (v, l) ->
                            DropdownMenuItem(text = { Text(l) }, onClick = { docType = v; docTypeExpanded = false })
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = docSeries, onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) docSeries = it },
                        label = { Text("Серия") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = docNumber, onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) docNumber = it },
                        label = { Text("Номер*") }, singleLine = true, modifier = Modifier.weight(1.5f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = extractDateDigits(it) },
                        label = { Text("Дата рождения") }, singleLine = true, modifier = Modifier.weight(1f),
                        placeholder = { Text("дд.мм.гггг") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = DateVisualTransformation
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Пол", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = gender == "male", onClick = { gender = "male" }, label = { Text("М") })
                            FilterChip(selected = gender == "female", onClick = { gender = "female" }, label = { Text("Ж") })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(passenger.copy(lastName = lastName.trim(), firstName = firstName.trim(),
                        patronymic = patronymic.trim(), documentType = docType,
                        documentSeries = docSeries.trim(), documentNumber = docNumber.trim(),
                        birthDate = fmtDate(birthDate), gender = gender))
                },
                enabled = isValid
            ) { Text("Сохранить") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

// ─── Способ оплаты ───────────────────────────────────────────────────────────

@Composable
private fun PaymentCard(cards: List<PaymentCard>, onAdd: () -> Unit, onRemove: (PaymentCard) -> Unit) {
    ProfileCard(title = "Способ оплаты", icon = Icons.Default.CreditCard) {
        if (cards.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CreditCardOff, null, Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                Text("Нет привязанных карт",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Добавить карту")
                }
            }
        } else {
            cards.forEach { card -> PaymentCardItem(card = card, onRemove = { onRemove(card) }) }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Добавить карту")
            }
        }
    }
}

@Composable
private fun PaymentCardItem(card: PaymentCard, onRemove: () -> Unit) {
    val typeColor = when (card.type) {
        "mastercard" -> MaterialTheme.colorScheme.tertiary
        "mir" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CreditCard, null, tint = typeColor, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(card.type.uppercase(), style = MaterialTheme.typography.labelSmall,
                    color = typeColor, fontWeight = FontWeight.SemiBold)
                Text("•••• •••• •••• ${card.last4}", style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
                Text("до ${card.expiry}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─── Диалог добавления карты ──────────────────────────────────────────────────

@Composable
private fun AddCardDialog(onDismiss: () -> Unit, onAdd: (PaymentCard) -> Unit) {
    var last4 by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("visa") }
    val isValid = last4.length == 4 && expiry.length == 5

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить карту") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = last4,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) last4 = it },
                    label = { Text("Последние 4 цифры") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.CreditCard, null, Modifier.size(18.dp)) }
                )
                OutlinedTextField(
                    value = expiry,
                    onValueChange = { input ->
                        val d = input.filter { it.isDigit() }.take(4)
                        expiry = if (d.length >= 2) "${d.take(2)}/${d.drop(2)}" else d
                    },
                    label = { Text("Срок действия") },
                    placeholder = { Text("ММ/ГГ") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.DateRange, null, Modifier.size(18.dp)) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("visa" to "Visa", "mastercard" to "Mastercard", "mir" to "Мир").forEach { (v, l) ->
                        FilterChip(selected = type == v, onClick = { type = v }, label = { Text(l) })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onAdd(PaymentCard(last4, expiry, type)) }, enabled = isValid) { Text("Добавить") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

// ─── Настройки ───────────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(state: ProfileUiState, viewModel: ProfileViewModel) {
    ProfileCard(title = "Настройки", icon = Icons.Default.Settings) {
        SwitchRow(
            icon = if (state.settings.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
            title = "Тёмная тема",
            checked = state.settings.isDarkTheme,
            onToggle = viewModel::toggleDarkTheme
        )
        HorizontalDivider()
        SwitchRow(
            icon = Icons.Default.Notifications,
            title = "Уведомления",
            checked = state.settings.notificationsEnabled,
            onToggle = viewModel::toggleNotifications
        )
        HorizontalDivider()
        ActionRow(
            icon = Icons.Default.DeleteSweep,
            title = "Очистить кэш",
            subtitle = "Маршруты и билеты из памяти",
            onClick = viewModel::confirmClearCache
        )
    }
}

// ─── Аккаунт ─────────────────────────────────────────────────────────────────

@Composable
private fun AccountCard(state: ProfileUiState, viewModel: ProfileViewModel, onSignOut: () -> Unit) {
    ProfileCard(title = "Аккаунт", icon = Icons.Default.ManageAccounts) {
        ActionRow(
            icon = Icons.Default.Info,
            title = "О приложении",
            subtitle = "Transport Tickets · Версия 1.0"
        )
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSignOut).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Default.Logout, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
            Text("Выйти из аккаунта", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── Переиспользуемые компоненты ──────────────────────────────────────────────

@Composable
private fun ProfileCard(
    title: String,
    icon: ImageVector,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f))
                action?.invoke()
            }
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    loading: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick, enabled = !loading).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
