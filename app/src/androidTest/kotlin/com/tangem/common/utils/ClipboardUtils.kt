package com.tangem.common.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.SystemClock
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
// The app writes the clip after the snackbar confirms the copy, so a single read sees it empty. Polled here
// rather than through awaitSuccess so a miss reports the two values instead of a bare timeout.
fun assertClipboardTextEquals(
    expected: String,
    context: Context = ApplicationProvider.getApplicationContext(),
    timeoutMillis: Long = CLIPBOARD_TIMEOUT_MS,
) {
    val deadline = SystemClock.uptimeMillis() + timeoutMillis
    var actual = getClipboardText(context)
    while (actual != expected && SystemClock.uptimeMillis() < deadline) {
        SystemClock.sleep(CLIPBOARD_POLL_INTERVAL_MS)
        actual = getClipboardText(context)
    }
    assertEquals("Clipboard text mismatch", expected, actual)
}

fun assertClipboardIsEmpty(context: Context = ApplicationProvider.getApplicationContext()) {
    val actual = getClipboardText(context)
    assertTrue("Expected empty clipboard but was: '$actual'", actual.isNullOrEmpty())
}

private const val CLIPBOARD_TIMEOUT_MS = 5_000L
private const val CLIPBOARD_POLL_INTERVAL_MS = 100L