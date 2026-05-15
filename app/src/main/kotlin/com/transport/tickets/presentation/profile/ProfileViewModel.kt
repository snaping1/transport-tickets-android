package com.transport.tickets.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.transport.tickets.data.preferences.AppPreferences
import com.transport.tickets.domain.model.AppSettings
import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.domain.model.UserProfile
import com.transport.tickets.domain.repository.UserRepository
import com.transport.tickets.domain.usecase.ClearCacheUseCase
import com.transport.tickets.domain.usecase.DeletePassengerUseCase
import com.transport.tickets.domain.usecase.GetPassengersUseCase
import com.transport.tickets.domain.usecase.GetProfileUseCase
import com.transport.tickets.domain.usecase.GetSettingsUseCase
import com.transport.tickets.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val editName: String = "",
    val editPhone: String = "",
    val editBirthDate: String = "",
    val profileSaving: Boolean = false,
    val profileSaved: Boolean = false,
    val profileError: String? = null,
    val passwordResetLoading: Boolean = false,
    val passwordResetSent: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val showClearCacheConfirm: Boolean = false,
    val cacheCleared: Boolean = false,
    val avatarUri: String? = null,
    val savedPassengers: List<Passenger> = emptyList()
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val userRepository: UserRepository,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val appPreferences: AppPreferences,
    private val getPassengersUseCase: GetPassengersUseCase,
    private val deletePassengerUseCase: DeletePassengerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getProfileUseCase().collect { profile ->
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            editName = it.editName.ifEmpty { profile.displayName },
                            editPhone = it.editPhone.ifEmpty { profile.phone },
                            editBirthDate = it.editBirthDate.ifEmpty { profile.birthDate }
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            appPreferences.avatarUri.collect { uri ->
                _uiState.update { it.copy(avatarUri = uri) }
            }
        }
        viewModelScope.launch {
            getPassengersUseCase().collect { passengers ->
                _uiState.update { it.copy(savedPassengers = passengers) }
            }
        }
    }

    fun setEditName(v: String) = _uiState.update { it.copy(editName = v) }
    fun setEditPhone(v: String) = _uiState.update { it.copy(editPhone = v) }
    fun setEditBirthDate(v: String) = _uiState.update { it.copy(editBirthDate = v) }

    fun saveProfile() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(profileSaving = true, profileError = null) }
            try {
                updateProfileUseCase(
                    state.profile.copy(
                        displayName = state.editName.trim(),
                        phone = state.editPhone.trim(),
                        birthDate = state.editBirthDate.trim()
                    )
                )
                _uiState.update { it.copy(profileSaving = false, profileSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(profileSaving = false, profileError = e.message ?: "Ошибка сохранения") }
            }
        }
    }

    fun resetSaveState() = _uiState.update { it.copy(profileSaved = false, profileError = null) }

    fun sendPasswordReset() {
        val email = _uiState.value.profile.email.ifEmpty { return }
        viewModelScope.launch {
            _uiState.update { it.copy(passwordResetLoading = true) }
            try {
                firebaseAuth.sendPasswordResetEmail(email)
                _uiState.update { it.copy(passwordResetLoading = false, passwordResetSent = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(passwordResetLoading = false) }
            }
        }
    }

    fun resetPasswordResetState() = _uiState.update { it.copy(passwordResetSent = false) }

    fun toggleDarkTheme() {
        val newVal = !_uiState.value.settings.isDarkTheme
        viewModelScope.launch { userRepository.setDarkTheme(newVal) }
    }

    fun toggleNotifications() {
        val newVal = !_uiState.value.settings.notificationsEnabled
        viewModelScope.launch { userRepository.setNotifications(newVal) }
    }

    fun toggleLanguage() {
        val newLang = if (_uiState.value.settings.language == "ru") "en" else "ru"
        viewModelScope.launch { userRepository.setLanguage(newLang) }
    }

    fun confirmClearCache() = _uiState.update { it.copy(showClearCacheConfirm = true) }
    fun dismissClearCache() = _uiState.update { it.copy(showClearCacheConfirm = false) }

    fun clearCache() {
        viewModelScope.launch {
            clearCacheUseCase()
            _uiState.update { it.copy(showClearCacheConfirm = false, cacheCleared = true) }
        }
    }

    fun resetCacheClearedState() = _uiState.update { it.copy(cacheCleared = false) }

    fun setAvatarUri(uri: String?) {
        viewModelScope.launch { appPreferences.setAvatarUri(uri) }
    }

    fun deletePassenger(passenger: com.transport.tickets.domain.model.Passenger) {
        viewModelScope.launch { deletePassengerUseCase(passenger) }
    }

    fun signOut() = firebaseAuth.signOut()
}
