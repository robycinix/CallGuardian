package com.callguardian.app

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.getSystemService
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.callguardian.app.core.model.PermissionSummary
import com.callguardian.app.core.permissions.PermissionChecker
import com.callguardian.app.data.repository.GuardianRepository
import com.callguardian.app.ui.components.LocalContextualHelpEnabled
import com.callguardian.app.ui.components.GuardianAppChrome
import com.callguardian.app.ui.navigation.CallGuardianNavHost
import com.callguardian.app.ui.screens.PermissionOnboardingScreen
import com.callguardian.app.ui.screens.PreloadScreen
import com.callguardian.app.ui.theme.CallGuardianTheme
import com.callguardian.app.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

val LocalPermissionActions = staticCompositionLocalOf<PermissionActions> {
    error("PermissionActions not provided")
}

data class PermissionActions(
    val requestRuntimePermissions: () -> Unit,
    val requestCallScreeningRole: () -> Unit,
    val openOverlaySettings: () -> Unit,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var repository: GuardianRepository
    @Inject lateinit var permissionChecker: PermissionChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runBlocking { repository.initialize() }

        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            val setupPrefs = remember {
                getSharedPreferences("first_setup", Context.MODE_PRIVATE)
            }
            var permissionSummary by remember { mutableStateOf(permissionChecker.summary()) }
            var firstSetupFinished by remember {
                mutableStateOf(setupPrefs.getBoolean(KEY_FIRST_SETUP_FINISHED, false))
            }
            var automaticSetupStarted by rememberSaveable { mutableStateOf(false) }
            var automaticRuntimeRequested by rememberSaveable { mutableStateOf(false) }
            var automaticRoleRequested by rememberSaveable { mutableStateOf(false) }
            var automaticOverlayRequested by rememberSaveable { mutableStateOf(false) }
            var currentPermissionStep by remember { mutableStateOf<PermissionStep?>(null) }

            fun refreshPermissionSummary() {
                permissionSummary = permissionChecker.summary()
                if (permissionSummary.isSetupCompleteForFirstRun()) {
                    setupPrefs.edit().putBoolean(KEY_FIRST_SETUP_FINISHED, true).apply()
                    firstSetupFinished = true
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { grants ->
                    currentPermissionStep = null
                    refreshPermissionSummary()
                    if (grants.values.any { granted -> !granted }) {
                        Toast.makeText(
                            this,
                            "Protezione automatica limitata: puoi continuare a usare le funzioni manuali e concedere i permessi piu tardi.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            )
            val roleLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
                onResult = {
                    currentPermissionStep = null
                    refreshPermissionSummary()
                }
            )
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val appReady by settingsViewModel.appReady.collectAsStateWithLifecycle()
            var preloadMinimumElapsed by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(900)
                preloadMinimumElapsed = true
            }
            LaunchedEffect(Unit) { refreshPermissionSummary() }
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        currentPermissionStep = null
                        refreshPermissionSummary()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val actions = PermissionActions(
                requestRuntimePermissions = {
                    currentPermissionStep = PermissionStep.RUNTIME
                    val permissions = buildList {
                        add(Manifest.permission.READ_PHONE_STATE)
                        add(Manifest.permission.READ_CONTACTS)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                },
                requestCallScreeningRole = {
                    currentPermissionStep = PermissionStep.CALL_SCREENING
                    val roleManager = getSystemService<RoleManager>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
                    ) {
                        roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                    } else {
                        roleLauncher.launch(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    }
                },
                openOverlaySettings = {
                    currentPermissionStep = PermissionStep.OVERLAY
                    roleLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            )
            val runNextSetupStep: (Boolean) -> Boolean = { automatic ->
                refreshPermissionSummary()
                when {
                    !permissionSummary.runtimePermissionsGranted || !permissionSummary.notificationPermissionGranted -> {
                        if (automatic && automaticRuntimeRequested) {
                            false
                        } else {
                            if (automatic) automaticRuntimeRequested = true
                            actions.requestRuntimePermissions()
                            true
                        }
                    }
                    !permissionSummary.callScreeningRoleHeld -> {
                        if (automatic && automaticRoleRequested) {
                            false
                        } else {
                            if (automatic) automaticRoleRequested = true
                            actions.requestCallScreeningRole()
                            true
                        }
                    }
                    !permissionSummary.overlayAllowed -> {
                        if (automatic && automaticOverlayRequested) {
                            false
                        } else {
                            if (automatic) automaticOverlayRequested = true
                            actions.openOverlaySettings()
                            true
                        }
                    }
                    else -> {
                        setupPrefs.edit().putBoolean(KEY_FIRST_SETUP_FINISHED, true).apply()
                        firstSetupFinished = true
                        true
                    }
                }
            }
            LaunchedEffect(permissionSummary, currentPermissionStep, automaticSetupStarted) {
                if (automaticSetupStarted &&
                    currentPermissionStep == null &&
                    !permissionSummary.isSetupCompleteForFirstRun()
                ) {
                    delay(700)
                    runNextSetupStep(true)
                }
            }

            CompositionLocalProvider(
                LocalPermissionActions provides actions,
                LocalContextualHelpEnabled provides settings.contextualHelpEnabled,
            ) {
                CallGuardianTheme(
                    themeMode = settings.themeMode,
                    palette = settings.palette,
                    highContrast = settings.highContrast,
                ) {
                    GuardianAppChrome {
                        if (appReady && preloadMinimumElapsed) {
                            if (!firstSetupFinished && !permissionSummary.isSetupCompleteForFirstRun()) {
                                PermissionOnboardingScreen(
                                    permissions = permissionSummary,
                                    activeStep = currentPermissionStep?.label,
                                    automaticSetupStarted = automaticSetupStarted,
                                    onStartSetup = {
                                        automaticSetupStarted = true
                                        runNextSetupStep(true)
                                    },
                                    onContinue = { runNextSetupStep(false) },
                                    onRuntimePermissions = actions.requestRuntimePermissions,
                                    onCallScreeningRole = actions.requestCallScreeningRole,
                                    onOverlaySettings = actions.openOverlaySettings,
                                )
                            } else {
                                CallGuardianNavHost()
                            }
                        } else {
                            PreloadScreen()
                        }
                    }
                }
            }
        }
    }

    private enum class PermissionStep(val label: String) {
        RUNTIME("Permessi telefono e rubrica"),
        CALL_SCREENING("Ruolo ID chiamante e spam"),
        OVERLAY("Popup sopra le altre app"),
    }

    private fun PermissionSummary.isSetupCompleteForFirstRun(): Boolean =
        runtimePermissionsGranted &&
            callScreeningRoleHeld &&
            notificationPermissionGranted &&
            overlayAllowed

    private companion object {
        const val KEY_FIRST_SETUP_FINISHED = "first_setup_finished"
    }
}
