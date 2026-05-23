package com.callguardian.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class HelpContent(
    val title: String,
    val explanation: String,
    val benefits: String,
    val drawbacks: String,
    val advice: String = "",
    val androidLimits: String = "",
    val examples: List<String> = emptyList(),
)

val LocalContextualHelpEnabled = compositionLocalOf { true }

@Composable
fun ContextualHelpButton(
    title: String,
    explanation: String,
    benefits: String,
    drawbacks: String,
    advice: String,
    androidLimits: String,
    examples: List<String> = emptyList(),
) {
    ContextualHelpButton(
        help = HelpContent(
            title = title,
            explanation = explanation,
            benefits = benefits,
            drawbacks = drawbacks,
            advice = advice,
            androidLimits = androidLimits,
            examples = examples,
        )
    )
}

@Composable
fun ContextualHelpButton(help: HelpContent) {
    if (!LocalContextualHelpEnabled.current) return

    var open by remember { mutableStateOf(false) }
    IconButton(
        onClick = { open = true },
        modifier = Modifier.semantics {
            role = Role.Button
            contentDescription = "Aiuto: ${help.title}"
        },
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
            shadowElevation = 2.dp,
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                        ),
                    ),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(26.dp))
                    }
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = help.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    IconButton(onClick = { open = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Chiudi")
                    }
                }
            },
            text = {
                val examples = help.examples.ifEmpty { help.defaultExamples() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    HelpHero(help.explanation)
                    Spacer(Modifier.height(12.dp))
                    HelpSection(
                        icon = Icons.Filled.CheckCircle,
                        title = "Perche usarlo",
                        body = help.benefits,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    HelpSection(
                        icon = Icons.Filled.Warning,
                        title = "Da considerare",
                        body = help.drawbacks,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    if (help.advice.isNotBlank()) {
                        HelpSection(
                            icon = Icons.Filled.Lightbulb,
                            title = "Consiglio pratico",
                            body = help.advice,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (examples.isNotEmpty()) {
                        HelpExamples(examples)
                    }
                    if (help.androidLimits.isNotBlank()) {
                        HelpSection(
                            icon = Icons.Filled.Info,
                            title = "Limiti Android",
                            body = help.androidLimits,
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.70f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { open = false }) { Text("Ho capito") }
            }
        )
    }
}

@Composable
private fun HelpHero(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                        ),
                    ),
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HelpSection(
    icon: ImageVector,
    title: String,
    body: String,
    containerColor: Color,
    contentColor: Color,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.82f), contentColor = contentColor),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.10f),
                contentColor = contentColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.padding(start = 10.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = body,
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun HelpExamples(examples: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "Esempi d'uso",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            examples.forEach { example ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(">", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = example,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun HelpContent.defaultExamples(): List<String> {
    val normalizedTitle = title.lowercase()
    return when {
        "simula" in normalizedTitle -> listOf(
            "Scrivi un numero sospetto prima di salvarlo e verifica se verrebbe bloccato, avvisato o consentito.",
            "Prova lo stesso numero dopo aver cambiato una regola per capire subito l'effetto della modifica.",
        )
        "blocca numero" in normalizedTitle -> listOf(
            "Usalo per un numero preciso che ti ha gia chiamato piu volte.",
            "Inserisci il numero completo quando vuoi evitare effetti su contatti simili.",
        )
        "consenti numero" in normalizedTitle -> listOf(
            "Aggiungi banca, medico o familiari per evitare blocchi accidentali.",
            "Usalo quando un numero e importante anche se rientra in un pattern normalmente sospetto.",
        )
        "prefisso" in normalizedTitle || "pattern" in normalizedTitle -> listOf(
            "Blocca una famiglia di numeri con lo stesso inizio, ad esempio un centralino insistente.",
            "Preferisci pattern abbastanza lunghi per non coinvolgere chiamate legittime.",
        )
        "anonimi" in normalizedTitle -> listOf(
            "Scegli blocco se non vuoi ricevere chiamate private.",
            "Scegli ripetuti se vuoi lasciare una possibilita a chi deve davvero raggiungerti.",
        )
        "ester" in normalizedTitle || "nazione" in normalizedTitle || "paese" in normalizedTitle -> listOf(
            "Blocca paesi da cui non aspetti contatti commerciali o personali.",
            "Escludi una nazione se lavori con l'estero o hai familiari fuori Italia.",
        )
        "popup" in normalizedTitle -> listOf(
            "Attivalo se vuoi vedere un avviso anche mentre stai usando un'altra app.",
            "Disattivalo se preferisci notifiche meno invasive durante una chiamata.",
        )
        "permessi" in normalizedTitle || "ruolo" in normalizedTitle -> listOf(
            "Completa questo passaggio durante la prima configurazione.",
            "Ricontrollalo dopo aggiornamenti Android o cambio dispositivo.",
        )
        "tema" in normalizedTitle || "palette" in normalizedTitle || "contrasto" in normalizedTitle -> listOf(
            "Scegli l'opzione piu leggibile nell'ambiente in cui usi di piu il dispositivo.",
            "Prova alto contrasto se testi e pulsanti non risaltano abbastanza.",
        )
        else -> listOf(
            "Apri questo aiuto quando non sei sicuro dell'effetto di una scelta.",
            "Confronta vantaggi e attenzioni prima di applicare una configurazione permanente.",
        )
    }
}
