package com.callguardian.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callguardian.app.core.model.CallAction
import com.callguardian.app.core.model.CountryStatus
import com.callguardian.app.core.model.ForeignCallMode
import com.callguardian.app.core.model.RuleType
import com.callguardian.app.data.local.CountryRuleEntity
import com.callguardian.app.data.local.RuleEntity
import com.callguardian.app.ui.components.ContextualHelpButton
import com.callguardian.app.ui.components.HelpContent
import com.callguardian.app.ui.components.SectionCard
import com.callguardian.app.ui.components.ScreenHeader
import com.callguardian.app.ui.components.SwipeToDeleteContainer
import com.callguardian.app.viewmodel.RulesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesScreen(viewModel: RulesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var number by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("") }
    var scheduleStart by remember { mutableStateOf("09:00") }
    var scheduleEnd by remember { mutableStateOf("18:00") }
    var countryQuery by remember { mutableStateOf("") }
    var simulatorNumber by remember { mutableStateOf("+393479998888") }
    var selectedRuleIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedTab by remember { mutableStateOf(RulesTab.LISTS) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val numberInputValid = isDialablePhoneInput(number)
    val prefixInputValid = isPrefixInput(prefix)
    val simulatorInputValid = isDialablePhoneInput(simulatorNumber) || simulatorNumber.trim().equals("anonimo", ignoreCase = true)
    val scheduleRules = state.rules.filter { it.type == RuleType.SCHEDULE }
    val customRules = state.rules.filterNot { it.type == RuleType.ANONYMOUS || it.type == RuleType.SCHEDULE }
    val selectedCustomRuleIds = selectedRuleIds.intersect(customRules.map { it.id }.toSet())
    val configuredCountries = state.countries
        .filter { it.status != CountryStatus.MONITORED }
        .sortedWith(compareBy<CountryRuleEntity> { it.status.ordinal }.thenBy { it.name })
    val blockedNumbers = customRules.count { it.type == RuleType.BLACKLIST_NUMBER }
    val allowedNumbers = customRules.count { it.type == RuleType.WHITELIST }
    val blockedNumberPatterns = customRules.count { it.type == RuleType.PREFIX || it.type == RuleType.RANGE }
    val blockedCountries = configuredCountries.count { it.status == CountryStatus.BLOCKED }
    val allowedCountries = configuredCountries.count { it.status == CountryStatus.ALLOWED }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item {
            ScreenHeader(
                title = "Centro regole",
                subtitle = "Numeri, pattern e nazioni in una console locale di difesa.",
            )
        }
        state.message?.let { message ->
            item {
                SectionCard(title = "Stato") {
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            RulesTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }
        when (selectedTab) {
            RulesTab.LISTS -> {
                item {
                    SectionCard(title = "Benvenuto nel Centro regole") {
                        GuidanceText("Qui decidi quali chiamate far passare e quali fermare, con regole salvate solo sul telefono.")
                        GuidanceText("Puoi consentire un contatto importante, bloccare un numero preciso o fermare numeri che iniziano con lo stesso pattern.")
                    }
                }
                item {
                    SectionCard(
                        title = "Regola per numero singolo",
                        trailing = {
                            ContextualHelpButton(
                                title = "Consentiti e bloccati",
                                explanation = "Le regole su numero esatto hanno priorità alta.",
                                benefits = "Controllo preciso senza inviare dati fuori dal telefono.",
                                drawbacks = "Richiedono inserimento manuale o azioni rapide.",
                                advice = "Usa la lista consentiti per banca, medico e contatti importanti.",
                                androidLimits = "La rubrica è leggibile solo con il permesso Lettura contatti.",
                            )
                        }
                    ) {
                        GuidanceText("Aggiungi numeri precisi da bloccare o proteggere.")
                        OutlinedTextField(
                            value = number,
                            onValueChange = { number = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Numero completo") },
                            placeholder = { Text("+393331234567") },
                            supportingText = { Text("Meglio con prefisso internazionale.") },
                            singleLine = true,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    enabled = numberInputValid,
                                    onClick = { viewModel.addBlockedNumber(number); number = "" },
                                ) { Text("Blocca") }
                                ContextualHelpButton(blockNumberHelp())
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    enabled = numberInputValid,
                                    onClick = { viewModel.addAllowedNumber(number); number = "" },
                                ) { Text("Consenti") }
                                ContextualHelpButton(allowNumberHelp())
                            }
                        }
                    }
                }
                item {
                    SectionCard(title = "Regola per pattern di numero") {
                        GuidanceText("Blocca tutti i numeri che iniziano con il pattern indicato, non solo il prefisso isolato.")
                        OutlinedTextField(
                            value = prefix,
                            onValueChange = { prefix = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Pattern iniziale del numero") },
                            placeholder = { Text("+4420 oppure 0288") },
                            supportingText = { Text("Esempio: +4420 blocca i numeri che iniziano con +4420.") },
                            singleLine = true,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                enabled = prefixInputValid,
                                onClick = { viewModel.addBlockedPrefix(prefix); prefix = "" },
                            ) { Text("Blocca pattern") }
                            ContextualHelpButton(blockPrefixHelp())
                        }
                    }
                }
            }
            RulesTab.FOREIGN -> {
                item {
                    ForeignScheduleSection(
                        enabled = state.foreignCallMode == ForeignCallMode.SCHEDULED,
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
            RulesTab.TEST -> {
                item {
                    SectionCard(
                        title = "Prova una chiamata",
                        trailing = {
                            ContextualHelpButton(
                                title = "Simulatore regole",
                                explanation = "Verifica un numero con le regole attuali.",
                                benefits = "Ti mostra subito se CallGuardian consente, avvisa o blocca.",
                                drawbacks = "La prova viene salvata nel registro locale come evento tecnico.",
                                advice = "Usalo dopo modifiche a numeri, pattern o nazioni.",
                                androidLimits = "Una prova non sostituisce una chiamata reale filtrata da Android.",
                            )
                        }
                    ) {
                        GuidanceText("Controlla in anticipo come verra trattato un numero.")
                        OutlinedTextField(
                            value = simulatorNumber,
                            onValueChange = { simulatorNumber = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Numero") },
                            placeholder = { Text("+393479998888 oppure anonimo") },
                            singleLine = true,
                        )
                        Button(
                            enabled = simulatorInputValid,
                            onClick = { viewModel.simulateCall(simulatorNumber) },
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("Prova")
                        }
                        ContextualHelpButton(simulateHelp())
                        state.simulatedDecision?.let { decision ->
                            val actionColor = when (decision.action) {
                                CallAction.BLOCKED -> MaterialTheme.colorScheme.error
                                CallAction.WARNED, CallAction.SILENCED -> MaterialTheme.colorScheme.tertiary
                                CallAction.ALLOWED -> MaterialTheme.colorScheme.primary
                            }
                            Text("Numero: ${state.simulationNumber}", style = MaterialTheme.typography.bodySmall)
                            Text("Esito: ${decision.action.displayName()}", color = actionColor, style = MaterialTheme.typography.titleMedium)
                            Text("Rischio: ${decision.riskLevel.displayName().lowercase()} - ${decision.score}")
                            Text("Motivo: ${decision.reason}")
                        }
                    }
                }
            }
            RulesTab.SUMMARY -> {
                item {
                    SectionCard(title = "Regole configurate") {
                        if (customRules.isEmpty() && scheduleRules.isEmpty() && configuredCountries.isEmpty()) {
                            GuidanceText("Le regole create appariranno qui.")
                        } else {
                            GuidanceText("Riepilogo di numeri, pattern, fasce programmate e nazioni configurate.")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AssistChip(onClick = {}, label = { Text("$blockedNumbers bloccati") })
                                AssistChip(onClick = {}, label = { Text("$allowedNumbers consentiti") })
                                AssistChip(onClick = {}, label = { Text("$blockedNumberPatterns pattern") })
                                AssistChip(onClick = {}, label = { Text("${scheduleRules.size} programmate") })
                                AssistChip(onClick = {}, label = { Text("$allowedCountries nazioni consentite") })
                                AssistChip(onClick = {}, label = { Text("$blockedCountries nazioni bloccate") })
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
                                    Text("Elimina selezionate")
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
                                            subtitle = "${rule.type.displayName()} - ${rule.action.displayName()}",
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
                                        subtitle = "Programmata - ${rule.action.displayName()}",
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
        FloatingActionButton(
            onClick = {
                selectedTab = RulesTab.LISTS
                scope.launch { listState.animateScrollToItem(3) }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi numero")
        }
    }
}

private enum class RulesTab(val label: String) {
    LISTS("Liste"),
    FOREIGN("Esteri"),
    TEST("Test"),
    SUMMARY("Sintesi"),
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

@Composable
private fun RulesTabRow(
    selectedTab: RulesTab,
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
                        Text(
                            text = tab.label,
                            maxLines = 1,
                        )
                    },
                )
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
                    Icon(Icons.Default.Delete, contentDescription = "Elimina")
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
                Text("${country.flag} ${country.name}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Nazione - ${countryStatusLabel(country.status, foreignMode)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    country.dialCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onReset) {
                Icon(Icons.Default.Delete, contentDescription = "Ripristina nazione")
            }
        }
    }
}

@Composable
private fun ForeignScheduleSection(
    enabled: Boolean,
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
        title = "Programmazione esteri",
        trailing = {
            ContextualHelpButton(
                title = "Fasce orarie esteri",
                explanation = "Le fasce sono usate quando in Opzioni scegli Chiamate estere: Programmata.",
                benefits = "Applica i blocchi per nazione solo negli orari in cui ti servono.",
                drawbacks = "Fuori fascia le chiamate estere ricevono solo un avviso, salvo altre regole.",
                advice = "Crea fasce per lavoro, reperibilità o periodi in cui non attendi chiamate internazionali.",
                androidLimits = "Android consegna la chiamata in arrivo; CallGuardian valuta l'orario locale del telefono.",
            )
        }
    ) {
        GuidanceText(
            if (enabled) {
                "Aggiungi le fasce in cui applicare le nazioni bloccate o consentite."
            } else {
                "Per usare queste fasce seleziona Programmata in Opzioni > Chiamate estere."
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = start,
                onValueChange = onStartChange,
                modifier = Modifier.weight(1f),
                label = { Text("Dalle") },
                placeholder = { Text("09:00") },
                singleLine = true,
            )
            OutlinedTextField(
                value = end,
                onValueChange = onEndChange,
                modifier = Modifier.weight(1f),
                label = { Text("Alle") },
                placeholder = { Text("18:00") },
                singleLine = true,
            )
        }
        Button(onClick = onAddSchedule, enabled = enabled) {
            Text("Salva fascia")
        }
        if (scheduleRules.isEmpty()) {
            GuidanceText("Nessuna fascia salvata.")
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
                                Icon(Icons.Default.Delete, contentDescription = "Elimina")
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

@OptIn(ExperimentalLayoutApi::class)
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
    SectionCard(
        title = "Nazioni",
        trailing = {
            ContextualHelpButton(
                title = "Regole nazioni",
                explanation = countryModeExplanation(foreignMode),
                benefits = "Mantieni controllo sui prefissi esteri senza cancellare scelte precedenti.",
                drawbacks = "Il prefisso non certifica sempre la posizione reale del chiamante.",
                advice = countryModeAdvice(foreignMode),
                androidLimits = "Android fornisce il numero, non una reputazione ufficiale della chiamata.",
            )
        }
    ) {
        GuidanceText(countryModeExplanation(foreignMode))
        GuidanceText("Le scelte restano salvate quando cambi modalità.")
        OutlinedTextField(
            value = countryQuery,
            onValueChange = onCountryQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Cerca nazione o prefisso") },
            singleLine = true,
        )
        when {
            countryQuery.isBlank() -> {
                Text(
                    "Cerca per nome o prefisso: Francia, America, +33.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            filteredCountries.isEmpty() -> {
                Text(
                    "Nessuna corrispondenza.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Text(
                    "${filteredCountries.size} risultati",
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
                Text("${country.flag} ${country.name}", style = MaterialTheme.typography.titleSmall)
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
                    help = countryStatusHelp(country.name, status, foreignMode),
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

private fun simulateHelp() = HelpContent("Simula numero", "Valuta il numero scritto usando le regole attuali.", "Aiuta a capire prima cosa succederà a una chiamata simile.", "Non garantisce che Android fornisca sempre gli stessi dati durante una chiamata reale.")

private fun blockNumberHelp() = HelpContent("Blocca numero", "Aggiunge il numero alla lista bloccati.", "Massima precisione contro un chiamante specifico.", "Se il numero è scritto male o cambia formato, la regola può non agganciarsi.")

private fun allowNumberHelp() = HelpContent("Consenti numero", "Aggiunge il numero alla lista consentiti.", "Protegge contatti importanti da blocchi accidentali.", "Un numero consentito passa anche se in futuro sembra sospetto.")

private fun blockPrefixHelp() = HelpContent("Blocca pattern", "Blocca ogni numero che inizia con il pattern indicato.", "Ferma famiglie di numeri indesiderati che condividono la stessa parte iniziale.", "Un pattern troppo corto può coinvolgere chiamate legittime.")

private fun ruleToggleHelp(label: String) = HelpContent("Attiva regola", "Abilita o sospende la regola \"$label\".", "Permette prove rapide senza cancellare la configurazione.", "Una regola disattivata non protegge finché non viene riaccesa.")

private fun ruleDeleteHelp(label: String) = HelpContent("Elimina regola", "Rimuove definitivamente la regola \"$label\".", "Pulisce regole vecchie o sbagliate.", "Per riaverla devi crearla di nuovo.")

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
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN -> "Esteri sconosciuti bloccati. Qui scegli le nazioni sicure da escludere."
    ForeignCallMode.BLOCK_ALL_FOREIGN -> "Tutti gli esteri bloccati. Qui scegli le nazioni autorizzate."
    ForeignCallMode.BLOCK_BY_COUNTRY -> "Blocco per nazione. Qui scegli quali paesi fermare."
    ForeignCallMode.SCHEDULED -> "Modalità programmata. Prepara qui le nazioni da gestire negli orari scelti."
    ForeignCallMode.WARN_ONLY -> "Solo avviso. Qui prepari eccezioni e blocchi per quando cambierai modalità."
}

private fun countryModeAdvice(mode: ForeignCallMode): String = when (mode) {
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN, ForeignCallMode.BLOCK_ALL_FOREIGN -> "Escludi solo nazioni affidabili."
    ForeignCallMode.BLOCK_BY_COUNTRY -> "Blocca solo nazioni da cui non attendi contatti."
    ForeignCallMode.SCHEDULED -> "Usa la programmazione per orari di lavoro, reperibilità o viaggi."
    ForeignCallMode.WARN_ONLY -> "Inizia osservando gli avvisi, poi passa al blocco se necessario."
}

private fun countryStatusesForMode(mode: ForeignCallMode): List<CountryStatus> = when (mode) {
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN, ForeignCallMode.BLOCK_ALL_FOREIGN ->
        listOf(CountryStatus.MONITORED, CountryStatus.ALLOWED)
    ForeignCallMode.BLOCK_BY_COUNTRY ->
        listOf(CountryStatus.MONITORED, CountryStatus.BLOCKED)
    ForeignCallMode.WARN_ONLY, ForeignCallMode.SCHEDULED -> CountryStatus.entries
}

private fun countryStatusLabel(status: CountryStatus, mode: ForeignCallMode): String = when (mode) {
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN, ForeignCallMode.BLOCK_ALL_FOREIGN -> when (status) {
        CountryStatus.ALLOWED -> "Esclusa"
        CountryStatus.MONITORED -> "Blocco attivo"
        CountryStatus.BLOCKED -> "Blocco attivo"
    }
    ForeignCallMode.BLOCK_BY_COUNTRY -> when (status) {
        CountryStatus.BLOCKED -> "Bloccata"
        CountryStatus.MONITORED -> "Non bloccata"
        CountryStatus.ALLOWED -> "Non bloccata"
    }
    ForeignCallMode.WARN_ONLY, ForeignCallMode.SCHEDULED -> status.displayName()
}

private fun countryStatusSummaryHelp(country: CountryRuleEntity) = HelpContent("Stato ${country.name}", "Mostra come verrà trattata ${country.name} con la modalità esteri attuale.", "Ti conferma subito se il paese è escluso, osservato o bloccato.", "Il prefisso del numero non certifica sempre la posizione reale del chiamante.")

private fun countryStatusHelp(countryName: String, status: CountryStatus, mode: ForeignCallMode): HelpContent = when (mode) {
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN, ForeignCallMode.BLOCK_ALL_FOREIGN -> when (status) {
        CountryStatus.ALLOWED -> HelpContent("$countryName esclusa", "Esclude $countryName dal blocco esteri scelto in Opzioni.", "Utile se aspetti contatti da questa nazione.", "Lascia passare anche chiamate indesiderate provenienti da quel prefisso.")
        CountryStatus.MONITORED, CountryStatus.BLOCKED -> HelpContent("$countryName nel blocco", "Mantiene $countryName nella regola di blocco esteri.", "Riduce il rischio da prefissi non attesi.", "Puoi perdere chiamate legittime da quel paese.")
    }
    ForeignCallMode.BLOCK_BY_COUNTRY -> when (status) {
        CountryStatus.BLOCKED -> HelpContent("$countryName bloccata", "Blocca le chiamate associate a $countryName.", "Riduce il rischio da prefissi internazionali indesiderati.", "Puoi perdere chiamate legittime da quel paese.")
        CountryStatus.MONITORED, CountryStatus.ALLOWED -> HelpContent("$countryName non bloccata", "Non applica blocco automatico a $countryName.", "Evita blocchi se aspetti contatti da questa nazione.", "Potresti ricevere chiamate fastidiose.")
    }
    ForeignCallMode.WARN_ONLY, ForeignCallMode.SCHEDULED -> when (status) {
        CountryStatus.ALLOWED -> HelpContent("$countryName consentita", "Consente le chiamate associate a $countryName.", "Evita blocchi se aspetti contatti da questa nazione.", "Lascia passare anche chiamate indesiderate provenienti da quel prefisso.")
        CountryStatus.MONITORED -> HelpContent("$countryName monitorata", "Tiene la nazione sotto controllo senza blocco automatico totale.", "Equilibrio utile quando non sei sicuro.", "Potresti ricevere comunque chiamate fastidiose.")
        CountryStatus.BLOCKED -> HelpContent("$countryName bloccata", "Blocca le chiamate associate a $countryName.", "Riduce il rischio da prefissi internazionali indesiderati.", "Puoi perdere chiamate legittime da quel paese.")
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
