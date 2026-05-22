package com.callguardian.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.callguardian.app.data.local.CallGuardianDatabase
import com.callguardian.app.data.local.SecureDatabaseKeyProvider
import com.callguardian.app.telephony.PhoneNumberNormalizer
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class DebugRuleAdminReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            val message = runBlocking {
                val database = openDatabase(context)
                try {
                    when (intent.action) {
                        ACTION_DELETE_RULE_FOR_NUMBER -> {
                            val rawNumber = intent.getStringExtra(EXTRA_NUMBER).orEmpty()
                            val values = candidateValues(rawNumber, PhoneNumberNormalizer())
                            val deleted = database.dao().deleteRulesByValues(values)
                            "deleted=$deleted values=${values.joinToString()}"
                        }
                        ACTION_DUMP_RULES -> {
                            database.dao().enabledRules().joinToString(separator = "\n") { rule ->
                                "id=${rule.id} type=${rule.type} action=${rule.action} value=${rule.value} label=${rule.label}"
                            }.ifBlank { "no enabled rules" }
                        }
                        else -> "ignored action=${intent.action}"
                    }
                } finally {
                    database.close()
                }
            }
            Log.i(TAG, message)
            setResultCode(1)
            setResultData(message)
        }.onFailure { error ->
            Log.e(TAG, "debug command failed", error)
            setResultCode(0)
            setResultData("error=${error.message}")
        }
    }

    private fun openDatabase(context: Context): CallGuardianDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(SecureDatabaseKeyProvider(context).getOrCreatePassphrase())
        return Room.databaseBuilder(context, CallGuardianDatabase::class.java, "callguardian.db")
            .openHelperFactory(factory)
            .build()
    }

    private fun candidateValues(rawNumber: String, normalizer: PhoneNumberNormalizer): List<String> {
        val normalized = normalizer.normalize(rawNumber)
        val digits = rawNumber.filter { it.isDigit() }
        return buildSet {
            add(rawNumber)
            add(digits)
            add(normalized)
            if (digits.startsWith("0")) {
                add("+39${digits.drop(1)}")
                add("+39$digits")
            }
            if (digits.startsWith("00")) {
                add("+${digits.drop(2)}")
            }
        }.filter { it.isNotBlank() }
    }

    companion object {
        private const val TAG = "CGDebugRuleAdmin"
        const val ACTION_DELETE_RULE_FOR_NUMBER = "com.callguardian.app.debug.DELETE_RULE_FOR_NUMBER"
        const val ACTION_DUMP_RULES = "com.callguardian.app.debug.DUMP_RULES"
        const val EXTRA_NUMBER = "number"
    }
}
