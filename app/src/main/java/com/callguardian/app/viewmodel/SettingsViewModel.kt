package com.callguardian.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callguardian.app.core.model.AnonymousMode
import com.callguardian.app.core.model.ForeignCallMode
import com.callguardian.app.core.model.ProtectionLevel
import com.callguardian.app.core.model.ThemeMode
import com.callguardian.app.core.model.ThemePalette
import com.callguardian.app.data.backup.LocalBackupManager
import com.callguardian.app.data.local.AppSettingsEntity
import com.callguardian.app.data.repository.GuardianRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: GuardianRepository,
    private val backupManager: LocalBackupManager,
) : ViewModel() {
    val settings: StateFlow<AppSettingsEntity> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.initialSettings())
    private val _appReady = MutableStateFlow(false)
    val appReady: StateFlow<Boolean> = _appReady.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
            _appReady.value = true
        }
    }

    fun setProtectionLevel(level: ProtectionLevel) = update { it.copy(protectionLevel = level) }
    fun setAnonymousMode(mode: AnonymousMode) = update { it.copy(anonymousMode = mode) }
    fun setForeignMode(mode: ForeignCallMode) = update { it.copy(foreignCallMode = mode) }
    fun setRepeatedAnonymousAttempts(attempts: Int) = update { it.copy(allowRepeatedAnonymousAfterAttempts = attempts.coerceIn(2, 10)) }
    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setPalette(palette: ThemePalette) = update { it.copy(palette = palette) }
    fun setHighContrast(enabled: Boolean) = update { it.copy(highContrast = enabled) }
    fun setContextualHelp(enabled: Boolean) = update { it.copy(contextualHelpEnabled = enabled) }

    suspend fun exportBackupJson(): String = backupManager.exportJson()

    suspend fun importBackupJson(json: String) {
        backupManager.importJson(json)
        repository.initialize()
        repository.refreshSettingsSnapshot()
    }

    private fun update(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(transform(settings.value))
        }
    }
}
