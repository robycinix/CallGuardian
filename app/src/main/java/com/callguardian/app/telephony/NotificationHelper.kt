package com.callguardian.app.telephony

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.callguardian.app.MainActivity
import com.callguardian.app.R
import com.callguardian.app.core.model.CallDecision
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CALLS,
                context.getString(R.string.notification_channel_calls),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_calls_description)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STATUS,
                context.getString(R.string.notification_channel_status),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    fun showCallDecision(number: String, decision: CallDecision) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_guardian_shield)
            .setContentTitle("CallGuardian: ${decision.actionLabel()}")
            .setContentText("$number - ${decision.reason}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$number\n${decision.reason}\nPunteggio rischio: ${decision.score}"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(number.hashCode(), notification)
    }

    private fun CallDecision.actionLabel(): String = when (action.name) {
        "BLOCKED" -> "bloccata"
        "SILENCED" -> "silenziata"
        "WARNED" -> "avviso"
        else -> "consentita"
    }

    private companion object {
        const val CHANNEL_CALLS = "callguardian_call_alerts"
        const val CHANNEL_STATUS = "callguardian_status"
    }
}
