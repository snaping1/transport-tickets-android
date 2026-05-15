package com.transport.tickets.presentation.purchase

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.presentation.util.DateVisualTransformation
import com.transport.tickets.presentation.util.extractDateDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerInputScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    viewModel: PassengerInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    uiState.showSavedPickerForIdx?.let { idx ->
        SavedPassengerPickerDialog(
            passengers = uiState.savedPassengers,
            onSelect = { viewModel.selectSavedPassenger(idx, it) },
            onDismiss = viewModel::hideSavedPicker
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Данные пассажиров") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filled = uiState.seats.indices.count { uiState.isValid(it) }
                    if (uiState.seats.size > 1) {
                        LinearProgressIndicator(
                            progress = { filled.toFloat() / uiState.seats.size },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Заполнено $filled из ${uiState.seats.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { if (viewModel.confirmPassengers()) onContinue() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.allValid
                    ) {
                        Icon(Icons.Default.CreditCard, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Перейти к оплате")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.seats.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.currentSeatIdx,
                    edgePadding = 16.dp
                ) {
                    uiState.seats.forEachIndexed { idx, seat ->
                        Tab(
                            selected = uiState.currentSeatIdx == idx,
                            onClick = { viewModel.setCurrentSeat(idx) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Место $seat")
                                    if (uiState.isValid(idx)) {
                                        Icon(
                                            Icons.Default.CheckCircle, null,
                                            Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = uiState.currentSeatIdx,
                label = "seat_form"
            ) { idx ->
                if (idx < uiState.seats.size) {
                    val seat = uiState.seats[idx]
                    val draft = uiState.draftFor(idx)
                    val berthLabel = if (uiState.route?.transportType == "train") {
                        PricingUtils.trainBerthLabel(seat)
                    } else ""
                    val isSaving = uiState.savingPassengerIdx == idx
                    val isSaved = idx in uiState.savedIndices

                    PassengerForm(
                        seatNumber = seat,
                        berthLabel = berthLabel,
                        passenger = draft,
                        hasSavedPassengers = uiState.savedPassengers.isNotEmpty(),
                        isSaving = isSaving,
                        isSaved = isSaved,
                        onPassengerChange = { viewModel.updateDraft(idx, it) },
                        onPickSaved = { viewModel.showSavedPicker(idx) },
                        onSaveToMyPassengers = { viewModel.savePassengerToMyPassengers(idx) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassengerForm(
    seatNumber: Int,
    berthLabel: String,
    passenger: Passenger,
    hasSavedPassengers: Boolean,
    isSaving: Boolean,
    isSaved: Boolean,
    onPassengerChange: (Passenger) -> Unit,
    onPickSaved: () -> Unit,
    onSaveToMyPassengers: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AirlineSeatReclineNormal, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Column {
                        Text(
                            "Место $seatNumber",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (berthLabel.isNotEmpty()) {
                            Text(
                                berthLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                if (hasSavedPassengers) {
                    OutlinedButton(
                        onClick = onPickSaved,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Icon(Icons.Default.People, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Мои пассажиры")
                    }
                }
            }
        }

        Text(
            "Личные данные",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = passenger.lastName,
            onValueChange = { onPassengerChange(passenger.copy(lastName = it)) },
            label = { Text("Фамилия *") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, null) },
            singleLine = true
        )
        OutlinedTextField(
            value = passenger.firstName,
            onValueChange = { onPassengerChange(passenger.copy(firstName = it)) },
            label = { Text("Имя *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = passenger.patronymic,
            onValueChange = { onPassengerChange(passenger.copy(patronymic = it)) },
            label = { Text("Отчество") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        // Храним сырые цифры локально; VisualTransformation отображает как дд.мм.гггг
        var birthDateRaw by remember(passenger.birthDate) {
            mutableStateOf(extractDateDigits(passenger.birthDate))
        }
        OutlinedTextField(
            value = birthDateRaw,
            onValueChange = { input ->
                val raw = extractDateDigits(input)
                birthDateRaw = raw
                onPassengerChange(passenger.copy(birthDate = raw))
            },
            label = { Text("Дата рождения") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = DateVisualTransformation,
            placeholder = { Text("дд.мм.гггг") },
            singleLine = true
        )

        var genderExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = it }) {
            OutlinedTextField(
                value = if (passenger.gender == "male") "Мужской" else "Женский",
                onValueChange = {},
                label = { Text("Пол") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(genderExpanded) }
            )
            ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                DropdownMenuItem(text = { Text("Мужской") }, onClick = {
                    onPassengerChange(passenger.copy(gender = "male")); genderExpanded = false
                })
                DropdownMenuItem(text = { Text("Женский") }, onClick = {
                    onPassengerChange(passenger.copy(gender = "female")); genderExpanded = false
                })
            }
        }

        HorizontalDivider()
        Text(
            "Документ",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        var docTypeExpanded by remember { mutableStateOf(false) }
        val docTypes = listOf(
            "passport" to "Паспорт РФ",
            "foreign_passport" to "Загранпаспорт",
            "birth_cert" to "Св-во о рождении"
        )
        ExposedDropdownMenuBox(expanded = docTypeExpanded, onExpandedChange = { docTypeExpanded = it }) {
            OutlinedTextField(
                value = docTypes.find { it.first == passenger.documentType }?.second ?: "Паспорт РФ",
                onValueChange = {},
                label = { Text("Тип документа") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                leadingIcon = { Icon(Icons.Default.Badge, null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(docTypeExpanded) }
            )
            ExposedDropdownMenu(expanded = docTypeExpanded, onDismissRequest = { docTypeExpanded = false }) {
                docTypes.forEach { (key, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        onPassengerChange(passenger.copy(documentType = key)); docTypeExpanded = false
                    })
                }
            }
        }

        if (passenger.documentType == "passport") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = passenger.documentSeries,
                    onValueChange = { if (it.length <= 4) onPassengerChange(passenger.copy(documentSeries = it)) },
                    label = { Text("Серия") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = { Text("0000") }
                )
                OutlinedTextField(
                    value = passenger.documentNumber,
                    onValueChange = { if (it.length <= 6) onPassengerChange(passenger.copy(documentNumber = it)) },
                    label = { Text("Номер *") },
                    modifier = Modifier.weight(1.5f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = { Text("000000") }
                )
            }
        } else {
            OutlinedTextField(
                value = passenger.documentNumber,
                onValueChange = { onPassengerChange(passenger.copy(documentNumber = it)) },
                label = { Text("Номер документа *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        HorizontalDivider()

        OutlinedButton(
            onClick = onSaveToMyPassengers,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && !isSaved && passenger.firstName.isNotBlank() && passenger.lastName.isNotBlank()
        ) {
            when {
                isSaving -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                isSaved -> {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Сохранено в мои пассажиры")
                }
                else -> {
                    Icon(Icons.Default.BookmarkAdd, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Сохранить в мои пассажиры")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SavedPassengerPickerDialog(
    passengers: List<Passenger>,
    onSelect: (Passenger) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Мои пассажиры") },
        text = {
            if (passengers.isEmpty()) {
                Text("Сохранённых пассажиров нет")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    passengers.forEach { passenger ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSelect(passenger) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(passenger.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    "${passenger.documentLabel}: ${passenger.documentFull}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
