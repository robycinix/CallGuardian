package com.callguardian.app.core.permissions

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.callguardian.app.core.model.PermissionSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun summary(): PermissionSummary {
        val required = buildList {
            add(Manifest.permission.READ_CONTACTS)
        }
        val runtimeGranted = required.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val roleManager = context.getSystemService<RoleManager>()
        val roleHeld = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

        return PermissionSummary(
            runtimePermissionsGranted = runtimeGranted,
            callScreeningRoleHeld = roleHeld,
            overlayAllowed = Settings.canDrawOverlays(context),
            notificationPermissionGranted = notificationGranted,
        )
    }
}
