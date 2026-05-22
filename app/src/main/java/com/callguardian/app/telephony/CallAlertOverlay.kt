package com.callguardian.app.telephony

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.callguardian.app.core.model.CallAction
import com.callguardian.app.core.model.CallDecision
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallAlertOverlay @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentView: View? = null
    private var pendingShow: Runnable? = null
    private var pendingDismiss: Runnable? = null

    fun show(number: String, decision: CallDecision) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Overlay permission is not available; skipping call alert")
            return
        }

        mainHandler.post {
            dismiss(animate = false)
            val showRunnable = Runnable {
                pendingShow = null
                showFromBottom(number, decision)
            }
            pendingShow = showRunnable
            mainHandler.postDelayed(showRunnable, SHOW_DELAY_MS)
        }
    }

    private fun showFromBottom(number: String, decision: CallDecision) {
        val windowManager = context.getSystemService(WindowManager::class.java)
        val view = buildView(number, decision)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(18)
        }

        runCatching {
            view.alpha = 0f
            view.translationY = dp(110).toFloat()
            windowManager.addView(view, params)
            currentView = view
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ENTER_ANIMATION_MS)
                .start()
            Log.i(TAG, "Call alert overlay shown for action=${decision.action}")
            val dismissRunnable = Runnable { dismissView(view, animate = true) }
            pendingDismiss = dismissRunnable
            mainHandler.postDelayed(dismissRunnable, ALERT_DURATION_MS)
        }.onFailure {
            Log.e(TAG, "Unable to show call alert overlay", it)
        }
    }

    fun dismiss() {
        dismiss(animate = true)
    }

    private fun dismiss(animate: Boolean) {
        pendingShow?.let { mainHandler.removeCallbacks(it) }
        pendingShow = null
        pendingDismiss?.let { mainHandler.removeCallbacks(it) }
        pendingDismiss = null

        val view = currentView ?: return
        dismissView(view, animate)
    }

    private fun dismissView(view: View, animate: Boolean) {
        if (currentView == view) {
            currentView = null
            pendingDismiss = null
        }
        if (!animate) {
            removeView(view)
            return
        }

        view.animate()
            .alpha(0f)
            .translationY(dp(110).toFloat())
            .setDuration(EXIT_ANIMATION_MS)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.animate().setListener(null)
                    removeView(view)
                }
            })
            .start()
    }

    private fun removeView(view: View) {
        runCatching {
            context.getSystemService(WindowManager::class.java).removeView(view)
        }.onFailure {
            Log.w(TAG, "Unable to remove call alert overlay", it)
        }
    }

    private fun buildView(number: String, decision: CallDecision): View {
        val accent = when (decision.action) {
            CallAction.BLOCKED -> Color.rgb(185, 28, 28)
            CallAction.SILENCED -> Color.rgb(180, 83, 9)
            CallAction.WARNED -> Color.rgb(217, 119, 6)
            CallAction.ALLOWED -> Color.rgb(22, 101, 52)
        }
        val softAccent = when (decision.action) {
            CallAction.BLOCKED -> Color.rgb(254, 226, 226)
            CallAction.SILENCED -> Color.rgb(255, 237, 213)
            CallAction.WARNED -> Color.rgb(254, 243, 199)
            CallAction.ALLOWED -> Color.rgb(220, 252, 231)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                colors = intArrayOf(Color.WHITE, Color.rgb(255, 251, 235))
                setStroke(dp(2), accent)
            }
            elevation = dp(28).toFloat()
            minimumHeight = dp(188)
        }

        container.addView(View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(4).toFloat()
                setColor(accent)
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(5)))

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }

        header.addView(TextView(context).apply {
            text = when (decision.action) {
                CallAction.BLOCKED -> "STOP"
                CallAction.SILENCED -> "~"
                CallAction.WARNED -> "!"
                CallAction.ALLOWED -> "OK"
            }
            gravity = Gravity.CENTER
            setTextColor(accent)
            textSize = when (decision.action) {
                CallAction.BLOCKED -> 11f
                CallAction.ALLOWED -> 13f
                else -> 22f
            }
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(softAccent)
                setStroke(dp(2), accent)
            }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(10), 0)
        }

        textColumn.addView(TextView(context).apply {
            text = decision.title()
            setTextColor(Color.rgb(15, 23, 42))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        })

        textColumn.addView(TextView(context).apply {
            text = "$number - rischio ${decision.score}/100"
            setTextColor(accent)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        })

        header.addView(textColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(header)

        container.addView(TextView(context).apply {
            text = decision.primaryAdvice()
            setTextColor(Color.rgb(15, 23, 42))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(14), 0, dp(6))
            setLineSpacing(dp(2).toFloat(), 1f)
        })

        container.addView(TextView(context).apply {
            text = decision.reason
            setTextColor(Color.rgb(71, 85, 105))
            textSize = 13f
            maxLines = 2
        })

        val options = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, 0)
        }

        decision.suggestedOptions().forEachIndexed { index, option ->
            val isPrimary = index == 0
            options.addView(TextView(context).apply {
                text = option
                gravity = Gravity.CENTER
                setTextColor(if (isPrimary) Color.WHITE else accent)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(10), 0, dp(10), 0)
                background = GradientDrawable().apply {
                    cornerRadius = dp(18).toFloat()
                    setColor(if (isPrimary) accent else Color.WHITE)
                    setStroke(dp(2), accent)
                }
            }, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                marginEnd = dp(8)
            })
        }

        container.addView(options)

        return LinearLayout(context).apply {
            setPadding(dp(12), 0, dp(12), 0)
            addView(container)
        }
    }

    private fun CallDecision.title(): String = when (action) {
        CallAction.BLOCKED -> "Bloccata da CallGuardian"
        CallAction.SILENCED -> "Silenziata da CallGuardian"
        CallAction.WARNED -> "Avviso CallGuardian"
        CallAction.ALLOWED -> "Consentita da CallGuardian"
    }

    private fun CallDecision.primaryAdvice(): String = when (action) {
        CallAction.BLOCKED -> when {
            hasReason("lista bloccati") -> "Numero gia segnalato nelle tue regole: lascia bloccare."
            hasReason("Nazione bloccata") -> "Prefisso bloccato dalle tue regole paese: non intervenire."
            hasReason("Chiamate ravvicinate") -> "Tentativi ripetuti: lascia che CallGuardian interrompa il disturbo."
            hasReason("Numero anonimo") -> "Anonimo bloccato secondo le tue impostazioni: non serve rispondere."
            else -> "La chiamata supera la soglia di blocco: lasciala respingere."
        }
        CallAction.SILENCED -> when {
            hasReason("Numero anonimo") -> "Anonimo silenziato: richiama solo se trovi un motivo affidabile."
            hasReason("Regola oraria") -> "Fascia protetta attiva: lascia squillare senza interromperti."
            else -> "Chiamata silenziata: verifica dopo, senza richiamare d'impulso."
        }
        CallAction.WARNED -> when {
            hasReason("Numero non presente in rubrica") -> "Numero non in rubrica: rispondi solo se stai aspettando qualcuno."
            hasReason("Numero estero sconosciuto") -> "Estero non riconosciuto: evita di rispondere se non lo aspettavi."
            hasReason("Numero anonimo") -> "Numero nascosto: rispondi solo se il contesto ti torna."
            hasReason("Chiamate ravvicinate") -> "Ha chiamato piu volte: rifiuta se non riconosci il numero."
            hasReason("Regola") -> "Una tua regola lo segnala: valuta prima di rispondere."
            score >= 75 -> "Rischio alto: rifiuta, salvo chiamata attesa."
            else -> "Rischio moderato: rispondi solo se numero o contesto tornano."
        }
        CallAction.ALLOWED -> "Numero compatibile con le tue regole: puoi rispondere."
    }

    private fun CallDecision.suggestedOptions(): List<String> = when (action) {
        CallAction.BLOCKED -> when {
            hasReason("lista bloccati") -> listOf("Lascia bloccata", "Verifica regola dopo")
            hasReason("Nazione bloccata") -> listOf("Lascia bloccata", "Controlla paese dopo")
            else -> listOf("Lascia bloccare", "Controlla il log dopo")
        }
        CallAction.SILENCED -> when {
            hasReason("Regola oraria") -> listOf("Non interrompere", "Verifica dopo")
            else -> listOf("Ignora ora", "Verifica dopo")
        }
        CallAction.WARNED -> when {
            hasReason("Numero estero sconosciuto") -> listOf("Rifiuta", "Rispondi solo se attesa")
            hasReason("Numero anonimo") -> listOf("Rifiuta", "Rispondi solo se sensata")
            hasReason("Chiamate ravvicinate") -> listOf("Rifiuta", "Valuta dopo")
            score >= 75 -> listOf("Rifiuta", "Rispondi solo se attesa")
            else -> listOf("Valuta", "Rifiuta se sospetta")
        }
        CallAction.ALLOWED -> listOf("Rispondi", "Lascia squillare")
    }

    private fun CallDecision.hasReason(value: String): Boolean =
        reason.contains(value, ignoreCase = true)

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "CallGuardianOverlay"
        const val SHOW_DELAY_MS = 250L
        const val ENTER_ANIMATION_MS = 220L
        const val EXIT_ANIMATION_MS = 180L
        const val ALERT_DURATION_MS = 8_000L
    }
}
