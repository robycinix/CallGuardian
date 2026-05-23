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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
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
import com.callguardian.app.R
import com.callguardian.app.core.model.CallAction
import com.callguardian.app.core.model.PermissionSummary
import com.callguardian.app.ui.components.SectionCard
import com.callguardian.app.viewmodel.ProtectionViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProtectionScreen(
    viewModel: ProtectionViewModel = hiltViewModel(),
    onOpenLogs: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenRulesLists: () -> Unit = {},
    onOpenRulesGroups: () -> Unit = {},
    onOpenRulesForeign: () -> Unit = {},
    onOpenRulesSummary: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionActions = LocalPermissionActions.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val setupComplete = state.permissions?.isSetupComplete() == true
    val listState = rememberLazyListState()
    val heroScroll = listState.firstVisibleItemScrollOffset.coerceAtMost(420) / 420f
    val showBottomStatus = setupComplete && LocalConfiguration.current.screenHeightDp >= 720

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

    Box(Modifier.fillMaxSize()) {
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
                    protectionLevel = state.settings.protectionLevel.localizedDisplayName(),
                    onBlockedClick = onOpenLogs,
                    onLevelClick = onOpenSettings,
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
                RulesQuickActionsPanel(
                    onOpenLists = onOpenRulesLists,
                    onOpenGroups = onOpenRulesGroups,
                    onOpenForeign = onOpenRulesForeign,
                    onOpenSummary = onOpenRulesSummary,
                )
            }
        }
        if (showBottomStatus) {
            HomeBottomStatusStrip(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun HomeBottomStatusStrip(modifier: Modifier = Modifier) {
    Surface(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = uiText("Protezione locale attiva", "Local protection active"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = uiText("Regole e registro restano sul dispositivo", "Rules and logs stay on device"),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = uiText("Locale", "Local"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
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
                text = stringResource(R.string.app_name),
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
                text = if (roleHeld) stringResource(R.string.brand_protected) else "$readiness%",
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
    onBlockedClick: () -> Unit,
    onLevelClick: () -> Unit,
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
    SectionCard(title = stringResource(R.string.command_center_title)) {
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
                        if (roleHeld) stringResource(R.string.command_center_android_connected) else stringResource(R.string.command_center_last_step),
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
            MetricTile(stringResource(R.string.metric_status), if (roleHeld) stringResource(R.string.metric_status_operational) else stringResource(R.string.metric_status_incomplete), Modifier.weight(1f))
            MetricTile(stringResource(R.string.metric_blocked), blockedToday.toString(), Modifier.weight(1f), onClick = onBlockedClick)
            MetricTile(stringResource(R.string.metric_level), protectionLevel, Modifier.weight(1f), onClick = onLevelClick)
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
                    stringResource(R.string.protection_operational_message)
                } else {
                    stringResource(R.string.protection_role_missing_message)
                }
            )
        }
    }
}

@Composable
private fun RulesQuickActionsPanel(
    onOpenLists: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenForeign: () -> Unit,
    onOpenSummary: () -> Unit,
) {
    SectionCard(title = uiText("Centro regole", "Rules center")) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickRuleTile(uiText("Liste", "Lists"), onOpenLists, Modifier.weight(1f))
            QuickRuleTile(uiText("Gruppi", "Groups"), onOpenGroups, Modifier.weight(1f))
            QuickRuleTile(uiText("Esteri", "Foreign"), onOpenForeign, Modifier.weight(1f))
            QuickRuleTile(uiText("Sintesi", "Summary"), onOpenSummary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RowScope.QuickRuleTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
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
private fun RowScope.MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val tileModifier = if (onClick != null) {
        modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxHeight()
    }
    Surface(
        modifier = tileModifier,
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
    SectionCard(title = stringResource(R.string.setup_tour_title)) {
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
                        stringResource(R.string.setup_tour_heading),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    stringResource(R.string.setup_tour_body),
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
            Text(stringResource(R.string.setup_recommended))
        }
        SetupStep(
            title = stringResource(R.string.setup_phone_title),
            detail = stringResource(R.string.setup_phone_detail),
            complete = permissions?.runtimePermissionsGranted == true,
            icon = { Icon(Icons.Default.Phone, contentDescription = null) },
            actionLabel = stringResource(R.string.action_grant),
            onClick = permissionActions.requestRuntimePermissions,
        )
        SetupStep(
            title = stringResource(R.string.setup_call_screening_title),
            detail = stringResource(R.string.setup_call_screening_detail),
            complete = permissions?.callScreeningRoleHeld == true,
            icon = { Icon(Icons.Default.Security, contentDescription = null) },
            actionLabel = stringResource(R.string.action_activate_role),
            onClick = permissionActions.requestCallScreeningRole,
            emphasized = true,
        )
        SetupStep(
            title = stringResource(R.string.setup_notifications_title),
            detail = stringResource(R.string.setup_notifications_detail),
            complete = permissions?.notificationPermissionGranted == true,
            icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
            actionLabel = stringResource(R.string.action_grant),
            onClick = permissionActions.requestRuntimePermissions,
        )
        SetupStep(
            title = stringResource(R.string.setup_overlay_title),
            detail = stringResource(R.string.setup_overlay_detail),
            complete = permissions?.overlayAllowed == true,
            icon = { Icon(Icons.Default.Visibility, contentDescription = null) },
            actionLabel = stringResource(R.string.action_open_popup),
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
                Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.content_completed), tint = MaterialTheme.colorScheme.secondary)
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
                        "$formattedTime - ${action.localizedDisplayName()}",
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
                Text(stringResource(R.string.event_action_format, action.localizedDisplayName()), color = if (action == CallAction.BLOCKED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            actions()
        }
        detail()
    }
}

@Composable
private fun protectionHeadline(permissions: PermissionSummary?): String {
    return if (permissions?.callScreeningRoleHeld == true) {
        stringResource(R.string.protection_headline_active)
    } else {
        stringResource(R.string.protection_headline_incomplete)
    }
}

@Composable
private fun readinessTitle(readiness: Int, roleHeld: Boolean): String {
    return when {
        roleHeld && readiness >= 90 -> stringResource(R.string.readiness_ready)
        roleHeld -> stringResource(R.string.readiness_almost_ready)
        else -> stringResource(R.string.readiness_needs_step)
    }
}

@Composable
private fun readinessMessage(permissions: PermissionSummary?): String {
    return when {
        permissions?.callScreeningRoleHeld != true -> stringResource(R.string.readiness_role_missing)
        permissions.runtimePermissionsGranted != true -> stringResource(R.string.readiness_runtime_missing)
        permissions.notificationPermissionGranted != true -> stringResource(R.string.readiness_notifications_missing)
        else -> stringResource(R.string.readiness_complete)
    }
}
