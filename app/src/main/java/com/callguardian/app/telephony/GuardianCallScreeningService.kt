package com.callguardian.app.telephony

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.callguardian.app.core.model.CallAction
import com.callguardian.app.core.model.CallDecision
import com.callguardian.app.data.repository.GuardianRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class GuardianCallScreeningService : CallScreeningService() {
    @Inject lateinit var repository: GuardianRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var callAlertOverlay: CallAlertOverlay
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        serviceScope.launch {
            val decision = runCatching {
                withContext(Dispatchers.IO) {
                    repository.evaluateIncomingCall(number)
                }
            }.onFailure {
                Log.e(TAG, "Unable to screen incoming call; allowing it by default", it)
                respondToCall(callDetails, allowCallResponse())
            }.getOrNull() ?: return@launch

            handleDecision(callDetails, number, decision)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleDecision(callDetails: Call.Details, number: String, decision: CallDecision) {
        Log.i(TAG, "Screened incoming call: action=${decision.action}, score=${decision.score}, reason=${decision.reason}")

        respondToCall(callDetails, buildCallResponse(decision))
        if (decision.action != CallAction.ALLOWED) {
            val displayNumber = number.ifBlank { "Numero anonimo" }
            callAlertOverlay.show(displayNumber, decision)
            notificationHelper.showCallDecision(displayNumber, decision)
        }
    }

    private fun buildCallResponse(decision: CallDecision): CallResponse = when (decision.action) {
            CallAction.BLOCKED -> CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            CallAction.SILENCED -> CallResponse.Builder()
                .setDisallowCall(false)
                .setSilenceCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            CallAction.WARNED, CallAction.ALLOWED -> CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
    }

    private fun allowCallResponse(): CallResponse = CallResponse.Builder()
        .setDisallowCall(false)
        .setRejectCall(false)
        .setSkipCallLog(false)
        .setSkipNotification(false)
        .build()

    private companion object {
        const val TAG = "CallGuardianScreening"
    }
}
