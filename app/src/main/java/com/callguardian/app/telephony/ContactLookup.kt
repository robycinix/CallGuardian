package com.callguardian.app.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactLookup @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class ContactMatch(
        val displayName: String?,
    )

    fun exists(phoneNumber: String): Boolean {
        return lookup(phoneNumber) != null
    }

    fun lookup(phoneNumber: String): ContactMatch? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        if (phoneNumber == PhoneNumberNormalizer.ANONYMOUS) return null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME,
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val displayNameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val displayName = if (displayNameIndex >= 0) {
                cursor.getString(displayNameIndex)?.takeIf { it.isNotBlank() }
            } else {
                null
            }
            return ContactMatch(displayName = displayName)
        }
        return null
    }
}
