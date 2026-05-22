package com.callguardian.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callguardian.app.core.model.CallDecision
import com.callguardian.app.core.model.CountryStatus
import com.callguardian.app.core.model.ForeignCallMode
import com.callguardian.app.core.model.RuleAction
import com.callguardian.app.data.local.CountryRuleEntity
import com.callguardian.app.data.local.RuleEntity
import com.callguardian.app.data.repository.GuardianRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RulesUiState(
    val rules: List<RuleEntity> = emptyList(),
    val countries: List<CountryRuleEntity> = emptyList(),
    val foreignCallMode: ForeignCallMode = ForeignCallMode.BLOCK_UNKNOWN_FOREIGN,
    val simulatedDecision: CallDecision? = null,
    val simulationNumber: String = "",
    val message: String? = null,
)

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val repository: GuardianRepository,
) : ViewModel() {
    private val simulationResult = MutableStateFlow<Pair<String, CallDecision>?>(null)
    private val message = MutableStateFlow<String?>(null)

    private val baseState = combine(
        repository.rules,
        repository.countryRules,
        repository.settings,
    ) { rules, countries, settings ->
        RulesUiState(
            rules = rules,
            countries = countries,
            foreignCallMode = settings.foreignCallMode,
        )
    }

    val uiState: StateFlow<RulesUiState> = combine(
        baseState,
        simulationResult,
        message,
    ) { base, simulation, currentMessage ->
        base.copy(
            simulatedDecision = simulation?.second,
            simulationNumber = simulation?.first.orEmpty(),
            message = currentMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

    init {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    fun addBlockedNumber(number: String) {
        if (number.isBlank()) return
        viewModelScope.launch {
            saveRule("Regola salvata") {
                repository.addNumberRule("Blocca ${number.trim()}", number, RuleAction.BLOCK)
            }
        }
    }

    fun addAllowedNumber(number: String) {
        if (number.isBlank()) return
        viewModelScope.launch {
            saveRule("Regola salvata") {
                repository.addNumberRule("Consenti ${number.trim()}", number, RuleAction.ALLOW)
            }
        }
    }

    fun addBlockedPrefix(prefix: String) {
        if (prefix.isBlank()) return
        viewModelScope.launch {
            saveRule("Pattern salvato") {
                repository.addPrefixRule("Pattern numero ${prefix.trim()}", prefix)
            }
        }
    }

    fun addSchedule(startTime: String, endTime: String) {
        viewModelScope.launch {
            val startMinute = parseMinuteOfDay(startTime)
            val endMinute = parseMinuteOfDay(endTime)
            if (startMinute == null || endMinute == null) {
                message.value = "Orario non valido. Usa formato HH:mm, per esempio 09:00."
                return@launch
            }
            saveRule("Programmazione salvata") {
                repository.addScheduleRule(
                    label = "Esteri ${startMinute.toClockLabel()}-${endMinute.toClockLabel()}",
                    startsAtMinute = startMinute,
                    endsAtMinute = endMinute,
                )
            }
        }
    }

    fun toggleRule(rule: RuleEntity, enabled: Boolean) {
        viewModelScope.launch { repository.setRuleEnabled(rule.id, enabled) }
    }

    fun deleteRule(rule: RuleEntity) {
        viewModelScope.launch { repository.deleteRule(rule) }
    }

    fun deleteRules(ruleIds: Collection<Long>) {
        viewModelScope.launch { repository.deleteRules(ruleIds) }
    }

    fun setCountryStatus(rule: CountryRuleEntity, status: CountryStatus) {
        viewModelScope.launch {
            saveRule("Nazione aggiornata") {
                repository.updateCountryStatus(rule, status)
            }
        }
    }

    fun simulateCall(number: String) {
        if (number.isBlank()) return
        viewModelScope.launch {
            simulationResult.value = number to repository.evaluateIncomingCall(number)
        }
    }

    private suspend fun saveRule(successMessage: String, block: suspend () -> Unit) {
        runCatching { block() }
            .onSuccess { message.value = successMessage }
            .onFailure { message.value = "Salvataggio non riuscito: ${it.message ?: "errore imprevisto"}" }
    }

    private fun parseMinuteOfDay(value: String): Int? {
        val parts = value.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    private fun Int.toClockLabel(): String {
        val hour = this / 60
        val minute = this % 60
        return "%02d:%02d".format(hour, minute)
    }
}
