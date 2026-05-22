package com.callguardian.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callguardian.app.core.model.CallDecision
import com.callguardian.app.core.model.PermissionSummary
import com.callguardian.app.core.permissions.PermissionChecker
import com.callguardian.app.data.local.AppSettingsEntity
import com.callguardian.app.data.repository.GuardianRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProtectionUiState(
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val blockedToday: Int = 0,
    val permissions: PermissionSummary? = null,
    val readinessScore: Int = 0,
    val testDecision: CallDecision? = null,
    val testNumber: String = "",
    val testError: String? = null,
)

private data class TestNumberState(
    val decision: CallDecision? = null,
    val number: String = "",
    val error: String? = null,
)

@HiltViewModel
class ProtectionViewModel @Inject constructor(
    private val repository: GuardianRepository,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {
    private val permissions = MutableStateFlow<PermissionSummary?>(null)
    private val testDecision = MutableStateFlow<CallDecision?>(null)
    private val testNumber = MutableStateFlow("")
    private val testError = MutableStateFlow<String?>(null)
    private val testState = combine(
        testDecision,
        testNumber,
        testError,
    ) { decision, number, error ->
        TestNumberState(decision, number, error)
    }

    val uiState: StateFlow<ProtectionUiState> = combine(
        repository.settings,
        repository.blockedToday,
        permissions,
        testState,
    ) { settings, blockedToday, permissionSummary, test ->
        ProtectionUiState(
            settings = settings,
            blockedToday = blockedToday,
            permissions = permissionSummary,
            readinessScore = readinessScore(permissionSummary, settings),
            testDecision = test.decision,
            testNumber = test.number,
            testError = test.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProtectionUiState())

    init {
        viewModelScope.launch { repository.initialize() }
        refreshPermissions()
    }

    fun refreshPermissions() {
        permissions.value = permissionChecker.summary()
    }

    fun applyRecommendedSetup() {
        viewModelScope.launch { repository.applyRecommendedSetup() }
    }

    fun updateTestNumber(number: String) {
        testNumber.value = number
        testError.value = null
    }

    fun previewTestNumber() {
        val number = testNumber.value.trim()
        if (number.isBlank()) {
            testDecision.value = null
            testError.value = "Inserisci un numero o lascia anonimo per simulare una chiamata privata."
            return
        }
        viewModelScope.launch {
            runCatching { repository.previewCallDecision(number) }
                .onSuccess {
                    testDecision.value = it
                    testError.value = null
                }
                .onFailure {
                    testDecision.value = null
                    testError.value = it.message ?: "Simulazione non riuscita"
                }
        }
    }

    private fun readinessScore(
        permissions: PermissionSummary?,
        settings: AppSettingsEntity,
    ): Int {
        var score = 0
        if (permissions?.runtimePermissionsGranted == true) score += 25
        if (permissions?.callScreeningRoleHeld == true) score += 35
        if (permissions?.notificationPermissionGranted == true) score += 10
        if (permissions?.overlayAllowed == true) score += 5
        score += when (settings.protectionLevel.name) {
            "AGGRESSIVE" -> 25
            "BALANCED", "CUSTOM" -> 18
            "LIGHT" -> 10
            else -> 0
        }
        return score.coerceIn(0, 100)
    }
}
