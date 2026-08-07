package com.tangem.common.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

fun getClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return if (clipboard.hasPrimaryClip()) {
        clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    } else {
        null
    }
}

fun setClipboardText(context: Context, text: String?) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("label", text)
    clipboard.setPrimaryClip(clip)
}

fun clearClipboard(
    context: Context = ApplicationProvider.getApplicationContext()
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.clearPrimaryClip()
}

// Kotlin's assert() is a no-op on device (JVM assertions disabled) — use JUnit asserts here.
fun assertClipboardTextEquals(
    expected: String,
    context: Context = ApplicationProvider.getApplicationContext()
) {
    assertEquals("Clipboard text mismatch", expected, getClipboardText(context))
}

fun assertClipboardIsEmpty(context: Context = ApplicationProvider.getApplicationContext()) {
    val actual = getClipboardText(context)
    assertTrue("Expected empty clipboard but was: '$actual'", actual.isNullOrEmpty())
}