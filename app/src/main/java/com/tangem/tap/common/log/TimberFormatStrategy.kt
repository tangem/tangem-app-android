package com.tangem.tap.common.log

import com.orhanobut.logger.FormatStrategy
import com.orhanobut.logger.LogStrategy
import com.orhanobut.logger.LogcatLogStrategy

class TimberFormatStrategy : FormatStrategy {

    private val logStrategy: LogStrategy = LogcatLogStrategy()

    override fun log(priority: Int, tag: String?, message: String) {
        logTopBorder(priority, tag)
        val bytes = message.toByteArray()
        val length = bytes.size
        if (length <= CHUNK_SIZE) {
            logContent(priority, tag, message)
            logBottomBorder(priority, tag)
            return
        }
        var i = 0
        while (i < length) {
            val count = (length - i).coerceAtMost(CHUNK_SIZE)
            // create a new String with system's default charset (which is UTF-8 for Android)
            logContent(priority, tag, String(bytes, i, count))
            i += CHUNK_SIZE
        }
        logBottomBorder(priority, tag)
    }

    private fun logTopBorder(logType: Int, tag: String?) {
        logChunk(logType, tag, TOP_BORDER)
    }

    private fun logBottomBorder(logType: Int, tag: String?) {
        logChunk(logType, tag, BOTTOM_BORDER)
    }

    private fun logContent(logType: Int, tag: String?, chunk: String) {
        chunk.split(System.lineSeparator()).forEach { line ->
            logChunk(logType, tag, "$HORIZONTAL_LINE $line")
        }
    }

    private fun logChunk(priority: Int, tag: String?, chunk: String) {
        logStrategy.log(priority, tag, chunk)
    }

    private companion object {
        /**
         * Android's max limit for a log entry is ~4076 bytes,
         * so 4000 bytes is used as chunk size since default charset
         * is UTF-8
         */
        private const val CHUNK_SIZE = 4000

        const val TOP_LEFT_CORNER = "┌"
        const val BOTTOM_LEFT_CORNER = "└"
        const val HORIZONTAL_LINE = "│"
        const val DOUBLE_DIVIDER = "────────────────────────────────────────────────────────"
        const val TOP_BORDER = TOP_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER
        const val BOTTOM_BORDER = BOTTOM_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER
    }
}