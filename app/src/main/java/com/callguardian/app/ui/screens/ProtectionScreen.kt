package com.callguardian.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.callguardian.app.LocalPermissionActions
import com.callguardian.app.core.model.CallAction
import com.callguardian.app.core.model.PermissionSummary
import com.callguardian.app.ui.components.ContextualHelpButton
import com.callguardian.app.ui.components.SectionCard
import com.callguardian.app.viewmodel.ProtectionViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProtectionScreen(viewModel: ProtectionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionActions = LocalPermissionActions.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val setupComplete = state.permissions?.isSetupComplete() == true
    val listState = rememberLazyListState()
    val heroScroll = listState.firstVisibleItemScrollOffset.coerceAtMost(420) / 420f

    LaunchedEffect(Unit) { viewModel.refreshPermissions() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = 1f - heroScroll * 0.18f
                    translationY = -heroScroll * 18f
                },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeroBrandLockup(
                    subtitle = protectionHeadline(state.permissions),
                    readiness = state.readinessScore,
                    roleHeld = state.permissions?.callScreeningRoleHeld == true,
                )
            }
        }
        item {
            CommandCenterPanel(
                readiness = state.readinessScore,
                blockedToday = state.blockedToday,
                roleHeld = state.permissions?.callScreeningRoleHeld == true,
                permissions = state.permissions,
                protectionLevel = state.settings.protectionLevel.displayName(),
            )
        }
        item {
            AnimatedVisibility(visible = !setupComplete) {
                SetupGuidePanel(
                    readiness = state.readinessScore,
                    permissions = state.permissions,
                    onRecommendedSetup = viewModel::applyRecommendedSetup,
                    permissionActions = permissionActions,
                )
            }
        }
        item {
            SectionCard(
                title = "Stato protezione",
                trailing = {
                    ContextualHelpButton(
                        title = "Livello protezione",
                        explanation = "Regola la severità del punteggio rischio prima del filtro chiamate.",
                        benefits = "Permette di adattare l'app al tuo profilo di rischio.",
                        drawbacks = "Livelli aggressivi possono aumentare gli avvisi.",
                        advice = "Bilanciata è adatta all'uso quotidiano.",
                        androidLimits = "Il blocco effettivo richiede il ruolo ID chiamante e spam.",
                    )
                }
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(state.settings.protectionLevel.displayName())
                        Text("Chiamate bloccate oggi: ${state.blockedToday}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun HeroBrandLockup(
    subtitle: String,
    readiness: Int,
    roleHeld: Boolean,
) {
    val motion = rememberInfiniteTransition(label = "brandRadarMotion")
    val shimmer by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2200), repeatMode = RepeatMode.Reverse),
        label = "brandRadarShimmer",
    )
    val radarGreen = Color(0xFF39F2A6)
    val signalAmber = Color(0xFFFFC857)
    val inkGreen = Color(0xFF083B33)
    val statusAccent = if (roleHeld) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
    val statusContainer = if (roleHeld) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val adaptiveTextColor = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.small,
            color = inkGreen.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, radarGreen.copy(alpha = 0.58f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(radarGreen.copy(alpha = 0.16f), radius = size.minDimension * 0.42f, center = center)
                    drawCircle(
                        radarGreen.copy(alpha = 0.46f),
                        radius = size.minDimension * 0.34f,
                        center = center,
                        style = Stroke(width = 1.4f),
                    )
                    drawLine(
                        color = signalAmber.copy(alpha = 0.75f),
                        start = center,
                        end = Offset(center.x + size.minDimension * 0.22f, center.y - size.minDimension * 0.16f),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round,
                    )
                }
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = radarGreen,
                    modifier = Modifier
                        .size(30.dp)
                        .graphicsLayer {
                            scaleX = 0.96f + shimmer * 0.08f
                            scaleY = 0.96f + shimmer * 0.08f
                        },
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "CallGuardian",
                style = MaterialTheme.typography.headlineMedium,
                color = adaptiveTextColor,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            shape = MaterialTheme.shapes.small,
            color = statusContainer,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                statusAccent.copy(alpha = 0.46f),
            ),
        ) {
            Text(
                text = if (roleHeld) "Protetto" else "$readiness%",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun CommandCenterPanel(
    readiness: Int,
    blockedToday: Int,
    roleHeld: Boolean,
    permissions: PermissionSummary?,
    protectionLevel: String,
) {
    val animatedReadiness by animateFloatAsState(
        targetValue = readiness / 100f,
        animationSpec = tween(durationMillis = 700),
        label = "readiness",
    )
    val colorScheme = MaterialTheme.colorScheme
    val panelAccent = if (roleHeld) colorScheme.secondary else colorScheme.tertiary
    val panelAccentContainer = if (roleHeld) {
        colorScheme.secondaryContainer
    } else {
        colorScheme.tertiaryContainer
    }
    val panelText = colorScheme.onSurface
    SectionCard(title = "Centro protezione") {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colorScheme.surfaceVariant,
                            colorScheme.primaryContainer,
                            panelAccentContainer,
                        )
                    ),
                    MaterialTheme.shapes.small,
                )
                .padding(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (roleHeld) Icons.Default.Security else Icons.Default.Warning,
                    contentDescription = null,
                    tint = panelAccent,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (roleHeld) "Scudo chiamate agganciato ad Android" else "Ultimo aggancio da completare",
                        color = panelText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        protectionLevel,
                        color = panelText.copy(alpha = 0.76f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Text(
            readinessTitle(readiness, roleHeld),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(readinessMessage(permissions), style = MaterialTheme.typography.bodyMedium)
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricTile("Stato", if (roleHeld) "Operativa" else "Da completare", Modifier.weight(1f))
            MetricTile("Bloccate", blockedToday.toString(), Modifier.weight(1f))
            MetricTile("Livello", protectionLevel, Modifier.weight(1f))
        }
        ReadinessProgressIndicator(
            progress = animatedReadiness,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (roleHeld) Icons.Default.Security else Icons.Default.Warning,
                contentDescription = null,
                tint = if (roleHeld) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(
                if (roleHeld) {
                    "CallGuardian è operativo in attesa di chiamate sospette!"
                } else {
                    "Attiva il ruolo ID chiamante e spam: e il passaggio che abilita il blocco reale."
                }
            )
        }
    }
}

@Composable
private fun ReadinessProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val startColor = MaterialTheme.colorScheme.outlineVariant
    val endColor = readinessColor(clampedProgress)

    Canvas(modifier = modifier) {
        val cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(
            color = trackColor,
            size = size,
            cornerRadius = cornerRadius,
        )
        if (clampedProgress > 0f) {
            val progressSize = Size(width = size.width * clampedProgress, height = size.height)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(startColor, endColor),
                    startX = 0f,
                    endX = progressSize.width,
                ),
                size = progressSize,
                cornerRadius = cornerRadius,
            )
        }
    }
}

@Composable
private fun readinessColor(progress: Float): Color {
    return lerp(
        MaterialTheme.colorScheme.outlineVariant,
        MaterialTheme.colorScheme.primary,
        progress.coerceIn(0f, 1f),
    )
}

@Composable
private fun RowScope.MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupGuidePanel(
    readiness: Int,
    permissions: PermissionSummary?,
    onRecommendedSetup: () -> Unit,
    permissionActions: com.callguardian.app.PermissionActions,
) {
    val glow = 0.38f
    SectionCard(title = "Visita guidata") {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f + glow * 0.18f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.76f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.82f + glow * 0.18f),
                        )
                    ),
                    MaterialTheme.shapes.small,
                )
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        "Configuriamo CallGuardian con le scelte ideali",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "La guida resta visibile solo finche manca qualcosa. Quando la protezione e pronta, sparisce.",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { readiness / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        ElevatedButton(onClick = onRecommendedSetup, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Tune, contentDescription = null)
            Text("Applica configurazione consigliata")
        }
        SetupStep(
            title = "Permessi telefono e rubrica",
            detail = "Servono per riconoscere contatti, numero chiamante e registro.",
            complete = permissions?.runtimePermissionsGranted == true,
            icon = { Icon(Icons.Default.Phone, contentDescription = null) },
            actionLabel = "Concedi",
            onClick = permissionActions.requestRuntimePermissions,
        )
        SetupStep(
            title = "Ruolo ID chiamante e spam",
            detail = "E il permesso decisivo: consente ad Android di affidare le chiamate a CallGuardian.",
            complete = permissions?.callScreeningRoleHeld == true,
            icon = { Icon(Icons.Default.Security, contentDescription = null) },
            actionLabel = "Attiva ruolo",
            onClick = permissionActions.requestCallScreeningRole,
            emphasized = true,
        )
        SetupStep(
            title = "Notifiche",
            detail = "Mostrano il risultato dopo un blocco o un avviso.",
            complete = permissions?.notificationPermissionGranted == true,
            icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
            actionLabel = "Concedi",
            onClick = permissionActions.requestRuntimePermissions,
        )
        SetupStep(
            title = "Popup durante la chiamata",
            detail = "Opzionale, ma rende visibile l'avviso sopra le altre app.",
            complete = permissions?.overlayAllowed == true,
            icon = { Icon(Icons.Default.Visibility, contentDescription = null) },
            actionLabel = "Apri popup",
            onClick = permissionActions.openOverlaySettings,
        )
    }
}

private fun PermissionSummary.isSetupComplete(): Boolean =
    runtimePermissionsGranted &&
        callScreeningRoleHeld &&
        notificationPermissionGranted &&
        overlayAllowed

@Composable
private fun SetupStep(
    title: String,
    detail: String,
    complete: Boolean,
    icon: @Composable () -> Unit,
    actionLabel: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    val targetColor = if (complete) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val containerColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(450), label = "stepColor")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (complete) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Completato", tint = MaterialTheme.colorScheme.secondary)
            } else {
                Box(Modifier.alpha(if (emphasized) 1f else 0.82f)) { icon() }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
            if (!complete) {
                if (emphasized) {
                    ElevatedButton(onClick = onClick) { Text(actionLabel) }
                } else {
                    OutlinedButton(onClick = onClick) { Text(actionLabel) }
                }
            }
        }
    }
}

@Composable
fun EventRow(
    title: String,
    subtitle: String,
    timeMillis: Long,
    action: CallAction,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    actions: @Composable () -> Unit = {},
    detail: @Composable () -> Unit = {},
) {
    val formattedTime = remember(timeMillis) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timeMillis))
    }
    if (compact) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "$formattedTime - ${action.displayName()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (action == CallAction.BLOCKED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    actions()
                }
            }
        }
        return
    }

    SectionCard(title = title, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(subtitle)
                Text(formattedTime)
                Text("Azione: ${action.displayName()}", color = if (action == CallAction.BLOCKED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            actions()
        }
        detail()
    }
}

private fun protectionHeadline(permissions: PermissionSummary?): String {
    return if (permissions?.callScreeningRoleHeld == true) {
        "Protezione locale attiva sulle chiamate in arrivo"
    } else {
        "Completa la configurazione per attivare il blocco chiamate"
    }
}

private fun readinessTitle(readiness: Int, roleHeld: Boolean): String {
    return when {
        roleHeld && readiness >= 90 -> "Protezione pronta"
        roleHeld -> "Protezione quasi pronta"
        else -> "Serve ancora un passaggio"
    }
}

private fun readinessMessage(permissions: PermissionSummary?): String {
    return when {
        permissions?.callScreeningRoleHeld != true -> "Il ruolo ID chiamante e spam non e ancora attivo: senza questo Android non puo far filtrare davvero le chiamate."
        permissions.runtimePermissionsGranted != true -> "Il ruolo e attivo, ma mancano alcuni permessi per valutare i numeri con piu precisione."
        permissions.notificationPermissionGranted != true -> "Il blocco e attivo. Abilita le notifiche per vedere sempre cosa e stato deciso."
        else -> "Tutto il necessario e configurato. Puoi chiudere l'app: Android la richiamera quando arriva una chiamata."
    }
}
