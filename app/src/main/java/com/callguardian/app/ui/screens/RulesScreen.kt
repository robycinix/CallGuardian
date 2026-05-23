package com.callguardian.app.ui.screens

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callguardian.app.R
import com.callguardian.app.core.model.ContactPhoneSelection
import com.callguardian.app.core.model.CountryStatus
import com.callguardian.app.core.model.ForeignCallMode
import com.callguardian.app.core.model.RuleType
import com.callguardian.app.core.model.resolveInternationalDialingInput
import com.callguardian.app.data.local.BlockGroupEntity
import com.callguardian.app.data.local.CountryRuleEntity
import com.callguardian.app.data.local.RuleEntity
import com.callguardian.app.ui.components.ContextualHelpButton
import com.callguardian.app.ui.components.HelpContent
import com.callguardian.app.ui.components.InternationalDialingInput
import com.callguardian.app.ui.components.SectionCard
import com.callguardian.app.ui.components.ScreenHeader
import com.callguardian.app.ui.components.SwipeToDeleteContainer
import com.callguardian.app.ui.components.rememberDefaultDialingCountry
import com.callguardian.app.viewmodel.BlockGroupContactUiModel
import com.callguardian.app.viewmodel.BlockGroupUiModel
import com.callguardian.app.viewmodel.RulesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesScreen(
    onSwipePastStart: () -> Unit = {},
    onSwipePastEnd: () -> Unit = {},
    initialTab: String = "lists",
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val defaultDialingCountry = rememberDefaultDialingCountry()
    var number by remember { mutableStateOf("") }
    var numberCountry by remember { mutableStateOf(defaultDialingCountry) }
    var prefix by remember { mutableStateOf("") }
    var prefixCountry by remember { mutableStateOf(defaultDialingCountry) }
    var groupName by remember { mutableStateOf("") }
    var scheduleStart by remember { mutableStateOf("09:00") }
    var scheduleEnd by remember { mutableStateOf("18:00") }
    var countryQuery by remember { mutableStateOf("") }
    var selectedRuleIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedTab by remember { mutableStateOf(RulesTab.LISTS) }
    var contactPickerGroupId by remember { mutableStateOf<Long?>(null) }
    var contactPickerContacts by remember { mutableStateOf<List<BlockGroupContactOption>>(emptyList()) }
    var contactPickerLoading by remember { mutableStateOf(false) }
    var contactPickerQuery by remember { mutableStateOf("") }
    var selectedContactKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showNewSummaryIndicator by remember { mutableStateOf(false) }
    var previousSummarySignal by remember { mutableStateOf<RulesSummarySignal?>(null) }
    val swipeThresholdPx = with(LocalDensity.current) { 120.dp.toPx() }
    LaunchedEffect(contactPickerGroupId) {
        if (contactPickerGroupId != null) {
            contactPickerLoading = true
            contactPickerContacts = withContext(Dispatchers.IO) { loadBlockGroupContactOptions(context) }
            contactPickerLoading = false
        } else {
            contactPickerContacts = emptyList()
            contactPickerQuery = ""
            selectedContactKeys = emptySet()
        }
    }
    val numberInputValid = isDialablePhoneInput(number)
    val prefixInputValid = isPrefixInput(prefix)
    val scheduleRules = state.rules.filter { it.type == RuleType.SCHEDULE }
    val customRules = state.rules.filterNot { it.type == RuleType.ANONYMOUS || it.type == RuleType.SCHEDULE }
    val selectedCustomRuleIds = selectedRuleIds.intersect(customRules.map { it.id }.toSet())
    val configuredCountries = state.countries
        .filter { it.status != CountryStatus.MONITORED }
        .sortedWith(compareBy<CountryRuleEntity> { it.status.ordinal }.thenBy { it.name })
    val blockedNumbers = customRules.count { it.type == RuleType.BLACKLIST_NUMBER }
    val allowedNumbers = customRules.count { it.type == RuleType.WHITELIST }
    val blockedNumberPatterns = customRules.count { it.type == RuleType.PREFIX || it.type == RuleType.RANGE }
    val blockGroupContacts = state.blockGroups.sumOf { it.contacts.size }
    val blockedCountries = configuredCountries.count { it.status == CountryStatus.BLOCKED }
    val allowedCountries = configuredCountries.count { it.status == CountryStatus.ALLOWED }
    val hasSummaryItems = customRules.isNotEmpty() ||
        state.blockGroups.isNotEmpty() ||
        scheduleRules.isNotEmpty() ||
        configuredCountries.isNotEmpty()
    val summarySignal = RulesSummarySignal(
        customRules = customRules.size,
        blockGroups = state.blockGroups.size,
        blockGroupContacts = blockGroupContacts,
        scheduleRules = scheduleRules.size,
        configuredCountries = configuredCountries.size,
    )
    val showCountrySearchDock = selectedTab == RulesTab.FOREIGN && countryQuery.isNotBlank()

    LaunchedEffect(initialTab) {
        selectedTab = initialTab.toRulesTab()
    }

    LaunchedEffect(summarySignal, selectedTab) {
        val previous = previousSummarySignal
        if (selectedTab == RulesTab.SUMMARY) {
            showNewSummaryIndicator = false
        } else if (previous != null && summarySignal.hasIncreaseComparedTo(previous)) {
            showNewSummaryIndicator = true
        }
        previousSummarySignal = summarySignal
    }

    contactPickerGroupId?.let { groupId ->
        ContactMultiSelectDialog(
            contacts = contactPickerContacts,
            query = contactPickerQuery,
            selectedKeys = selectedContactKeys,
            loading = contactPickerLoading,
            onQueryChange = { contactPickerQuery = it },
            onContactToggle = { key ->
                selectedContactKeys = if (key in selectedContactKeys) {
                    selectedContactKeys - key
                } else {
                    selectedContactKeys + key
                }
            },
            onDismiss = { contactPickerGroupId = null },
            onConfirm = {
                val selectedContacts = contactPickerContacts.filter { it.key in selectedContactKeys }
                viewModel.addContactsToBlockGroup(
                    groupId = groupId,
                    contacts = selectedContacts.flatMap { it.phoneSelections },
                )
                contactPickerGroupId = null
            },
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .rulesTabGestureNavigation(
                selectedTab = selectedTab,
                swipeThresholdPx = swipeThresholdPx,
                onTabSelected = { selectedTab = it },
                onSwipePastStart = onSwipePastStart,
                onSwipePastEnd = onSwipePastEnd,
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item {
            ScreenHeader(
                title = uiText("Centro regole", "Rules center"),
                subtitle = uiText("Numeri, gruppi, pattern e nazioni in una console locale di difesa.", "Numbers, groups, patterns, and countries in a local defense console."),
            )
        }
        state.message?.let { message ->
            item {
                SectionCard(title = uiText("Stato", "Status")) {
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            RulesTabRow(
                selectedTab = selectedTab,
                showSummaryIndicator = showNewSummaryIndicator && selectedTab != RulesTab.SUMMARY,
                onTabSelected = { selectedTab = it },
            )
        }
        when (selectedTab) {
            RulesTab.LISTS -> {
                item {
                    SectionCard(title = uiText("Benvenuto nel Centro regole", "Welcome to the Rules center")) {
                        GuidanceText(uiText("Qui decidi quali chiamate far passare e quali fermare, con regole salvate solo sul dispositivo.", "Here you decide which calls to allow and which to stop, with rules saved only on the device."))
                        GuidanceText(uiText("Puoi consentire un contatto importante, bloccare un numero preciso o fermare numeri che iniziano con lo stesso pattern.", "You can allow an important contact, block a specific number, or stop numbers that start with the same pattern."))
                    }
                }
                item {
                    SectionCard(
                        title = uiText("Regola per numero singolo", "Single number rule"),
                        trailing = {
                            ContextualHelpButton(
                                title = uiText("Consentiti e bloccati", "Allowed and blocked"),
                                explanation = uiText("Le regole su numero esatto hanno priorità alta.", "Exact-number rules have high priority."),
                                benefits = uiText("Controllo preciso senza inviare dati fuori dal dispositivo.", "Precise control without sending data off the device."),
                                drawbacks = uiText("Richiedono inserimento manuale o azioni rapide.", "They require manual entry or quick actions."),
                                advice = uiText("Usa la lista consentiti per banca, medico e contatti importanti.", "Use the allow list for bank, doctor, and important contacts."),
                                androidLimits = uiText("La rubrica è leggibile solo con il permesso Lettura contatti.", "Contacts can be read only with the Read contacts permission."),
                            )
                        }
                    ) {
                        GuidanceText(uiText("Aggiungi numeri precisi da bloccare o proteggere.", "Add exact numbers to block or protect."))
                        InternationalDialingInput(
                            value = number,
                            onValueChange = { number = it },
                            selectedCountry = numberCountry,
                            onCountrySelected = { numberCountry = it },
                            label = uiText("Numero", "Number"),
                            placeholder = "3331234567",
                            supportingText = uiText("Il prefisso selezionato viene aggiunto se non scrivi gia + o 00.", "The selected prefix is added unless you already type + or 00."),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    enabled = numberInputValid,
                                    onClick = {
                                        viewModel.addBlockedNumber(resolveInternationalDialingInput(number, numberCountry))
                                        number = ""
                                    },
                                ) { Text(uiText("Blocca", "Block")) }
                                ContextualHelpButton(blockNumberHelp())
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    enabled = numberInputValid,
                                    onClick = {
                                        viewModel.addAllowedNumber(resolveInternationalDialingInput(number, numberCountry))
                                        number = ""
                                    },
                                ) { Text(uiText("Consenti", "Allow")) }
                                ContextualHelpButton(allowNumberHelp())
                            }
                        }
                    }
                }
                item {
                    SectionCard(title = uiText("Regola per pattern di numero", "Number pattern rule")) {
                        GuidanceText(uiText("Blocca tutti i numeri che iniziano con il pattern indicato, non solo il prefisso isolato.", "Block all numbers that start with the specified pattern, not just the isolated prefix."))
                        InternationalDialingInput(
                            value = prefix,
                            onValueChange = { prefix = it },
                            selectedCountry = prefixCountry,
                            onCountrySelected = { prefixCountry = it },
                            label = uiText("Pattern dopo prefisso", "Pattern after prefix"),
                            placeholder = uiText("0288 oppure 333", "20 or 555"),
                            supportingText = uiText("Esempio: seleziona Regno Unito e scrivi 20 per creare +4420.", "Example: select United Kingdom and type 20 to create +4420."),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                enabled = prefixInputValid,
                                onClick = {
                                    viewModel.addBlockedPrefix(resolveInternationalDialingInput(prefix, prefixCountry))
                                    prefix = ""
                                },
                            ) { Text(uiText("Blocca pattern", "Block pattern")) }
                            ContextualHelpButton(blockPrefixHelp())
                        }
                    }
                }
            }
            RulesTab.GROUPS -> {
                item {
                    BlockGroupsSection(
                        blockGroups = state.blockGroups,
                        groupName = groupName,
                        onGroupNameChange = { groupName = it },
                        onCreateGroup = {
                            viewModel.createBlockGroup(groupName)
                            groupName = ""
                        },
                        onPickContact = { groupId -> contactPickerGroupId = groupId },
                        onUpdateGroup = viewModel::updateBlockGroup,
                        onToggleGroup = viewModel::toggleBlockGroup,
                        onClearGroup = viewModel::clearBlockGroup,
                        onDeleteGroup = viewModel::deleteBlockGroup,
                        onDeleteContact = viewModel::deleteBlockGroupContact,
                    )
                }
            }
            RulesTab.FOREIGN -> {
                if (state.foreignCallMode == ForeignCallMode.SCHEDULED) {
                    item {
                        ForeignScheduleSection(
                            start = scheduleStart,
                            end = scheduleEnd,
                            scheduleRules = scheduleRules,
                            onStartChange = { scheduleStart = it },
                            onEndChange = { scheduleEnd = it },
                            onAddSchedule = { viewModel.addSchedule(scheduleStart, scheduleEnd) },
                            onToggleSchedule = viewModel::toggleRule,
                            onDeleteSchedule = viewModel::deleteRule,
                        )
                    }
                }
                item {
                    CountryRulesSection(
                        countries = state.countries,
                        countryQuery = countryQuery,
                        onCountryQueryChange = { countryQuery = it },
                        foreignMode = state.foreignCallMode,
                        onStatusSelected = viewModel::setCountryStatus,
                    )
                }
            }
            RulesTab.SUMMARY -> {
                item {
                    SectionCard(title = uiText("Regole configurate", "Configured rules")) {
                        if (!hasSummaryItems) {
                            GuidanceText(uiText("Le regole create appariranno qui.", "Created rules will appear here."))
                        } else {
                            GuidanceText(uiText("Riepilogo di numeri, gruppi, fasce programmate e nazioni configurate.", "Summary of numbers, groups, schedules, and countries."))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AssistChip(onClick = {}, label = { Text(uiText("$blockedNumbers bloccati", "$blockedNumbers blocked")) })
                                AssistChip(onClick = {}, label = { Text(uiText("$allowedNumbers consentiti", "$allowedNumbers allowed")) })
                                AssistChip(onClick = {}, label = { Text(uiText("$blockedNumberPatterns pattern", "$blockedNumberPatterns patterns")) })
                                AssistChip(onClick = {}, label = { Text(uiText("${state.blockGroups.size} gruppi", "${state.blockGroups.size} groups")) })
                                AssistChip(onClick = {}, label = { Text(uiText("$blockGroupContacts contatti gruppo", "$blockGroupContacts group contacts")) })
                                AssistChip(onClick = {}, label = { Text(uiText("${scheduleRules.size} programmate", "${scheduleRules.size} scheduled")) })
                                AssistChip(onClick = {}, label = { Text(uiText("$allowedCountries nazioni consentite", "$allowedCountries allowed countries")) })
                                AssistChip(onClick = {}, label = { Text(uiText("$blockedCountries nazioni bloccate", "$blockedCountries blocked countries")) })
                            }
                            FlowRow(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    enabled = selectedCustomRuleIds.isNotEmpty(),
                                    onClick = {
                                        viewModel.deleteRules(selectedCustomRuleIds)
                                        selectedRuleIds = emptySet()
                                    },
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Text(uiText("Elimina selezionate", "Delete selected"))
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                customRules.forEach { rule ->
                                    SwipeToDeleteContainer(
                                        onDelete = {
                                            viewModel.deleteRule(rule)
                                            selectedRuleIds = selectedRuleIds - rule.id
                                        },
                                    ) {
                                        ConfiguredRuleRow(
                                            title = rule.label,
                                            subtitle = stringResource(R.string.rule_subtitle_format, rule.type.localizedDisplayName(), rule.action.localizedDisplayName()),
                                            detail = rule.value,
                                            selected = rule.id in selectedRuleIds,
                                            enabled = rule.enabled,
                                            onSelectedChange = { checked ->
                                                selectedRuleIds = if (checked) selectedRuleIds + rule.id else selectedRuleIds - rule.id
                                            },
                                            onEnabledChange = { viewModel.toggleRule(rule, it) },
                                            onDelete = {
                                                viewModel.deleteRule(rule)
                                                selectedRuleIds = selectedRuleIds - rule.id
                                            },
                                            toggleHelp = ruleToggleHelp(rule.label),
                                            deleteHelp = ruleDeleteHelp(rule.label),
                                        )
                                    }
                                }
                                scheduleRules.forEach { rule ->
                                    ConfiguredRuleRow(
                                        title = rule.label,
                                        subtitle = stringResource(R.string.rule_subtitle_format, ForeignCallMode.SCHEDULED.localizedDisplayName(), rule.action.localizedDisplayName()),
                                        detail = scheduleRuleLabel(rule),
                                        selected = false,
                                        enabled = rule.enabled,
                                        onSelectedChange = null,
                                        onEnabledChange = { viewModel.toggleRule(rule, it) },
                                        onDelete = { viewModel.deleteRule(rule) },
                                        toggleHelp = ruleToggleHelp(rule.label),
                                        deleteHelp = ruleDeleteHelp(rule.label),
                                    )
                                }
                                configuredCountries.forEach { country ->
                                    CountryOverviewRow(
                                        country = country,
                                        foreignMode = state.foreignCallMode,
                                        onReset = { viewModel.setCountryStatus(country, CountryStatus.MONITORED) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
        if (showCountrySearchDock) {
            CountrySearchDock(
                query = countryQuery,
                onQueryChange = { countryQuery = it },
                onClear = { countryQuery = "" },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

private enum class RulesTab(val label: String) {
    LISTS("Liste"),
    GROUPS("Gruppi"),
    FOREIGN("Esteri"),
    SUMMARY("Sintesi"),
}

private data class RulesSummarySignal(
    val customRules: Int,
    val blockGroups: Int,
    val blockGroupContacts: Int,
    val scheduleRules: Int,
    val configuredCountries: Int,
) {
    fun hasIncreaseComparedTo(previous: RulesSummarySignal): Boolean =
        customRules > previous.customRules ||
            blockGroups > previous.blockGroups ||
            blockGroupContacts > previous.blockGroupContacts ||
            scheduleRules > previous.scheduleRules ||
            configuredCountries > previous.configuredCountries
}

private fun RulesTab.localizedLabel(): String = when (this) {
    RulesTab.LISTS -> uiText("Liste", "Lists")
    RulesTab.GROUPS -> uiText("Gruppi", "Groups")
    RulesTab.FOREIGN -> uiText("Esteri", "Foreign")
    RulesTab.SUMMARY -> uiText("Sintesi", "Summary")
}

private fun String.toRulesTab(): RulesTab = when (this) {
    "groups" -> RulesTab.GROUPS
    "foreign" -> RulesTab.FOREIGN
    "summary" -> RulesTab.SUMMARY
    else -> RulesTab.LISTS
}

private fun isDialablePhoneInput(value: String): Boolean {
    val trimmed = value.trim()
    val digits = trimmed.count { it.isDigit() }
    return digits >= 5 && trimmed.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }
}

private fun isPrefixInput(value: String): Boolean {
    val trimmed = value.trim()
    val digits = trimmed.count { it.isDigit() }
    return digits >= 2 && trimmed.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }
}

private data class BlockGroupContactOption(
    val key: String,
    val displayName: String,
    val phoneNumbers: List<String>,
    val phoneSelections: List<ContactPhoneSelection>,
) {
    fun matches(query: String): Boolean {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return true
        val digits = cleanQuery.filter(Char::isDigit)
        return displayName.contains(cleanQuery, ignoreCase = true) ||
            phoneNumbers.any { it.contains(cleanQuery, ignoreCase = true) } ||
            (digits.isNotEmpty() && phoneNumbers.any { it.filter(Char::isDigit).contains(digits) })
    }
}

private fun loadBlockGroupContactOptions(context: Context): List<BlockGroupContactOption> = runCatching {
    val resolver = context.contentResolver
    val phoneSelections = mutableListOf<ContactPhoneSelection>()
    resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
        ),
        null,
        null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            val number = cursor.stringOrNull(ContactsContract.CommonDataKinds.Phone.NUMBER)?.takeIf { it.isNotBlank() } ?: continue
            phoneSelections += ContactPhoneSelection(
                displayName = cursor.stringOrNull(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME) ?: number,
                phoneNumber = number,
                contactId = cursor.longOrNull(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
                contactLookupKey = cursor.stringOrNull(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY),
            )
        }
    }
    phoneSelections
        .groupBy { contact ->
            contact.contactLookupKey?.takeIf { it.isNotBlank() }
                ?: contact.contactId?.toString()
                ?: contact.displayName
        }
        .map { (key, contacts) ->
            val sortedContacts = contacts.sortedWith(compareBy<ContactPhoneSelection> { it.displayName.lowercase() }.thenBy { it.phoneNumber })
            val distinctContacts = sortedContacts.distinctBy { contact ->
                contact.phoneNumber.filter(Char::isDigit).ifBlank { contact.phoneNumber }
            }
            BlockGroupContactOption(
                key = key,
                displayName = sortedContacts.first().displayName,
                phoneNumbers = distinctContacts.map { it.phoneNumber },
                phoneSelections = distinctContacts,
            )
        }
        .sortedBy { it.displayName.lowercase() }
}.getOrElse { emptyList() }

@Composable
private fun ContactMultiSelectDialog(
    contacts: List<BlockGroupContactOption>,
    query: String,
    selectedKeys: Set<String>,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onContactToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val filteredContacts = remember(contacts, query) {
        contacts.filter { it.matches(query) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiText("Seleziona contatti", "Select contacts")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(uiText("Cerca in rubrica", "Search contacts")) },
                    singleLine = true,
                )
                Text(
                    when {
                        loading -> uiText("Caricamento rubrica...", "Loading contacts...")
                        contacts.isEmpty() -> uiText("Nessun contatto con numero disponibile.", "No contact with a phone number available.")
                        selectedKeys.isEmpty() -> uiText("Seleziona uno o piu contatti.", "Select one or more contacts.")
                        else -> uiText("${selectedKeys.size} selezionati", "${selectedKeys.size} selected")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filteredContacts, key = { it.key }) { contact ->
                        val selected = contact.key in selectedKeys
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onContactToggle(contact.key) },
                            shape = MaterialTheme.shapes.small,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.54f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { onContactToggle(contact.key) },
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(contact.displayName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (contact.phoneNumbers.size == 1) {
                                            contact.phoneNumbers.first()
                                        } else {
                                            uiText("${contact.phoneNumbers.size} numeri", "${contact.phoneNumbers.size} numbers")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedKeys.isNotEmpty() && !loading,
                onClick = onConfirm,
            ) {
                Text(uiText("Aggiungi selezionati", "Add selected"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(uiText("Annulla", "Cancel"))
            }
        },
    )
}

private fun Cursor.stringOrNull(columnName: String): String? {
    val index = getColumnIndex(columnName)
    return if (index >= 0 && !isNull(index)) getString(index) else null
}

private fun Cursor.longOrNull(columnName: String): Long? {
    val index = getColumnIndex(columnName)
    return if (index >= 0 && !isNull(index)) getLong(index) else null
}

@Composable
private fun RulesTabRow(
    selectedTab: RulesTab,
    showSummaryIndicator: Boolean,
    onTabSelected: (RulesTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.44f)),
    ) {
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f),
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            RulesTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = tab.localizedLabel(),
                                maxLines = 1,
                                softWrap = false,
                            )
                            if (tab == RulesTab.SUMMARY && showSummaryIndicator) {
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CountrySearchDock(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            label = { Text(uiText("Ricerca nazioni", "Country search")) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = uiText("Cancella ricerca", "Clear search"))
                }
            },
        )
    }
}

private fun Modifier.rulesTabGestureNavigation(
    selectedTab: RulesTab,
    swipeThresholdPx: Float,
    onTabSelected: (RulesTab) -> Unit,
    onSwipePastStart: () -> Unit,
    onSwipePastEnd: () -> Unit,
): Modifier = pointerInput(selectedTab, swipeThresholdPx) {
    detectRulesTabSwipe(
        selectedTab = selectedTab,
        swipeThresholdPx = swipeThresholdPx,
        onTabSelected = onTabSelected,
        onSwipePastStart = onSwipePastStart,
        onSwipePastEnd = onSwipePastEnd,
    )
}

private suspend fun PointerInputScope.detectRulesTabSwipe(
    selectedTab: RulesTab,
    swipeThresholdPx: Float,
    onTabSelected: (RulesTab) -> Unit,
    onSwipePastStart: () -> Unit,
    onSwipePastEnd: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var totalX = 0f
        var totalY = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break

            val delta = change.positionChange()
            totalX += delta.x
            totalY += delta.y

            val isHorizontalSwipe = kotlin.math.abs(totalX) > swipeThresholdPx &&
                kotlin.math.abs(totalX) > kotlin.math.abs(totalY) * 1.35f
            if (isHorizontalSwipe) {
                val currentIndex = RulesTab.entries.indexOf(selectedTab)
                val targetIndex = if (totalX < 0f) currentIndex + 1 else currentIndex - 1
                val targetTab = RulesTab.entries.getOrNull(targetIndex)
                if (targetTab != null) {
                    onTabSelected(targetTab)
                } else if (totalX < 0f) {
                    onSwipePastEnd()
                } else {
                    onSwipePastStart()
                }
                change.consume()
                break
            }
        }
    }
}

@Composable
private fun ConfiguredRuleRow(
    title: String,
    subtitle: String,
    detail: String,
    selected: Boolean,
    enabled: Boolean,
    onSelectedChange: ((Boolean) -> Unit)?,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    toggleHelp: HelpContent,
    deleteHelp: HelpContent,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                if (onSelectedChange != null) {
                    Checkbox(checked = selected, onCheckedChange = onSelectedChange)
                    Spacer(Modifier.width(8.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ContextualHelpButton(toggleHelp)
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
                ContextualHelpButton(deleteHelp)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = uiText("Elimina", "Delete"))
                }
            }
        }
    }
}

@Composable
private fun CountryOverviewRow(
    country: CountryRuleEntity,
    foreignMode: ForeignCallMode,
    onReset: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text("${country.flag} ${country.displayName()}", style = MaterialTheme.typography.titleSmall)
                Text(
                    uiText("Nazione - ${countryStatusLabel(country.status, foreignMode)}", "Country - ${countryStatusLabel(country.status, foreignMode)}"),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    country.dialCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onReset) {
                Icon(Icons.Default.Delete, contentDescription = uiText("Ripristina nazione", "Reset country"))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockGroupsSection(
    blockGroups: List<BlockGroupUiModel>,
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onPickContact: (Long) -> Unit,
    onUpdateGroup: (Long, String) -> Unit,
    onToggleGroup: (BlockGroupEntity, Boolean) -> Unit,
    onClearGroup: (BlockGroupEntity) -> Unit,
    onDeleteGroup: (BlockGroupEntity) -> Unit,
    onDeleteContact: (BlockGroupContactUiModel) -> Unit,
) {
    SectionCard(
        title = uiText("Gruppi di blocco", "Block groups"),
        trailing = {
            ContextualHelpButton(blockGroupsHelp())
        },
    ) {
        OutlinedTextField(
            value = groupName,
            onValueChange = onGroupNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(uiText("Nome gruppo", "Group name")) },
            placeholder = { Text(uiText("Amici", "Friends")) },
            singleLine = true,
        )
        Button(
            enabled = groupName.isNotBlank(),
            onClick = onCreateGroup,
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(uiText("Crea gruppo", "Create group"))
        }
        ContextualHelpButton(blockGroupsHelp())
        if (blockGroups.isEmpty()) {
            GuidanceText(uiText("Nessun gruppo creato.", "No groups created."))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                blockGroups.forEach { model ->
                    key(model.group.id) {
                        BlockGroupCard(
                            model = model,
                            onPickContact = onPickContact,
                            onUpdateGroup = onUpdateGroup,
                            onToggleGroup = onToggleGroup,
                            onClearGroup = onClearGroup,
                            onDeleteGroup = onDeleteGroup,
                            onDeleteContact = onDeleteContact,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockGroupCard(
    model: BlockGroupUiModel,
    onPickContact: (Long) -> Unit,
    onUpdateGroup: (Long, String) -> Unit,
    onToggleGroup: (BlockGroupEntity, Boolean) -> Unit,
    onClearGroup: (BlockGroupEntity) -> Unit,
    onDeleteGroup: (BlockGroupEntity) -> Unit,
    onDeleteContact: (BlockGroupContactUiModel) -> Unit,
) {
    val group = model.group
    var nameDraft by remember(group.id, group.name) { mutableStateOf(group.name) }
    val hasDraftChanges = nameDraft.trim() != group.name

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(group.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        uiText("${model.contacts.size} contatti", "${model.contacts.size} contacts"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (group.enabled) {
                                uiText("Attivo", "Active")
                            } else {
                                uiText("In pausa", "Paused")
                            }
                        )
                    },
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = if (group.enabled) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(uiText("Gruppo attivo", "Group active"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (group.enabled) {
                                uiText("Blocca questi contatti quando chiamano.", "Blocks these contacts when they call.")
                            } else {
                                uiText("Gruppo salvato ma sospeso: le chiamate passano normalmente.", "Group saved but paused: calls are handled normally.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ContextualHelpButton(blockGroupActivationHelp())
                        Switch(checked = group.enabled, onCheckedChange = { onToggleGroup(group, it) })
                    }
                }
            }
            OutlinedTextField(
                value = nameDraft,
                onValueChange = { nameDraft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(uiText("Nome", "Name")) },
                singleLine = true,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    enabled = hasDraftChanges && nameDraft.isNotBlank(),
                    onClick = { onUpdateGroup(group.id, nameDraft) },
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Text(uiText("Salva", "Save"))
                }
                OutlinedButton(onClick = { onPickContact(group.id) }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Text(uiText("Rubrica", "Contacts"))
                }
                OutlinedButton(
                    enabled = model.contacts.isNotEmpty(),
                    onClick = { onClearGroup(group) },
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(uiText("Svuota", "Clear"))
                }
                OutlinedButton(onClick = { onDeleteGroup(group) }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(uiText("Elimina", "Delete"))
                }
            }
            if (model.contacts.isEmpty()) {
                GuidanceText(uiText("Nessun contatto nel gruppo.", "No contacts in this group."))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    model.contacts.forEach { contact ->
                        BlockGroupContactRow(
                            contact = contact,
                            onDelete = { onDeleteContact(contact) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockGroupContactRow(
    contact: BlockGroupContactUiModel,
    onDelete: () -> Unit,
) {
    val detail = if (contact.phoneNumbers.size == 1) {
        contact.phoneNumbers.first()
    } else {
        uiText("${contact.phoneNumbers.size} numeri bloccati", "${contact.phoneNumbers.size} blocked numbers")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(contact.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = uiText("Rimuovi contatto", "Remove contact"))
        }
    }
}

@Composable
private fun ForeignScheduleSection(
    start: String,
    end: String,
    scheduleRules: List<RuleEntity>,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onAddSchedule: () -> Unit,
    onToggleSchedule: (RuleEntity, Boolean) -> Unit,
    onDeleteSchedule: (RuleEntity) -> Unit,
) {
    SectionCard(
        title = uiText("Programmazione esteri", "Foreign schedule"),
        trailing = {
            ContextualHelpButton(
                title = uiText("Fasce orarie esteri", "Foreign time windows"),
                explanation = uiText("Le fasce sono usate quando in Opzioni scegli Chiamate estere: Programmata.", "Time windows are used when Settings > Foreign calls is set to Scheduled."),
                benefits = uiText("Applica i blocchi per nazione solo negli orari in cui ti servono.", "Applies country blocks only at the times you need."),
                drawbacks = uiText("Fuori fascia le chiamate estere ricevono solo un avviso, salvo altre regole.", "Outside the window, foreign calls only receive a warning unless other rules apply."),
                advice = uiText("Crea fasce per lavoro, reperibilità o periodi in cui non attendi chiamate internazionali.", "Create windows for work, on-call periods, or times when you do not expect international calls."),
                androidLimits = uiText("Android consegna la chiamata in arrivo; CallGuardian valuta l'orario locale del dispositivo.", "Android delivers the incoming call; CallGuardian evaluates the device's local time."),
            )
        }
    ) {
        GuidanceText(
            uiText("Aggiungi le fasce in cui applicare le nazioni bloccate o consentite.", "Add the time windows where blocked or allowed countries apply.")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = start,
                onValueChange = onStartChange,
                modifier = Modifier.weight(1f),
                label = { Text(uiText("Dalle", "From")) },
                placeholder = { Text("09:00") },
                singleLine = true,
            )
            OutlinedTextField(
                value = end,
                onValueChange = onEndChange,
                modifier = Modifier.weight(1f),
                label = { Text(uiText("Alle", "To")) },
                placeholder = { Text("18:00") },
                singleLine = true,
            )
        }
        Button(onClick = onAddSchedule) {
            Text(uiText("Salva fascia", "Save window"))
        }
        if (scheduleRules.isEmpty()) {
            GuidanceText(uiText("Nessuna fascia salvata.", "No saved time windows."))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                scheduleRules.forEach { rule ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(rule.label, style = MaterialTheme.typography.titleSmall)
                            Text(scheduleRuleLabel(rule), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = rule.enabled, onCheckedChange = { onToggleSchedule(rule, it) })
                            IconButton(onClick = { onDeleteSchedule(rule) }) {
                                Icon(Icons.Default.Delete, contentDescription = uiText("Elimina", "Delete"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidanceText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun CountryRulesSection(
    countries: List<CountryRuleEntity>,
    countryQuery: String,
    onCountryQueryChange: (String) -> Unit,
    foreignMode: ForeignCallMode,
    onStatusSelected: (CountryRuleEntity, CountryStatus) -> Unit,
) {
    val filteredCountries = remember(countries, countryQuery) {
        if (countryQuery.isBlank()) {
            emptyList()
        } else {
            countries.filter { country -> country.matchesCountryQuery(countryQuery) }
        }
    }
    val resultsBringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(countryQuery, filteredCountries.size) {
        if (countryQuery.isNotBlank()) {
            delay(120)
            resultsBringIntoViewRequester.bringIntoView()
        }
    }

    SectionCard(
        title = uiText("Nazioni", "Countries"),
        trailing = {
            ContextualHelpButton(
                title = uiText("Regole nazioni", "Country rules"),
                explanation = countryModeExplanation(foreignMode),
                benefits = uiText("Mantieni controllo sui prefissi esteri senza cancellare scelte precedenti.", "Keep control over foreign prefixes without deleting previous choices."),
                drawbacks = uiText("Il prefisso non certifica sempre la posizione reale del chiamante.", "The prefix does not always certify the caller's real location."),
                advice = countryModeAdvice(foreignMode),
                androidLimits = uiText("Android fornisce il numero, non una reputazione ufficiale della chiamata.", "Android provides the number, not an official call reputation."),
            )
        }
    ) {
        GuidanceText(countryModeExplanation(foreignMode))
        GuidanceText(uiText("Le scelte restano salvate quando cambi modalità.", "Choices stay saved when you change mode."))
        OutlinedTextField(
            value = countryQuery,
            onValueChange = onCountryQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(uiText("Cerca nazione o prefisso", "Search country or prefix")) },
            singleLine = true,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(resultsBringIntoViewRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                countryQuery.isBlank() -> {
                    Text(
                        uiText("Cerca per nome o prefisso: Francia, America, +33.", "Search by name or prefix: France, America, +33."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                filteredCountries.isEmpty() -> {
                    Text(
                        uiText("Nessuna corrispondenza.", "No matches."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Text(
                        uiText("${filteredCountries.size} risultati", "${filteredCountries.size} results"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredCountries, key = { it.iso }) { country ->
                            CountryRuleRow(
                                country = country,
                                foreignMode = foreignMode,
                                onStatusSelected = { status -> onStatusSelected(country, status) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountryRuleRow(
    country: CountryRuleEntity,
    foreignMode: ForeignCallMode,
    onStatusSelected: (CountryStatus) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${country.flag} ${country.displayName()}", style = MaterialTheme.typography.titleSmall)
                Text(country.dialCode, style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(countryStatusLabel(country.status, foreignMode)) })
                ContextualHelpButton(countryStatusSummaryHelp(country))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            countryStatusesForMode(foreignMode).forEach { status ->
                HelpedFilterChip(
                    selected = country.status == status,
                    onClick = { onStatusSelected(status) },
                    label = countryStatusLabel(status, foreignMode),
                    help = countryStatusHelp(country.displayName(), status, foreignMode),
                )
            }
        }
    }
}

@Composable
private fun HelpedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    help: HelpContent,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label) },
            leadingIcon = leadingIcon,
        )
        ContextualHelpButton(help)
    }
}

private fun blockNumberHelp() = HelpContent(uiText("Blocca numero", "Block number"), uiText("Aggiunge il numero alla lista bloccati.", "Adds the number to the blocked list."), uiText("Massima precisione contro un chiamante specifico.", "Maximum precision against a specific caller."), uiText("Se il numero è scritto male o cambia formato, la regola può non agganciarsi.", "If the number is typed badly or changes format, the rule may not match."))

private fun allowNumberHelp() = HelpContent(uiText("Consenti numero", "Allow number"), uiText("Aggiunge il numero alla lista consentiti.", "Adds the number to the allowed list."), uiText("Protegge contatti importanti da blocchi accidentali.", "Protects important contacts from accidental blocks."), uiText("Un numero consentito passa anche se in futuro sembra sospetto.", "An allowed number gets through even if it later looks suspicious."))

private fun blockPrefixHelp() = HelpContent(uiText("Blocca pattern", "Block pattern"), uiText("Blocca ogni numero che inizia con il pattern indicato.", "Blocks every number that starts with the specified pattern."), uiText("Ferma famiglie di numeri indesiderati che condividono la stessa parte iniziale.", "Stops families of unwanted numbers sharing the same start."), uiText("Un pattern troppo corto può coinvolgere chiamate legittime.", "A pattern that is too short may affect legitimate calls."))

private fun blockGroupsHelp() = HelpContent(
    title = uiText("Gruppi di blocco", "Block groups"),
    explanation = uiText(
        "Un gruppo di blocco raccoglie contatti della rubrica: quando uno di loro chiama, CallGuardian blocca la chiamata.",
        "A block group collects contacts from your address book: when one of them calls, CallGuardian blocks the call.",
    ),
    benefits = uiText(
        "Ti permette di gestire amici, lavoro o altri contatti senza creare una regola manuale per ogni numero.",
        "It lets you manage friends, work, or other contacts without creating a manual rule for every number.",
    ),
    drawbacks = uiText(
        "Se un contatto ha piu numeri, vengono bloccati tutti ma il contatto appare una sola volta.",
        "If a contact has multiple numbers, all are blocked but the contact appears once.",
    ),
    advice = uiText(
        "Crea il gruppo, poi premi Rubrica e scegli i contatti.",
        "Create the group, then press Contacts and choose contacts.",
    ),
    androidLimits = uiText(
        "Il blocco effettivo richiede il ruolo ID chiamante e spam assegnato a CallGuardian.",
        "Actual blocking requires the Caller ID and spam role assigned to CallGuardian.",
    ),
    examples = listOf(
        uiText("Gruppo Amici: blocca temporaneamente le chiamate da quei contatti.", "Friends group: temporarily blocks calls from those contacts."),
        uiText("Gruppo Lavoro: mettilo in pausa quando vuoi ricevere di nuovo quelle chiamate.", "Work group: pause it when you want to receive those calls again."),
        uiText("Svuota mantiene il gruppo ma rimuove tutti i contatti; Elimina cancella tutto il gruppo.", "Clear keeps the group but removes all contacts; Delete removes the whole group."),
    ),
)

private fun blockGroupActivationHelp() = HelpContent(
    title = uiText("Attiva gruppo", "Enable group"),
    explanation = uiText(
        "Questo interruttore decide se il gruppo e operativo. Quando e attivo, i contatti del gruppo vengono bloccati.",
        "This switch decides whether the group is operational. When active, contacts in the group are blocked.",
    ),
    benefits = uiText(
        "Puoi riusare lo stesso gruppo nel tempo: lo spegni quando non serve e lo riaccendi senza riscrivere nomi o contatti.",
        "You can reuse the same group over time: turn it off when you do not need it and turn it back on without rewriting names or contacts.",
    ),
    drawbacks = uiText(
        "Quando e in pausa, il gruppo non applica il blocco: quei contatti saranno valutati dalle altre regole dell'app.",
        "When paused, the group does not block calls: those contacts are evaluated by the app's other rules.",
    ),
    advice = uiText(
        "Usalo per situazioni temporanee: una giornata in cui non vuoi chiamate da un gruppo, una riunione, un turno di lavoro o un periodo di riposo.",
        "Use it for temporary situations: a day when you do not want calls from a group, a meeting, a work shift, or downtime.",
    ),
    androidLimits = uiText(
        "Il flag agisce solo sulle regole di CallGuardian. Non modifica la rubrica e non cambia le impostazioni di sistema del telefono.",
        "The flag affects only CallGuardian rules. It does not modify contacts or change system phone settings.",
    ),
    examples = listOf(
        uiText("Oggi metti Amici in pausa: domani riaccendi il flag e il gruppo torna pronto.", "Pause Friends today: tomorrow turn the flag back on and the group is ready again."),
        uiText("Puoi lasciare il gruppo spento senza perdere i contatti gia selezionati.", "You can leave the group off without losing the contacts already selected."),
    ),
)

private fun ruleToggleHelp(label: String) = HelpContent(uiText("Attiva regola", "Enable rule"), uiText("Abilita o sospende la regola \"$label\".", "Enables or pauses the rule \"$label\"."), uiText("Permette prove rapide senza cancellare la configurazione.", "Allows quick tests without deleting the configuration."), uiText("Una regola disattivata non protegge finché non viene riaccesa.", "A disabled rule does not protect until it is turned back on."))

private fun ruleDeleteHelp(label: String) = HelpContent(uiText("Elimina regola", "Delete rule"), uiText("Rimuove definitivamente la regola \"$label\".", "Permanently removes the rule \"$label\"."), uiText("Pulisce regole vecchie o sbagliate.", "Cleans up old or wrong rules."), uiText("Per riaverla devi crearla di nuovo.", "To get it back, you must create it again."))

private fun scheduleRuleLabel(rule: RuleEntity): String {
    val start = rule.startsAtMinute?.toClockLabel() ?: "--:--"
    val end = rule.endsAtMinute?.toClockLabel() ?: "--:--"
    return "$start - $end"
}

private fun Int.toClockLabel(): String {
    val hour = this / 60
    val minute = this % 60
    return "%02d:%02d".format(hour, minute)
}

private fun countryModeExplanation(mode: ForeignCallMode): String = when (mode) {
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN -> uiText("Esteri sconosciuti bloccati. Qui scegli le nazioni sicure da escludere.", "Unknown foreign calls are blocked. Choose safe countries to exclude here.")
    ForeignCallMode.BLOCK_ALL_FOREIGN -> uiText("Tutti gli esteri bloccati. Qui scegli le nazioni autorizzate.", "All foreign calls are blocked. Choose authorized countries here.")
    ForeignCallMode.BLOCK_BY_COUNTRY -> uiText("Blocco per nazione. Qui scegli quali paesi fermare.", "Blocking by country. Choose which countries to stop here.")
    ForeignCallMode.SCHEDULED -> uiText("Modalità programmata. Prepara qui le nazioni da gestire negli orari scelti.", "Scheduled mode. Prepare the countries to manage during chosen times here.")
    ForeignCallMode.WARN_ONLY -> uiText("Solo avviso. Qui prepari eccezioni e blocchi per quando cambierai modalità.", "Warning only. Prepare exceptions and blocks here for when you change mode.")
}

private fun countryModeAdvice(mode: ForeignCallMode): String = when (mode) {
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN, ForeignCallMode.BLOCK_ALL_FOREIGN -> uiText("Escludi solo nazioni affidabili.", "Exclude only trusted countries.")
    ForeignCallMode.BLOCK_BY_COUNTRY -> uiText("Blocca solo nazioni da cui non attendi contatti.", "Block only countries you do not expect contacts from.")
    ForeignCallMode.SCHEDULED -> uiText("Usa la programmazione per orari di lavoro, reperibilità o viaggi.", "Use scheduling for work hours, on-call periods, or travel.")
    ForeignCallMode.WARN_ONLY -> uiText("Inizia osservando gli avvisi, poi passa al blocco se necessario.", "Start by watching warnings, then switch to blocking if needed.")
}

private fun countryStatusesForMode(mode: ForeignCallMode): List<CountryStatus> = when (mode) {
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN, ForeignCallMode.BLOCK_ALL_FOREIGN ->
        listOf(CountryStatus.MONITORED, CountryStatus.ALLOWED)
    ForeignCallMode.BLOCK_BY_COUNTRY ->
        listOf(CountryStatus.MONITORED, CountryStatus.BLOCKED)
    ForeignCallMode.WARN_ONLY, ForeignCallMode.SCHEDULED -> CountryStatus.entries
}

@Composable
private fun countryStatusLabel(status: CountryStatus, mode: ForeignCallMode): String = when (mode) {
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN, ForeignCallMode.BLOCK_ALL_FOREIGN -> when (status) {
        CountryStatus.ALLOWED -> stringResource(R.string.country_status_excluded)
        CountryStatus.MONITORED -> stringResource(R.string.country_status_block_active)
        CountryStatus.BLOCKED -> stringResource(R.string.country_status_block_active)
    }
    ForeignCallMode.BLOCK_BY_COUNTRY -> when (status) {
        CountryStatus.BLOCKED -> status.localizedDisplayName()
        CountryStatus.MONITORED -> stringResource(R.string.country_status_not_blocked)
        CountryStatus.ALLOWED -> stringResource(R.string.country_status_not_blocked)
    }
    ForeignCallMode.WARN_ONLY, ForeignCallMode.SCHEDULED -> status.localizedDisplayName()
}

private fun countryStatusSummaryHelp(country: CountryRuleEntity): HelpContent {
    val countryName = country.displayName()
    return HelpContent(uiText("Stato $countryName", "$countryName status"), uiText("Mostra come verrà trattata $countryName con la modalità esteri attuale.", "Shows how $countryName will be handled with the current foreign-call mode."), uiText("Ti conferma subito se il paese è escluso, osservato o bloccato.", "Confirms immediately whether the country is excluded, monitored, or blocked."), uiText("Il prefisso del numero non certifica sempre la posizione reale del chiamante.", "The number prefix does not always certify the caller's real location."))
}

private fun countryStatusHelp(countryName: String, status: CountryStatus, mode: ForeignCallMode): HelpContent = when (mode) {
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN, ForeignCallMode.BLOCK_ALL_FOREIGN -> when (status) {
        CountryStatus.ALLOWED -> HelpContent(uiText("$countryName esclusa", "$countryName excluded"), uiText("Esclude $countryName dal blocco esteri scelto in Opzioni.", "Excludes $countryName from the foreign block selected in Settings."), uiText("Utile se aspetti contatti da questa nazione.", "Useful if you expect contacts from this country."), uiText("Lascia passare anche chiamate indesiderate provenienti da quel prefisso.", "Also lets unwanted calls from that prefix through."))
        CountryStatus.MONITORED, CountryStatus.BLOCKED -> HelpContent(uiText("$countryName nel blocco", "$countryName in block"), uiText("Mantiene $countryName nella regola di blocco esteri.", "Keeps $countryName in the foreign blocking rule."), uiText("Riduce il rischio da prefissi non attesi.", "Reduces risk from unexpected prefixes."), uiText("Puoi perdere chiamate legittime da quel paese.", "You may miss legitimate calls from that country."))
    }
    ForeignCallMode.BLOCK_BY_COUNTRY -> when (status) {
        CountryStatus.BLOCKED -> HelpContent(uiText("$countryName bloccata", "$countryName blocked"), uiText("Blocca le chiamate associate a $countryName.", "Blocks calls associated with $countryName."), uiText("Riduce il rischio da prefissi internazionali indesiderati.", "Reduces risk from unwanted international prefixes."), uiText("Puoi perdere chiamate legittime da quel paese.", "You may miss legitimate calls from that country."))
        CountryStatus.MONITORED, CountryStatus.ALLOWED -> HelpContent(uiText("$countryName non bloccata", "$countryName not blocked"), uiText("Non applica blocco automatico a $countryName.", "Does not apply automatic blocking to $countryName."), uiText("Evita blocchi se aspetti contatti da questa nazione.", "Avoids blocks if you expect contacts from this country."), uiText("Potresti ricevere chiamate fastidiose.", "You may still receive nuisance calls."))
    }
    ForeignCallMode.WARN_ONLY, ForeignCallMode.SCHEDULED -> when (status) {
        CountryStatus.ALLOWED -> HelpContent(uiText("$countryName consentita", "$countryName allowed"), uiText("Consente le chiamate associate a $countryName.", "Allows calls associated with $countryName."), uiText("Evita blocchi se aspetti contatti da questa nazione.", "Avoids blocks if you expect contacts from this country."), uiText("Lascia passare anche chiamate indesiderate provenienti da quel prefisso.", "Also lets unwanted calls from that prefix through."))
        CountryStatus.MONITORED -> HelpContent(uiText("$countryName monitorata", "$countryName monitored"), uiText("Tiene la nazione sotto controllo senza blocco automatico totale.", "Keeps the country under watch without full automatic blocking."), uiText("Equilibrio utile quando non sei sicuro.", "Useful balance when you are unsure."), uiText("Potresti ricevere comunque chiamate fastidiose.", "You may still receive nuisance calls."))
        CountryStatus.BLOCKED -> HelpContent(uiText("$countryName bloccata", "$countryName blocked"), uiText("Blocca le chiamate associate a $countryName.", "Blocks calls associated with $countryName."), uiText("Riduce il rischio da prefissi internazionali indesiderati.", "Reduces risk from unwanted international prefixes."), uiText("Puoi perdere chiamate legittime da quel paese.", "You may miss legitimate calls from that country."))
    }
}

private fun CountryRuleEntity.matchesCountryQuery(query: String): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return true
    val numericQuery = normalizedQuery.filter { it.isDigit() }
    val aliases = mapOf(
        "US" to listOf("america", "americano", "usa", "uniti"),
        "GB" to listOf("inghilterra", "gran bretagna", "uk"),
    )
    return name.contains(normalizedQuery, ignoreCase = true) ||
        iso.contains(normalizedQuery, ignoreCase = true) ||
        aliases[iso].orEmpty().any { it.contains(normalizedQuery, ignoreCase = true) } ||
        dialCode.contains(normalizedQuery) ||
        (numericQuery.isNotEmpty() && dialCode.filter { it.isDigit() }.startsWith(numericQuery))
}

private fun CountryRuleEntity.displayName(): String {
    val locale = Locale.getDefault()
    return Locale("", iso)
        .getDisplayCountry(locale)
        .takeIf { it.isNotBlank() }
        ?.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
        ?: name
}
