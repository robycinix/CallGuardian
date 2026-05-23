package com.callguardian.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callguardian.app.R
import com.callguardian.app.LocalPermissionActions
import com.callguardian.app.core.model.AnonymousMode
import com.callguardian.app.core.model.CallAction
import com.callguardian.app.core.model.ForeignCallMode
import com.callguardian.app.core.model.PermissionSummary
import com.callguardian.app.core.model.ProtectionLevel
import com.callguardian.app.core.model.ThemeMode
import com.callguardian.app.core.model.ThemePalette
import com.callguardian.app.core.model.resolveInternationalDialingInput
import com.callguardian.app.ui.components.ContextualHelpButton
import com.callguardian.app.ui.components.HelpContent
import com.callguardian.app.ui.components.InternationalDialingInput
import com.callguardian.app.ui.components.SectionCard
import com.callguardian.app.ui.components.ScreenHeader
import com.callguardian.app.ui.components.rememberDefaultDialingCountry
import com.callguardian.app.viewmodel.ProtectionViewModel
import com.callguardian.app.viewmodel.RulesViewModel
import com.callguardian.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    protectionViewModel: ProtectionViewModel = hiltViewModel(),
    rulesViewModel: RulesViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val protectionState by protectionViewModel.uiState.collectAsStateWithLifecycle()
    val rulesState by rulesViewModel.uiState.collectAsStateWithLifecycle()
    val permissionActions = LocalPermissionActions.current
    val context = LocalContext.current
    val defaultDialingCountry = rememberDefaultDialingCountry()
    val scope = rememberCoroutineScope()
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    var simulatorCountry by remember { mutableStateOf(defaultDialingCountry) }
    var simulatorNumber by remember { mutableStateOf("") }
    val simulatorInputValid = isSettingsSimulatorInput(simulatorNumber)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val json = pendingBackupJson ?: return@rememberLauncherForActivityResult
        pendingBackupJson = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error(uiText("Impossibile aprire il file", "Unable to open the file"))
                }
            }.onSuccess {
                Toast.makeText(context, uiText("Backup esportato", "Backup exported"), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, uiText("Export non riuscito: ${it.message}", "Export failed: ${it.message}"), Toast.LENGTH_LONG).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error(uiText("Impossibile leggere il file", "Unable to read the file"))
                }
                viewModel.importBackupJson(json)
            }.onSuccess {
                Toast.makeText(context, uiText("Backup ripristinato", "Backup restored"), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, uiText("Import non riuscito: ${it.message}", "Import failed: ${it.message}"), Toast.LENGTH_LONG).show()
            }
        }
    }
    LaunchedEffect(Unit) { protectionViewModel.refreshPermissions() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = uiText("Opzioni", "Settings"),
            subtitle = uiText("Comportamento, tema, permessi e backup governati da un solo pannello.", "Behavior, theme, permissions, and backup managed from one panel."),
        )
        SectionCard(title = uiText("Modalità protezione", "Protection mode")) {
            Text(
                uiText("Bilanciata è la scelta consigliata: protegge senza diventare troppo severa.", "Balanced is the recommended choice: it protects without becoming too strict."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EnumOptionList(
                values = ProtectionLevel.entries,
                selected = settings.protectionLevel,
                labelFor = { it.localizedDisplayName() },
                descriptionFor = ::protectionDescription,
                onSelected = viewModel::setProtectionLevel,
                helpFor = ::protectionHelp,
            )
        }
        SectionCard(
            title = uiText("Numeri anonimi", "Anonymous numbers"),
            trailing = {
                ContextualHelpButton(
                    title = uiText("Gestione anonimi", "Anonymous handling"),
                    explanation = uiText("Definisce come trattare chiamate senza numero visibile.", "Defines how to handle calls with no visible number."),
                    benefits = uiText("Riduce chiamate moleste non identificabili.", "Reduces unidentified nuisance calls."),
                    drawbacks = uiText("Alcune chiamate legittime usano numero privato.", "Some legitimate calls use a private number."),
                    advice = uiText("Inizia con Avviso, poi passa a Blocca se necessario.", "Start with Warning, then switch to Block if needed."),
                    androidLimits = uiText("Il servizio riceve solo le informazioni esposte dall'operatore telefonico.", "The service receives only the information exposed by the phone carrier."),
                )
            }
        ) {
            Text(
                uiText("Decidi cosa fare quando il numero non è visibile.", "Decide what to do when the number is not visible."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EnumOptionList(
                values = AnonymousMode.entries,
                selected = settings.anonymousMode,
                labelFor = { it.localizedDisplayName() },
                descriptionFor = ::anonymousDescription,
                onSelected = viewModel::setAnonymousMode,
                helpFor = ::anonymousHelp,
            )
            if (settings.anonymousMode == AnonymousMode.ALLOW_AFTER_REPEATED_ATTEMPTS) {
                Text(
                    uiText("Dopo quanti tentativi deve passare una chiamata anonima?", "After how many attempts should an anonymous call be allowed?"),
                    style = MaterialTheme.typography.titleSmall,
                )
                AttemptPicker(
                    selected = settings.allowRepeatedAnonymousAfterAttempts,
                    onSelected = viewModel::setRepeatedAnonymousAttempts,
                )
            }
        }
        SectionCard(title = uiText("Chiamate estere", "Foreign calls")) {
            Text(
                uiText("Utile contro frodi internazionali, ma scegli un livello compatibile con lavoro, viaggi e familiari.", "Useful against international fraud, but choose a level compatible with work, travel, and family."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EnumOptionList(
                values = ForeignCallMode.entries,
                selected = settings.foreignCallMode,
                labelFor = { it.localizedDisplayName() },
                descriptionFor = ::foreignDescription,
                onSelected = viewModel::setForeignMode,
                helpFor = ::foreignHelp,
            )
        }
        SectionCard(
            title = uiText("Test chiamata", "Call test"),
            trailing = {
                ContextualHelpButton(callTestHelp())
            },
        ) {
            Text(
                uiText("Controlla in anticipo come verra trattato un numero con le regole attuali.", "Check in advance how a number will be handled by the current rules."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InternationalDialingInput(
                value = simulatorNumber,
                onValueChange = { simulatorNumber = it },
                selectedCountry = simulatorCountry,
                onCountrySelected = { simulatorCountry = it },
                label = uiText("Numero", "Number"),
                placeholder = uiText("3479998888 oppure anonimo", "5551234567 or anonymous"),
                supportingText = uiText("Il prefisso selezionato viene aggiunto se non scrivi gia + o 00.", "The selected prefix is added unless you already type + or 00."),
                allowAnonymous = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    enabled = simulatorInputValid,
                    onClick = { rulesViewModel.simulateCall(resolveInternationalDialingInput(simulatorNumber, simulatorCountry)) },
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(uiText("Prova", "Test"))
                }
                ContextualHelpButton(callTestHelp())
            }
            rulesState.simulatedDecision?.let { decision ->
                val actionColor = when (decision.action) {
                    CallAction.BLOCKED -> MaterialTheme.colorScheme.error
                    CallAction.WARNED, CallAction.SILENCED -> MaterialTheme.colorScheme.tertiary
                    CallAction.ALLOWED -> MaterialTheme.colorScheme.primary
                }
                Text(stringResource(R.string.simulation_number_format, rulesState.simulationNumber), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.simulation_outcome_format, decision.action.localizedDisplayName()), color = actionColor, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.simulation_risk_format, decision.riskLevel.localizedDisplayName().lowercase(), decision.score))
                Text(stringResource(R.string.simulation_reason_format, decision.reason))
            }
        }
        SectionCard(title = uiText("Tema", "Theme")) {
            Text(
                uiText("Personalizza leggibilità e colori senza cambiare le regole di protezione.", "Customize readability and colors without changing protection rules."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(uiText("Aspetto", "Appearance"), style = MaterialTheme.typography.titleSmall)
            CompactEnumRow(ThemeMode.entries, settings.themeMode, { it.localizedDisplayName() }, viewModel::setThemeMode)
            Text(uiText("Colori", "Colors"), style = MaterialTheme.typography.titleSmall)
            CompactEnumRow(ThemePalette.entries, settings.palette, { it.localizedDisplayName() }, viewModel::setPalette)
            SettingSwitch(uiText("Alto contrasto", "High contrast"), settings.highContrast, viewModel::setHighContrast, highContrastHelp())
        }
        SectionCard(title = stringResource(R.string.settings_language_title)) {
            Text(
                stringResource(R.string.settings_language_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LanguagePicker(
                selected = settings.languageCode,
                onSelected = viewModel::setLanguageCode,
            )
        }
        SectionCard(title = uiText("Aiuti contestuali", "Contextual help")) {
            SettingSwitch(uiText("Mostra pulsanti informativi", "Show info buttons"), settings.contextualHelpEnabled, viewModel::setContextualHelp, contextualHelpHelp())
        }
        SectionCard(title = uiText("Permessi Android", "Android permissions")) {
            AndroidPermissionsPanel(
                permissions = protectionState.permissions,
                onRuntimePermissions = permissionActions.requestRuntimePermissions,
                onCallScreeningRole = permissionActions.requestCallScreeningRole,
                onOverlaySettings = permissionActions.openOverlaySettings,
            )
        }
        SectionCard(title = uiText("Backup locale", "Local backup")) {
            Text(uiText("Esporta o ripristina impostazioni, regole, paesi e ultimi eventi in un file JSON locale.", "Export or restore settings, rules, countries, and recent events to a local JSON file."))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            runCatching { viewModel.exportBackupJson() }
                                .onSuccess { json ->
                                    pendingBackupJson = json
                                    exportLauncher.launch("callguardian-backup.json")
                                }
                                .onFailure {
                                    Toast.makeText(context, uiText("Export non riuscito: ${it.message}", "Export failed: ${it.message}"), Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                ) { Text(uiText("Esporta JSON", "Export JSON")) }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                ) { Text(uiText("Importa JSON", "Import JSON")) }
            }
        }
        SectionCard(title = uiText("Sostieni CallGuardian", "Support CallGuardian")) {
            DonationButton()
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LanguagePicker(
    selected: String,
    onSelected: (String) -> Unit,
) {
    val options = listOf(
        LanguageOption("system", stringResource(R.string.language_system)),
        LanguageOption("it", stringResource(R.string.language_italian)),
        LanguageOption("en", stringResource(R.string.language_english)),
        LanguageOption("es", stringResource(R.string.language_spanish)),
        LanguageOption("fr", stringResource(R.string.language_french)),
        LanguageOption("de", stringResource(R.string.language_german)),
        LanguageOption("pt", stringResource(R.string.language_portuguese)),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = selected == option.code
            Surface(
                modifier = Modifier.clickable { onSelected(option.code) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.content_selected))
                    }
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

private data class LanguageOption(val code: String, val label: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AndroidPermissionsPanel(
    permissions: PermissionSummary?,
    onRuntimePermissions: () -> Unit,
    onCallScreeningRole: () -> Unit,
    onOverlaySettings: () -> Unit,
) {
    PermissionLine(uiText("Permesso rubrica", "Contacts permission"), permissions?.runtimePermissionsGranted == true)
    PermissionLine(uiText("Ruolo ID chiamante e spam", "Caller ID and spam role"), permissions?.callScreeningRoleHeld == true)
    PermissionLine(uiText("Notifiche", "Notifications"), permissions?.notificationPermissionGranted == true)
    PermissionLine(uiText("Popup sovrapposto", "Overlay popup"), permissions?.overlayAllowed == true)

    if (permissions?.isSetupComplete() == true) {
        Text(uiText("Tutto configurato. Nessuna azione richiesta.", "Everything is configured. No action required."), style = MaterialTheme.typography.bodyMedium)
        return
    }

    val runtimeOrNotificationMissing =
        permissions == null ||
            !permissions.runtimePermissionsGranted ||
            !permissions.notificationPermissionGranted
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (runtimeOrNotificationMissing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onRuntimePermissions) { Text(uiText("Concedi permessi", "Grant permissions")) }
                ContextualHelpButton(runtimePermissionsHelp())
            }
        }
        if (permissions?.callScreeningRoleHeld != true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onCallScreeningRole) { Text(uiText("Attiva filtro chiamate", "Enable call filter")) }
                ContextualHelpButton(callScreeningRoleHelp())
            }
        }
        if (permissions?.overlayAllowed != true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onOverlaySettings) { Text(uiText("Mostra avviso in chiamata", "Show in-call warning")) }
                ContextualHelpButton(overlayHelp())
            }
        }
    }
}

@Composable
private fun PermissionLine(label: String, ok: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Icon(
            imageVector = if (ok) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = if (ok) uiText("Concesso", "Granted") else uiText("Richiesto", "Required"),
            tint = if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun DonationButton() {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(uiText("Se l'app ti è utile, puoi contribuire allo sviluppo con una donazione libera.", "If the app is useful to you, you can support development with a voluntary donation."))
        Button(
            onClick = { uriHandler.openUri(PayPalDonationUrl) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(uiText("Dona con PayPal", "Donate with PayPal"))
        }
    }
}

@Composable
private fun <T : Enum<T>> EnumOptionList(
    values: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    descriptionFor: (T) -> String,
    onSelected: (T) -> Unit,
    helpFor: (T) -> HelpContent,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            val isSelected = selected == value
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(value) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioButton(selected = isSelected, onClick = { onSelected(value) })
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = labelFor(value),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            text = descriptionFor(value),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = uiText("Selezionata", "Selected"))
                    }
                    ContextualHelpButton(helpFor(value))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun <T : Enum<T>> CompactEnumRow(
    values: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            val isSelected = selected == value
            Surface(
                modifier = Modifier.clickable { onSelected(value) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = uiText("Selezionata", "Selected"))
                    }
                    Text(
                        text = labelFor(value),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AttemptPicker(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(2, 3, 5, 10).forEach { attempts ->
            val isSelected = selected == attempts
            Surface(
                modifier = Modifier.clickable { onSelected(attempts) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = uiText("Selezionata", "Selected"))
                    }
                    Text(
                        text = uiText("$attempts tentativi", "$attempts attempts"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit, help: HelpContent) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContextualHelpButton(help)
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

private fun protectionHelp(value: ProtectionLevel): HelpContent = when (value) {
    ProtectionLevel.OFF -> HelpContent(uiText("Disattivata", "Off"), uiText("Spegne la valutazione automatica delle chiamate.", "Turns off automatic call evaluation."), uiText("Evita falsi positivi e lascia passare tutto.", "Avoids false positives and lets everything through."), uiText("Non protegge da spam, frodi o numeri sospetti.", "Does not protect from spam, fraud, or suspicious numbers."))
    ProtectionLevel.LIGHT -> HelpContent(uiText("Protezione leggera", "Light protection"), uiText("Applica regole prudenti solo sui casi più evidenti.", "Applies cautious rules only to the clearest cases."), uiText("Riduce il rischio di bloccare chiamate utili.", "Reduces the risk of blocking useful calls."), uiText("Lascia passare più chiamate dubbie.", "Lets more questionable calls through."))
    ProtectionLevel.BALANCED -> HelpContent(uiText("Protezione bilanciata", "Balanced protection"), uiText("Usa una soglia intermedia per avvisi e blocchi.", "Uses an intermediate threshold for warnings and blocks."), uiText("Buon equilibrio per l'uso quotidiano.", "Good balance for everyday use."), uiText("Qualche chiamata legittima potrebbe ricevere un avviso.", "Some legitimate calls may receive a warning."))
    ProtectionLevel.AGGRESSIVE -> HelpContent(uiText("Protezione aggressiva", "Aggressive protection"), uiText("Abbassa la tolleranza verso numeri sospetti.", "Lowers tolerance for suspicious numbers."), uiText("Ferma più spam e chiamate insistenti.", "Stops more spam and persistent calls."), uiText("Aumenta il rischio di blocchi o avvisi eccessivi.", "Increases the risk of excessive blocks or warnings."))
    ProtectionLevel.CUSTOM -> HelpContent(uiText("Protezione personalizzata", "Custom protection"), uiText("Riserva spazio a regole e soglie scelte dall'utente.", "Leaves room for rules and thresholds chosen by the user."), uiText("Massimo controllo quando hai esigenze specifiche.", "Maximum control when you have specific needs."), uiText("Richiede più attenzione nella configurazione.", "Requires more care during setup."))
}

private fun protectionDescription(value: ProtectionLevel): String = when (value) {
    ProtectionLevel.OFF -> uiText("Lascia passare tutto. Da usare solo per prove o pause temporanee.", "Lets everything through. Use only for tests or temporary pauses.")
    ProtectionLevel.LIGHT -> uiText("Interviene solo sui casi più evidenti, con pochi falsi allarmi.", "Acts only on the clearest cases, with fewer false alarms.")
    ProtectionLevel.BALANCED -> uiText("Consigliata per tutti i giorni: avvisa e blocca con prudenza.", "Recommended for daily use: warns and blocks carefully.")
    ProtectionLevel.AGGRESSIVE -> uiText("Più severa contro spam e numeri sospetti.", "Stricter against spam and suspicious numbers.")
    ProtectionLevel.CUSTOM -> uiText("Per chi vuole regole specifiche e maggiore controllo.", "For users who want specific rules and more control.")
}

private fun anonymousHelp(value: AnonymousMode): HelpContent = when (value) {
    AnonymousMode.WARN -> HelpContent(uiText("Anonimi: avvisa", "Anonymous: warn"), uiText("Mostra un avviso per chiamate senza numero visibile.", "Shows a warning for calls with no visible number."), uiText("Ti lascia decidere caso per caso.", "Lets you decide case by case."), uiText("La chiamata può comunque disturbare.", "The call may still disturb you."))
    AnonymousMode.SILENCE -> HelpContent(uiText("Anonimi: silenzia", "Anonymous: silence"), uiText("Riduce l'impatto delle chiamate anonime senza bloccarle.", "Reduces the impact of anonymous calls without blocking them."), uiText("Meno interruzioni durante la giornata.", "Fewer interruptions during the day."), uiText("Potresti non accorgerti subito di una chiamata legittima.", "You may not notice a legitimate call immediately."))
    AnonymousMode.BLOCK -> HelpContent(uiText("Anonimi: blocca", "Anonymous: block"), uiText("Respinge le chiamate senza identificativo.", "Rejects calls without caller ID."), uiText("Molto efficace contro chiamate moleste anonime.", "Very effective against anonymous nuisance calls."), uiText("Puoi perdere chiamate importanti da numeri privati.", "You may miss important calls from private numbers."))
    AnonymousMode.ALLOW_AFTER_REPEATED_ATTEMPTS -> HelpContent(uiText("Anonimi: ripetuti", "Anonymous: repeated"), uiText("Consente anonimi dopo tentativi ripetuti.", "Allows anonymous calls after repeated attempts."), uiText("Aiuta chi deve raggiungerti davvero.", "Helps someone who really needs to reach you."), uiText("Un chiamante molesto insistente potrebbe passare.", "A persistent nuisance caller may get through."))
}

private fun anonymousDescription(value: AnonymousMode): String = when (value) {
    AnonymousMode.WARN -> uiText("Mostra un avviso, ma ti lascia rispondere.", "Shows a warning, but lets you answer.")
    AnonymousMode.SILENCE -> uiText("Riduce il disturbo senza rifiutare la chiamata.", "Reduces disturbance without rejecting the call.")
    AnonymousMode.BLOCK -> uiText("Rifiuta le chiamate senza numero visibile.", "Rejects calls with no visible number.")
    AnonymousMode.ALLOW_AFTER_REPEATED_ATTEMPTS -> uiText("Fa passare chi richiama più volte.", "Lets through callers who try repeatedly.")
}

private fun foreignHelp(value: ForeignCallMode): HelpContent = when (value) {
    ForeignCallMode.WARN_ONLY -> HelpContent(uiText("Esteri: solo avviso", "Foreign: warn only"), uiText("Segnala le chiamate estere senza bloccarle.", "Flags foreign calls without blocking them."), uiText("Utile se ricevi chiamate internazionali legittime.", "Useful if you receive legitimate international calls."), uiText("Non ferma automaticamente frodi dall'estero.", "Does not automatically stop fraud from abroad."))
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN -> HelpContent(uiText("Esteri sconosciuti", "Unknown foreign calls"), uiText("Blocca numeri esteri non riconosciuti.", "Blocks unrecognized foreign numbers."), uiText("Riduce spam internazionale mantenendo margine per contatti noti.", "Reduces international spam while leaving room for known contacts."), uiText("Un numero nuovo ma legittimo può essere bloccato.", "A new but legitimate number may be blocked."))
    ForeignCallMode.BLOCK_ALL_FOREIGN -> HelpContent(uiText("Blocca tutti gli esteri", "Block all foreign calls"), uiText("Respinge ogni chiamata con prefisso internazionale.", "Rejects every call with an international prefix."), uiText("Massima protezione se non aspetti chiamate dall'estero.", "Maximum protection if you do not expect calls from abroad."), uiText("Troppo rigida per lavoro, viaggi o familiari fuori Italia.", "Too strict for work, travel, or family outside Italy."))
    ForeignCallMode.BLOCK_BY_COUNTRY -> HelpContent(uiText("Blocca per nazione", "Block by country"), uiText("Usa le regole paese impostate nella schermata Regole.", "Uses country rules configured in the Rules screen."), uiText("Controllo preciso sui prefissi internazionali.", "Precise control over international prefixes."), uiText("I prefissi non sempre indicano affidabilità reale.", "Prefixes do not always indicate real trustworthiness."))
    ForeignCallMode.SCHEDULED -> HelpContent(uiText("Esteri programmata", "Scheduled foreign rules"), uiText("Prevede fasce orarie in cui applicare le regole estere preparate in Regole.", "Uses time windows to apply foreign rules prepared in Rules."), uiText("Comoda per lavoro, reperibilità, viaggi o familiari all'estero.", "Useful for work, on-call time, travel, or family abroad."), uiText("Una fascia impostata male può bloccare chiamate attese.", "A badly configured time window may block expected calls."))
}

private fun foreignDescription(value: ForeignCallMode): String = when (value) {
    ForeignCallMode.WARN_ONLY -> uiText("Segnala la chiamata estera senza bloccarla.", "Flags the foreign call without blocking it.")
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN -> uiText("Blocca gli esteri non riconosciuti, lasciando margine ai contatti noti.", "Blocks unrecognized foreign calls while leaving room for known contacts.")
    ForeignCallMode.BLOCK_ALL_FOREIGN -> uiText("Rifiuta tutte le chiamate con prefisso internazionale.", "Rejects all calls with an international prefix.")
    ForeignCallMode.BLOCK_BY_COUNTRY -> uiText("Usa le nazioni configurate nella schermata Regole.", "Uses countries configured in the Rules screen.")
    ForeignCallMode.SCHEDULED -> uiText("Programma quando applicare le regole estere: utile per orari di lavoro, viaggi o reperibilità.", "Schedules when to apply foreign rules: useful for work hours, travel, or on-call periods.")
}

private fun themeModeHelp(value: ThemeMode): HelpContent = when (value) {
    ThemeMode.SYSTEM -> HelpContent(uiText("Tema sistema", "System theme"), uiText("Segue automaticamente il tema del dispositivo.", "Automatically follows the device theme."), uiText("Coerente con il resto del dispositivo.", "Consistent with the rest of the device."), uiText("Cambia aspetto quando cambia l'impostazione di sistema.", "Changes appearance when the system setting changes."))
    ThemeMode.LIGHT -> HelpContent(uiText("Tema chiaro", "Light theme"), uiText("Mantiene l'interfaccia chiara.", "Keeps the interface light."), uiText("Buona leggibilità in ambienti luminosi.", "Good readability in bright environments."), uiText("Può risultare più intensa al buio.", "Can feel harsher in the dark."))
    ThemeMode.DARK -> HelpContent(uiText("Tema scuro", "Dark theme"), uiText("Mantiene l'interfaccia scura.", "Keeps the interface dark."), uiText("Riposa gli occhi in ambienti poco illuminati.", "Easier on the eyes in low light."), uiText("All'aperto può essere meno leggibile.", "May be less readable outdoors."))
}

private fun paletteHelp(value: ThemePalette): HelpContent = when (value) {
    ThemePalette.SECURITY_BLUE -> HelpContent(uiText("Palette blu sicurezza", "Security blue palette"), uiText("Usa accenti blu per l'interfaccia.", "Uses blue accents for the interface."), uiText("Aspetto sobrio e riconoscibile.", "Sober and recognizable look."), uiText("Meno distintiva per stati positivi/negativi.", "Less distinctive for positive/negative states."))
    ThemePalette.PROTECTION_GREEN -> HelpContent(uiText("Palette verde protezione", "Protection green palette"), uiText("Usa accenti verdi per dare priorità agli stati sicuri.", "Uses green accents to prioritize safe states."), uiText("Trasmette protezione e conferme rapide.", "Conveys protection and quick confirmations."), uiText("Può essere meno adatta a chi distingue male il verde.", "May be less suitable for users who have trouble distinguishing green."))
    ThemePalette.PROFESSIONAL_GRAY -> HelpContent(uiText("Palette grigio professionale", "Professional gray palette"), uiText("Riduce la saturazione dei colori.", "Reduces color saturation."), uiText("Interfaccia discreta e meno distraente.", "Discreet and less distracting interface."), uiText("Gli stati possono risaltare meno.", "States may stand out less."))
    ThemePalette.TECH_PURPLE -> HelpContent(uiText("Palette viola tecnico", "Tech purple palette"), uiText("Usa accenti viola per un aspetto più tecnico.", "Uses purple accents for a more technical look."), uiText("Aiuta a distinguere visivamente l'app.", "Helps visually distinguish the app."), uiText("Può sembrare meno istituzionale.", "May feel less institutional."))
}

private fun highContrastHelp() = HelpContent(uiText("Alto contrasto", "High contrast"), uiText("Aumenta il contrasto visivo dell'interfaccia.", "Increases the visual contrast of the interface."), uiText("Migliora la leggibilità per molti utenti.", "Improves readability for many users."), uiText("L'aspetto può risultare più netto e meno morbido.", "The appearance may feel sharper and less soft."))

private fun contextualHelpHelp() = HelpContent(uiText("Aiuti contestuali", "Contextual help"), uiText("Mostra o nasconde i punti interrogativi accanto alle funzioni.", "Shows or hides question marks next to features."), uiText("Tiene le spiegazioni sempre a portata di mano.", "Keeps explanations close at hand."), uiText("Aggiunge più elementi visivi nelle schermate.", "Adds more visual elements to screens."))

private fun callTestHelp() = HelpContent(
    title = uiText("Simula numero", "Simulate number"),
    explanation = uiText("Valuta il numero scritto usando le regole attuali.", "Evaluates the typed number using current rules."),
    benefits = uiText("Aiuta a capire prima cosa succedera a una chiamata simile.", "Helps you understand in advance what would happen to a similar call."),
    drawbacks = uiText("La prova viene salvata nel registro locale come evento tecnico.", "The test is saved in the local log as a technical event."),
    advice = uiText("Usalo dopo modifiche a numeri, pattern, gruppi o nazioni.", "Use it after changing numbers, patterns, groups, or countries."),
    androidLimits = uiText("Una prova non sostituisce una chiamata reale filtrata da Android.", "A test does not replace a real call filtered by Android."),
)

private fun runtimePermissionsHelp() = HelpContent(uiText("Permesso rubrica", "Contacts permission"), uiText("CallGuardian usa la rubrica per riconoscere i contatti salvati.", "CallGuardian uses contacts to recognize saved contacts."), uiText("Permette valutazioni locali piu accurate e regole basate sui contatti.", "Enables more accurate local evaluation and contact-based rules."), uiText("Se lo rifiuti, la protezione automatica resta limitata per le regole che dipendono dalla rubrica.", "If you deny it, automatic protection remains limited for rules that depend on contacts."), androidLimits = uiText("Android puo chiedere conferme diverse in base alla versione.", "Android may ask for different confirmations depending on the version."))

private fun callScreeningRoleHelp() = HelpContent(uiText("Ruolo ID chiamante e spam", "Caller ID and spam role"), uiText("Imposta CallGuardian come servizio di filtro chiamate.", "Sets CallGuardian as the call filtering service."), uiText("È necessario per bloccare davvero le chiamate in arrivo.", "Required to actually block incoming calls."), uiText("Richiede una schermata di sistema e può essere concesso a una sola app alla volta.", "Requires a system screen and can be granted to only one app at a time."), androidLimits = uiText("Il ruolo è disponibile da Android 10 in poi; su altri dispositivi si apre la gestione app predefinite.", "The role is available from Android 10 onward; on other devices default-app management opens."))

private fun overlayHelp() = HelpContent(uiText("Popup sovrapposto", "Overlay popup"), uiText("Apre le impostazioni per consentire avvisi sopra altre app.", "Opens settings to allow warnings above other apps."), uiText("Permette avvisi visibili durante le chiamate sospette.", "Allows visible warnings during suspicious calls."), uiText("Un popup sovrapposto è più invasivo e Android lo tratta come permesso sensibile.", "An overlay popup is more intrusive and Android treats it as a sensitive permission."), androidLimits = uiText("Alcuni produttori limitano i popup sovrapposti con impostazioni aggiuntive.", "Some manufacturers limit overlay popups with extra settings."))

private fun PermissionSummary.isSetupComplete(): Boolean =
    runtimePermissionsGranted &&
        callScreeningRoleHeld &&
        notificationPermissionGranted &&
        overlayAllowed

private fun isSettingsSimulatorInput(value: String): Boolean {
    val trimmed = value.trim()
    val digits = trimmed.count { it.isDigit() }
    return trimmed.equals("anonimo", ignoreCase = true) ||
        (digits >= 5 && trimmed.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' })
}

private const val PayPalDonationUrl = "https://www.paypal.com/donate/?business=robycinix77%40gmail.com&no_recurring=0&item_name=Supporta+CallGuardian&currency_code=EUR"
