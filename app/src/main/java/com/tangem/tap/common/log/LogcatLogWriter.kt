package com.tangem.tap.common.log

import android.os.Build
import android.util.Log
import com.tangem.utils.logging.Severity
import com.tangem.utils.logging.TangemLogger

/**
 * [TangemLogger.LogWriter] that pretty-prints log entries to Logcat.
 *
 * Wraps each entry in unicode borders and chunks long messages so that they fit
 * Android's per-entry byte limit (~4076 bytes).
 */
internal class LogcatLogWriter : TangemLogger.LogWriter {

    override fun write(
        severity: Severity,
        tag: String,
        message: String,
        throwable: Throwable?,
        shouldSanitize: Boolean,
    ) {
        val priority = severity.toAndroidPriority()
        val truncatedTag = tag.truncateForLogcat()
        val finalMessage = if (throwable != null) {
            "$message\n${Log.getStackTraceString(throwable)}"
        } else {
            message
        }
        printBoxed(priority, truncatedTag, finalMessage)
    }

    private fun printBoxed(priority: Int, tag: String, message: String) {
        Log.println(priority, tag, TOP_BORDER)
        val bytes = message.toByteArray()
        val length = bytes.size
        if (length <= CHUNK_SIZE) {
            printContent(priority, tag, message)
        } else {
            var i = 0
            while (i < length) {
                val count = (length - i).coerceAtMost(CHUNK_SIZE)
                printContent(priority, tag, String(bytes, i, count))
                i += CHUNK_SIZE
            }
        }
        Log.println(priority, tag, BOTTOM_BORDER)
    }

    private fun printContent(priority: Int, tag: String, chunk: String) {
        chunk.split(System.lineSeparator()).forEach { line ->
            Log.println(priority, tag, "$HORIZONTAL_LINE $line")
        }
    }

    @Suppress("MagicNumber")
    private fun String.truncateForLogcat(): String {
        // Tag length limit was removed in API 26.
        return if (length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) {
            this
        } else {
            substring(0, MAX_TAG_LENGTH)
        }
    }

    private fun Severity.toAndroidPriority(): Int = when (this) {
        Severity.Verbose -> Log.VERBOSE
        Severity.Debug -> Log.DEBUG
        Severity.Info -> Log.INFO
        Severity.Warn -> Log.WARN
        Severity.Error -> Log.ERROR
        Severity.Assert -> Log.ASSERT
    }

    private companion object {
        // Android's max per-entry byte limit is ~4076; leave headroom for borders.
        const val CHUNK_SIZE = 4000

        const val MAX_TAG_LENGTH = 23

        const val HORIZONTAL_LINE = "│"
        const val DIVIDER = "────────────────────────────────────────────────────────"
        const val TOP_BORDER = "┌$DIVIDER$DIVIDER"
        const val BOTTOM_BORDER = "└$DIVIDER$DIVIDER"
    }
}