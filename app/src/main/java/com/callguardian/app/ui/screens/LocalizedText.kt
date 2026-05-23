package com.callguardian.app.ui.screens

import java.util.Locale

fun uiText(it: String, en: String): String {
    return if (Locale.getDefault().language == "it") it else en
}
