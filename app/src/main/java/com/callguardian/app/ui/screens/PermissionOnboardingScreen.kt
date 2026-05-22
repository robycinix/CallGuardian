package com.callguardian.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.callguardian.app.core.model.PermissionSummary

@Composable
fun PermissionOnboardingScreen(
    permissions: PermissionSummary,
    activeStep: String?,
    automaticSetupStarted: Boolean,
    onStartSetup: () -> Unit,
    onContinue: () -> Unit,
    onRuntimePermissions: () -> Unit,
    onCallScreeningRole: () -> Unit,
    onOverlaySettings: () -> Unit,
) {
    val completed = listOf(
        permissions.runtimePermissionsGranted,
        permissions.callScreeningRoleHeld,
        permissions.notificationPermissionGranted,
        permissions.overlayAllowed,
    ).count { it }
    val progress = completed / 4f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FirstRunHero(progress = progress)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Prima configurazione necessaria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "CallGuardian chiede subito tutto quello che serve, cosi la protezione parte prima delle chiamate indesiderate.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        PermissionVisualGuide()
        Text(
            activeStep?.let { "Ora Android sta aprendo: $it" }
                ?: "Segui le finestre di Android: quando vedi Consenti, Attiva o CallGuardian, tocca quella scelta.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PermissionSetupRow(
            title = "Telefono e rubrica",
            detail = "Tocca Consenti. Serve per capire chi chiama e non bloccare i contatti salvati.",
            example = "Esempio: se chiama Maria in rubrica, passa.",
            complete = permissions.runtimePermissionsGranted,
            icon = Icons.Default.Phone,
            onClick = onRuntimePermissions,
        )
        PermissionSetupRow(
            title = "ID chiamante e spam",
            detail = "Tocca Attiva o scegli CallGuardian nella schermata di sistema.",
            example = "Esempio: Android manda la chiamata a CallGuardian prima dello squillo.",
            complete = permissions.callScreeningRoleHeld,
            icon = Icons.Default.Security,
            emphasized = true,
            onClick = onCallScreeningRole,
        )
        PermissionSetupRow(
            title = "Notifiche",
            detail = "Tocca Consenti per vedere cosa e stato bloccato o segnalato.",
            example = "Esempio: ricevi un avviso dopo una chiamata sospetta fermata.",
            complete = permissions.notificationPermissionGranted,
            icon = Icons.Default.Notifications,
            onClick = onRuntimePermissions,
        )
        PermissionSetupRow(
            title = "Popup sopra le altre app",
            detail = "Nelle impostazioni apri CallGuardian e accendi Consenti visualizzazione sopra altre app.",
            example = "Esempio: durante una chiamata dubbia compare un avviso grande.",
            complete = permissions.overlayAllowed,
            icon = Icons.Default.Visibility,
            onClick = onOverlaySettings,
        )
        ElevatedButton(
            onClick = {
                if (!automaticSetupStarted && completed == 0) {
                    onStartSetup()
                } else {
                    onContinue()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.TouchApp, contentDescription = null)
            Text(
                if (!automaticSetupStarted && completed == 0) {
                    "Ho capito, avvia i permessi"
                } else {
                    "Continua con il prossimo permesso"
                }
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FirstRunHero(progress: Float) {
    val motion = rememberInfiniteTransition(label = "onboardingPulse")
    val pulse by motion.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "pulse",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                                    MaterialTheme.colorScheme.surfaceVariant,
                                )
                            ),
                            MaterialTheme.shapes.small,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    PermissionRadar()
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Attiviamo lo scudo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "Ci vogliono pochi tocchi. L'app ti porta automaticamente nelle schermate giuste.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Text(
                "${(progress * 100).toInt()}% pronto",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PermissionRadar() {
    Canvas(Modifier.fillMaxSize().padding(8.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val green = Color(0xFF39F2A6)
        val amber = Color(0xFFFFC857)
        drawCircle(green.copy(alpha = 0.15f), size.minDimension * 0.43f, center)
        drawCircle(green.copy(alpha = 0.55f), size.minDimension * 0.34f, center, style = Stroke(2.4f))
        drawCircle(green.copy(alpha = 0.35f), size.minDimension * 0.20f, center, style = Stroke(1.8f))
        drawLine(
            color = amber,
            start = center,
            end = Offset(center.x + size.minDimension * 0.30f, center.y - size.minDimension * 0.18f),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )
        drawCircle(amber, 5f, Offset(center.x + size.minDimension * 0.22f, center.y - size.minDimension * 0.12f))
    }
}

@Composable
private fun PermissionVisualGuide() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MiniPhoneExample(
            title = "1. Consenti",
            body = "Telefono Rubrica Notifiche",
            modifier = Modifier.weight(1f),
        )
        MiniPhoneExample(
            title = "2. Attiva",
            body = "ID chiamante e spam",
            modifier = Modifier.weight(1f),
        )
        MiniPhoneExample(
            title = "3. Accendi",
            body = "Popup sopra altre app",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MiniPhoneExample(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp),
            ) {
                val phoneWidth = size.width * 0.62f
                val left = (size.width - phoneWidth) / 2f
                drawRoundRect(
                    color = Color(0xFF16221F),
                    topLeft = Offset(left, 0f),
                    size = Size(phoneWidth, size.height),
                    cornerRadius = CornerRadius(18f, 18f),
                )
                drawRoundRect(
                    color = Color(0xFFEAF6F1),
                    topLeft = Offset(left + 6f, 10f),
                    size = Size(phoneWidth - 12f, size.height - 20f),
                    cornerRadius = CornerRadius(12f, 12f),
                )
                drawRoundRect(
                    color = Color(0xFF39F2A6),
                    topLeft = Offset(left + 16f, size.height - 34f),
                    size = Size(phoneWidth - 32f, 14f),
                    cornerRadius = CornerRadius(8f, 8f),
                )
                drawRoundRect(
                    color = Color(0xFFB9C7C1),
                    topLeft = Offset(left + 18f, 25f),
                    size = Size(phoneWidth - 36f, 8f),
                    cornerRadius = CornerRadius(5f, 5f),
                )
                drawRoundRect(
                    color = Color(0xFFB9C7C1),
                    topLeft = Offset(left + 18f, 40f),
                    size = Size(phoneWidth - 46f, 7f),
                    cornerRadius = CornerRadius(5f, 5f),
                )
            }
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PermissionSetupRow(
    title: String,
    detail: String,
    example: String,
    complete: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    val container = when {
        complete -> MaterialTheme.colorScheme.secondaryContainer
        emphasized -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = container,
        tonalElevation = if (emphasized && !complete) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (complete) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                tint = if (complete) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodyMedium)
                Text(example, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!complete) {
                if (emphasized) {
                    ElevatedButton(onClick = onClick) { Text("Apri") }
                } else {
                    OutlinedButton(onClick = onClick) { Text("Apri") }
                }
            }
        }
    }
}
