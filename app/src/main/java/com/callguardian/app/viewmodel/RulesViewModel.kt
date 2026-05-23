package com.callguardian.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callguardian.app.core.model.CallDecision
import com.callguardian.app.core.model.ContactPhoneSelection
import com.callguardian.app.core.model.CountryStatus
import com.callguardian.app.core.model.ForeignCallMode
import com.callguardian.app.core.model.RuleAction
import com.callguardian.app.data.local.BlockGroupEntity
import com.callguardian.app.data.local.BlockGroupMemberEntity
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
import java.util.Locale

data class RulesUiState(
    val rules: List<RuleEntity> = emptyList(),
    val blockGroups: List<BlockGroupUiModel> = emptyList(),
    val countries: List<CountryRuleEntity> = emptyList(),
    val foreignCallMode: ForeignCallMode = ForeignCallMode.BLOCK_UNKNOWN_FOREIGN,
    val simulatedDecision: CallDecision? = null,
    val simulationNumber: String = "",
    val message: String? = null,
)

data class BlockGroupUiModel(
    val group: BlockGroupEntity,
    val members: List<BlockGroupMemberEntity> = emptyList(),
    val contacts: List<BlockGroupContactUiModel> = emptyList(),
)

data class BlockGroupContactUiModel(
    val displayName: String,
    val phoneNumbers: List<String>,
    val memberIds: List<Long>,
)

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val repository: GuardianRepository,
) : ViewModel() {
    private val simulationResult = MutableStateFlow<Pair<String, CallDecision>?>(null)
    private val message = MutableStateFlow<String?>(null)

    private val baseState = combine(
        repository.rules,
        repository.blockGroups,
        repository.blockGroupMembers,
        repository.countryRules,
        repository.settings,
    ) { rules, blockGroups, blockGroupMembers, countries, settings ->
        RulesUiState(
            rules = rules,
            blockGroups = blockGroups.map { group ->
                BlockGroupUiModel(
                    group = group,
                    members = blockGroupMembers
                        .filter { it.groupId == group.id }
                        .sortedWith(compareBy<BlockGroupMemberEntity> { it.displayName.lowercase() }.thenBy { it.phoneNumber }),
                    contacts = blockGroupMembers
                        .filter { it.groupId == group.id }
                        .toBlockGroupContacts(),
                )
            },
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
            saveRule(text("Regola salvata", "Rule saved")) {
                repository.addNumberRule(text("Blocca ${number.trim()}", "Block ${number.trim()}"), number, RuleAction.BLOCK)
            }
        }
    }

    fun addAllowedNumber(number: String) {
        if (number.isBlank()) return
        viewModelScope.launch {
            saveRule(text("Regola salvata", "Rule saved")) {
                repository.addNumberRule(text("Consenti ${number.trim()}", "Allow ${number.trim()}"), number, RuleAction.ALLOW)
            }
        }
    }

    fun addBlockedPrefix(prefix: String) {
        if (prefix.isBlank()) return
        viewModelScope.launch {
            saveRule(text("Pattern salvato", "Pattern saved")) {
                repository.addPrefixRule(text("Pattern numero ${prefix.trim()}", "Number pattern ${prefix.trim()}"), prefix)
            }
        }
    }

    fun addSchedule(startTime: String, endTime: String) {
        viewModelScope.launch {
            val startMinute = parseMinuteOfDay(startTime)
            val endMinute = parseMinuteOfDay(endTime)
            if (startMinute == null || endMinute == null) {
                message.value = text("Orario non valido. Usa formato HH:mm, per esempio 09:00.", "Invalid time. Use HH:mm format, for example 09:00.")
                return@launch
            }
            saveRule(text("Programmazione salvata", "Schedule saved")) {
                repository.addScheduleRule(
                    label = text("Esteri ${startMinute.toClockLabel()}-${endMinute.toClockLabel()}", "Foreign ${startMinute.toClockLabel()}-${endMinute.toClockLabel()}"),
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
            saveRule(text("Nazione aggiornata", "Country updated")) {
                repository.updateCountryStatus(rule, status)
            }
        }
    }

    fun createBlockGroup(name: String) {
        viewModelScope.launch {
            saveRule(text("Gruppo creato", "Group created")) {
                repository.createBlockGroup(name)
            }
        }
    }

    fun updateBlockGroup(groupId: Long, name: String) {
        viewModelScope.launch {
            saveRule(text("Gruppo aggiornato", "Group updated")) {
                repository.updateBlockGroup(groupId, name)
            }
        }
    }

    fun toggleBlockGroup(group: BlockGroupEntity, enabled: Boolean) {
        viewModelScope.launch { repository.setBlockGroupEnabled(group.id, enabled) }
    }

    fun deleteBlockGroup(group: BlockGroupEntity) {
        viewModelScope.launch {
            saveRule(text("Gruppo eliminato", "Group deleted")) {
                repository.deleteBlockGroup(group.id)
            }
        }
    }

    fun clearBlockGroup(group: BlockGroupEntity) {
        viewModelScope.launch {
            saveRule(text("Gruppo svuotato", "Group cleared")) {
                repository.clearBlockGroup(group.id)
            }
        }
    }

    fun deleteBlockGroupMember(member: BlockGroupMemberEntity) {
        viewModelScope.launch {
            saveRule(text("Contatto rimosso", "Contact removed")) {
                repository.deleteBlockGroupMember(member.id)
            }
        }
    }

    fun deleteBlockGroupContact(contact: BlockGroupContactUiModel) {
        viewModelScope.launch {
            saveRule(text("Contatto rimosso", "Contact removed")) {
                repository.deleteBlockGroupMembers(contact.memberIds)
            }
        }
    }

    fun addContactsToBlockGroup(groupId: Long, contacts: List<ContactPhoneSelection>) {
        viewModelScope.launch {
            if (contacts.isEmpty()) {
                message.value = text("Il contatto selezionato non ha numeri disponibili.", "The selected contact has no available phone numbers.")
                return@launch
            }
            runCatching { repository.addBlockGroupMembers(groupId, contacts) }
                .onSuccess { added ->
                    message.value = if (added > 0) {
                        text("$added numero/i aggiunti al gruppo", "$added number(s) added to the group")
                    } else {
                        text("Nessun numero valido da aggiungere", "No valid number to add")
                    }
                }
                .onFailure {
                    message.value = text("Aggiunta non riuscita: ${it.message ?: "errore imprevisto"}", "Add failed: ${it.message ?: "unexpected error"}")
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
            .onFailure { message.value = text("Salvataggio non riuscito: ${it.message ?: "errore imprevisto"}", "Save failed: ${it.message ?: "unexpected error"}") }
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

    private fun text(it: String, en: String): String =
        if (Locale.getDefault().language == "it") it else en
}

private fun List<BlockGroupMemberEntity>.toBlockGroupContacts(): List<BlockGroupContactUiModel> =
    groupBy { member ->
        member.contactLookupKey?.takeIf { it.isNotBlank() }
            ?: member.contactId?.toString()
            ?: "${member.displayName.lowercase()}|${member.normalizedNumber}"
    }.values.map { members ->
        val sortedMembers = members.sortedWith(compareBy<BlockGroupMemberEntity> { it.displayName.lowercase() }.thenBy { it.phoneNumber })
        BlockGroupContactUiModel(
            displayName = sortedMembers.first().displayName,
            phoneNumbers = sortedMembers
                .map { it.phoneNumber }
                .distinctBy { phoneNumber -> phoneNumber.filter(Char::isDigit).ifBlank { phoneNumber } },
            memberIds = sortedMembers.map { it.id },
        )
    }.sortedBy { it.displayName.lowercase() }
