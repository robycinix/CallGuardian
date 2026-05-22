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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callguardian.app.LocalPermissionActions
import com.callguardian.app.core.model.AnonymousMode
import com.callguardian.app.core.model.ForeignCallMode
import com.callguardian.app.core.model.PermissionSummary
import com.callguardian.app.core.model.ProtectionLevel
import com.callguardian.app.core.model.ThemeMode
import com.callguardian.app.core.model.ThemePalette
import com.callguardian.app.ui.components.ContextualHelpButton
import com.callguardian.app.ui.components.HelpContent
import com.callguardian.app.ui.components.SectionCard
import com.callguardian.app.ui.components.ScreenHeader
import com.callguardian.app.viewmodel.ProtectionViewModel
import com.callguardian.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    protectionViewModel: ProtectionViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val protectionState by protectionViewModel.uiState.collectAsStateWithLifecycle()
    val permissionActions = LocalPermissionActions.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
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
                    } ?: error("Impossibile aprire il file")
                }
            }.onSuccess {
                Toast.makeText(context, "Backup esportato", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Export non riuscito: ${it.message}", Toast.LENGTH_LONG).show()
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
                        ?: error("Impossibile leggere il file")
                }
                viewModel.importBackupJson(json)
            }.onSuccess {
                Toast.makeText(context, "Backup ripristinato", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Import non riuscito: ${it.message}", Toast.LENGTH_LONG).show()
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
            title = "Opzioni",
            subtitle = "Comportamento, tema, permessi e backup governati da un solo pannello.",
        )
        SectionCard(title = "Modalità protezione") {
            Text(
                "Bilanciata è la scelta consigliata: protegge senza diventare troppo severa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EnumOptionList(
                values = ProtectionLevel.entries,
                selected = settings.protectionLevel,
                labelFor = ProtectionLevel::displayName,
                descriptionFor = ::protectionDescription,
                onSelected = viewModel::setProtectionLevel,
                helpFor = ::protectionHelp,
            )
        }
        SectionCard(
            title = "Numeri anonimi",
            trailing = {
                ContextualHelpButton(
                    title = "Gestione anonimi",
                    explanation = "Definisce come trattare chiamate senza numero visibile.",
                    benefits = "Riduce chiamate moleste non identificabili.",
                    drawbacks = "Alcune chiamate legittime usano numero privato.",
                    advice = "Inizia con Avviso, poi passa a Blocca se necessario.",
                    androidLimits = "Il servizio riceve solo le informazioni esposte dall'operatore telefonico.",
                )
            }
        ) {
            Text(
                "Decidi cosa fare quando il numero non è visibile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EnumOptionList(
                values = AnonymousMode.entries,
                selected = settings.anonymousMode,
                labelFor = AnonymousMode::displayName,
                descriptionFor = ::anonymousDescription,
                onSelected = viewModel::setAnonymousMode,
                helpFor = ::anonymousHelp,
            )
            if (settings.anonymousMode == AnonymousMode.ALLOW_AFTER_REPEATED_ATTEMPTS) {
                Text(
                    "Dopo quanti tentativi deve passare una chiamata anonima?",
                    style = MaterialTheme.typography.titleSmall,
                )
                AttemptPicker(
                    selected = settings.allowRepeatedAnonymousAfterAttempts,
                    onSelected = viewModel::setRepeatedAnonymousAttempts,
                )
            }
        }
        SectionCard(title = "Chiamate estere") {
            Text(
                "Utile contro frodi internazionali, ma scegli un livello compatibile con lavoro, viaggi e familiari.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EnumOptionList(
                values = ForeignCallMode.entries,
                selected = settings.foreignCallMode,
                labelFor = ForeignCallMode::displayName,
                descriptionFor = ::foreignDescription,
                onSelected = viewModel::setForeignMode,
                helpFor = ::foreignHelp,
            )
        }
        SectionCard(title = "Tema") {
            Text(
                "Personalizza leggibilità e colori senza cambiare le regole di protezione.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Aspetto", style = MaterialTheme.typography.titleSmall)
            CompactEnumRow(ThemeMode.entries, settings.themeMode, ThemeMode::displayName, viewModel::setThemeMode)
            Text("Colori", style = MaterialTheme.typography.titleSmall)
            CompactEnumRow(ThemePalette.entries, settings.palette, ThemePalette::displayName, viewModel::setPalette)
            SettingSwitch("Alto contrasto", settings.highContrast, viewModel::setHighContrast, highContrastHelp())
        }
        SectionCard(title = "Aiuti contestuali") {
            SettingSwitch("Mostra pulsanti informativi", settings.contextualHelpEnabled, viewModel::setContextualHelp, contextualHelpHelp())
        }
        SectionCard(title = "Permessi Android") {
            AndroidPermissionsPanel(
                permissions = protectionState.permissions,
                onRuntimePermissions = permissionActions.requestRuntimePermissions,
                onCallScreeningRole = permissionActions.requestCallScreeningRole,
                onOverlaySettings = permissionActions.openOverlaySettings,
            )
        }
        SectionCard(title = "Backup locale") {
            Text("Esporta o ripristina impostazioni, regole, paesi e ultimi eventi in un file JSON locale.")
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
                                    Toast.makeText(context, "Export non riuscito: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                ) { Text("Esporta JSON") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                ) { Text("Importa JSON") }
            }
        }
        SectionCard(title = "Sostieni CallGuardian") {
            DonationButton()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AndroidPermissionsPanel(
    permissions: PermissionSummary?,
    onRuntimePermissions: () -> Unit,
    onCallScreeningRole: () -> Unit,
    onOverlaySettings: () -> Unit,
) {
    PermissionLine("Permessi telefono/rubrica", permissions?.runtimePermissionsGranted == true)
    PermissionLine("Ruolo ID chiamante e spam", permissions?.callScreeningRoleHeld == true)
    PermissionLine("Notifiche", permissions?.notificationPermissionGranted == true)
    PermissionLine("Popup sovrapposto", permissions?.overlayAllowed == true)

    if (permissions?.isSetupComplete() == true) {
        Text("Tutto configurato. Nessuna azione richiesta.", style = MaterialTheme.typography.bodyMedium)
        return
    }

    val runtimeOrNotificationMissing =
        permissions == null ||
            !permissions.runtimePermissionsGranted ||
            !permissions.notificationPermissionGranted
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (runtimeOrNotificationMissing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onRuntimePermissions) { Text("Concedi permessi") }
                ContextualHelpButton(runtimePermissionsHelp())
            }
        }
        if (permissions?.callScreeningRoleHeld != true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onCallScreeningRole) { Text("Attiva filtro chiamate") }
                ContextualHelpButton(callScreeningRoleHelp())
            }
        }
        if (permissions?.overlayAllowed != true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onOverlaySettings) { Text("Mostra avviso in chiamata") }
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
            contentDescription = if (ok) "Concesso" else "Richiesto",
            tint = if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun DonationButton() {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Se l'app ti è utile, puoi contribuire allo sviluppo con una donazione libera.")
        Button(
            onClick = { uriHandler.openUri(PayPalDonationUrl) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Dona con PayPal")
        }
    }
}

@Composable
private fun <T : Enum<T>> EnumOptionList(
    values: List<T>,
    selected: T,
    labelFor: (T) -> String,
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
                        Icon(Icons.Default.Check, contentDescription = "Selezionata")
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
    labelFor: (T) -> String,
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
                        Icon(Icons.Default.Check, contentDescription = "Selezionata")
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
                        Icon(Icons.Default.Check, contentDescription = "Selezionata")
                    }
                    Text(
                        text = "$attempts tentativi",
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
    ProtectionLevel.OFF -> HelpContent("Disattivata", "Spegne la valutazione automatica delle chiamate.", "Evita falsi positivi e lascia passare tutto.", "Non protegge da spam, frodi o numeri sospetti.")
    ProtectionLevel.LIGHT -> HelpContent("Protezione leggera", "Applica regole prudenti solo sui casi più evidenti.", "Riduce il rischio di bloccare chiamate utili.", "Lascia passare più chiamate dubbie.")
    ProtectionLevel.BALANCED -> HelpContent("Protezione bilanciata", "Usa una soglia intermedia per avvisi e blocchi.", "Buon equilibrio per l'uso quotidiano.", "Qualche chiamata legittima potrebbe ricevere un avviso.")
    ProtectionLevel.AGGRESSIVE -> HelpContent("Protezione aggressiva", "Abbassa la tolleranza verso numeri sospetti.", "Ferma più spam e chiamate insistenti.", "Aumenta il rischio di blocchi o avvisi eccessivi.")
    ProtectionLevel.CUSTOM -> HelpContent("Protezione personalizzata", "Riserva spazio a regole e soglie scelte dall'utente.", "Massimo controllo quando hai esigenze specifiche.", "Richiede più attenzione nella configurazione.")
}

private fun protectionDescription(value: ProtectionLevel): String = when (value) {
    ProtectionLevel.OFF -> "Lascia passare tutto. Da usare solo per prove o pause temporanee."
    ProtectionLevel.LIGHT -> "Interviene solo sui casi più evidenti, con pochi falsi allarmi."
    ProtectionLevel.BALANCED -> "Consigliata per tutti i giorni: avvisa e blocca con prudenza."
    ProtectionLevel.AGGRESSIVE -> "Più severa contro spam e numeri sospetti."
    ProtectionLevel.CUSTOM -> "Per chi vuole regole specifiche e maggiore controllo."
}

private fun anonymousHelp(value: AnonymousMode): HelpContent = when (value) {
    AnonymousMode.WARN -> HelpContent("Anonimi: avvisa", "Mostra un avviso per chiamate senza numero visibile.", "Ti lascia decidere caso per caso.", "La chiamata può comunque disturbare.")
    AnonymousMode.SILENCE -> HelpContent("Anonimi: silenzia", "Riduce l'impatto delle chiamate anonime senza bloccarle.", "Meno interruzioni durante la giornata.", "Potresti non accorgerti subito di una chiamata legittima.")
    AnonymousMode.BLOCK -> HelpContent("Anonimi: blocca", "Respinge le chiamate senza identificativo.", "Molto efficace contro chiamate moleste anonime.", "Puoi perdere chiamate importanti da numeri privati.")
    AnonymousMode.ALLOW_AFTER_REPEATED_ATTEMPTS -> HelpContent("Anonimi: ripetuti", "Consente anonimi dopo tentativi ripetuti.", "Aiuta chi deve raggiungerti davvero.", "Un chiamante molesto insistente potrebbe passare.")
}

private fun anonymousDescription(value: AnonymousMode): String = when (value) {
    AnonymousMode.WARN -> "Mostra un avviso, ma ti lascia rispondere."
    AnonymousMode.SILENCE -> "Riduce il disturbo senza rifiutare la chiamata."
    AnonymousMode.BLOCK -> "Rifiuta le chiamate senza numero visibile."
    AnonymousMode.ALLOW_AFTER_REPEATED_ATTEMPTS -> "Fa passare chi richiama più volte."
}

private fun foreignHelp(value: ForeignCallMode): HelpContent = when (value) {
    ForeignCallMode.WARN_ONLY -> HelpContent("Esteri: solo avviso", "Segnala le chiamate estere senza bloccarle.", "Utile se ricevi chiamate internazionali legittime.", "Non ferma automaticamente frodi dall'estero.")
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN -> HelpContent("Esteri sconosciuti", "Blocca numeri esteri non riconosciuti.", "Riduce spam internazionale mantenendo margine per contatti noti.", "Un numero nuovo ma legittimo può essere bloccato.")
    ForeignCallMode.BLOCK_ALL_FOREIGN -> HelpContent("Blocca tutti gli esteri", "Respinge ogni chiamata con prefisso internazionale.", "Massima protezione se non aspetti chiamate dall'estero.", "Troppo rigida per lavoro, viaggi o familiari fuori Italia.")
    ForeignCallMode.BLOCK_BY_COUNTRY -> HelpContent("Blocca per nazione", "Usa le regole paese impostate nella schermata Regole.", "Controllo preciso sui prefissi internazionali.", "I prefissi non sempre indicano affidabilità reale.")
    ForeignCallMode.SCHEDULED -> HelpContent("Esteri programmata", "Prevede fasce orarie in cui applicare le regole estere preparate in Regole.", "Comoda per lavoro, reperibilità, viaggi o familiari all'estero.", "Una fascia impostata male può bloccare chiamate attese.")
}

private fun foreignDescription(value: ForeignCallMode): String = when (value) {
    ForeignCallMode.WARN_ONLY -> "Segnala la chiamata estera senza bloccarla."
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN -> "Blocca gli esteri non riconosciuti, lasciando margine ai contatti noti."
    ForeignCallMode.BLOCK_ALL_FOREIGN -> "Rifiuta tutte le chiamate con prefisso internazionale."
    ForeignCallMode.BLOCK_BY_COUNTRY -> "Usa le nazioni configurate nella schermata Regole."
    ForeignCallMode.SCHEDULED -> "Programma quando applicare le regole estere: utile per orari di lavoro, viaggi o reperibilità."
}

private fun themeModeHelp(value: ThemeMode): HelpContent = when (value) {
    ThemeMode.SYSTEM -> HelpContent("Tema sistema", "Segue automaticamente il tema del telefono.", "Coerente con il resto del dispositivo.", "Cambia aspetto quando cambia l'impostazione di sistema.")
    ThemeMode.LIGHT -> HelpContent("Tema chiaro", "Mantiene l'interfaccia chiara.", "Buona leggibilità in ambienti luminosi.", "Può risultare più intensa al buio.")
    ThemeMode.DARK -> HelpContent("Tema scuro", "Mantiene l'interfaccia scura.", "Riposa gli occhi in ambienti poco illuminati.", "All'aperto può essere meno leggibile.")
}

private fun paletteHelp(value: ThemePalette): HelpContent = when (value) {
    ThemePalette.SECURITY_BLUE -> HelpContent("Palette blu sicurezza", "Usa accenti blu per l'interfaccia.", "Aspetto sobrio e riconoscibile.", "Meno distintiva per stati positivi/negativi.")
    ThemePalette.PROTECTION_GREEN -> HelpContent("Palette verde protezione", "Usa accenti verdi per dare priorità agli stati sicuri.", "Trasmette protezione e conferme rapide.", "Può essere meno adatta a chi distingue male il verde.")
    ThemePalette.PROFESSIONAL_GRAY -> HelpContent("Palette grigio professionale", "Riduce la saturazione dei colori.", "Interfaccia discreta e meno distraente.", "Gli stati possono risaltare meno.")
    ThemePalette.TECH_PURPLE -> HelpContent("Palette viola tecnico", "Usa accenti viola per un aspetto più tecnico.", "Aiuta a distinguere visivamente l'app.", "Può sembrare meno istituzionale.")
}

private fun highContrastHelp() = HelpContent("Alto contrasto", "Aumenta il contrasto visivo dell'interfaccia.", "Migliora la leggibilità per molti utenti.", "L'aspetto può risultare più netto e meno morbido.")

private fun contextualHelpHelp() = HelpContent("Aiuti contestuali", "Mostra o nasconde i punti interrogativi accanto alle funzioni.", "Tiene le spiegazioni sempre a portata di mano.", "Aggiunge più elementi visivi nelle schermate.")

private fun runtimePermissionsHelp() = HelpContent("Permessi telefono", "CallGuardian ha bisogno di questo permesso per riconoscere e bloccare i numeri di spam prima ancora che il telefono squilli.", "Permette valutazioni locali piu accurate e riconoscimento dei contatti.", "Se lo rifiuti, la protezione automatica resta limitata ma puoi usare le funzioni manuali.", androidLimits = "Android puo chiedere conferme diverse in base alla versione.")

private fun callScreeningRoleHelp() = HelpContent("Ruolo ID chiamante e spam", "Imposta CallGuardian come servizio di filtro chiamate.", "È necessario per bloccare davvero le chiamate in arrivo.", "Richiede una schermata di sistema e può essere concesso a una sola app alla volta.", androidLimits = "Il ruolo è disponibile da Android 10 in poi; su altri dispositivi si apre la gestione app predefinite.")

private fun overlayHelp() = HelpContent("Popup sovrapposto", "Apre le impostazioni per consentire avvisi sopra altre app.", "Permette avvisi visibili durante le chiamate sospette.", "Un popup sovrapposto è più invasivo e Android lo tratta come permesso sensibile.", androidLimits = "Alcuni produttori limitano i popup sovrapposti con impostazioni aggiuntive.")

private fun PermissionSummary.isSetupComplete(): Boolean =
    runtimePermissionsGranted &&
        callScreeningRoleHeld &&
        notificationPermissionGranted &&
        overlayAllowed

private const val PayPalDonationUrl = "https://www.paypal.com/donate/?business=robycinix77%40gmail.com&no_recurring=0&item_name=Supporta+CallGuardian&currency_code=EUR"
