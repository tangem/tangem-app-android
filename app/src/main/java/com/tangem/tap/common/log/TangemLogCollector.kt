package com.tangem.tap.common.log

import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.TangemSdkLogger
import java.text.SimpleDateFormat
import java.util.*

class TangemLogCollector(
    private val levels: List<Log.Level>,
    private val messageFormatter: LogFormat,
) : TangemSdkLogger {

    private val dateFormatter = SimpleDateFormat("HH:mm:ss.SSS")
    private val logs = mutableListOf<String>()
    private val mutex = Object()

    override fun log(message: () -> String, level: Log.Level) {
        if (!levels.contains(level)) return

        synchronized(mutex) {
            val formattedMessage = messageFormatter.format(message, level)
            val logMessage = "${dateFormatter.format(Date())}: $formattedMessage"
            logs.add("$logMessage\n")
        }
    }

    fun getLogs(): List<String> = synchronized(mutex) { logs.toList() }

    fun clearLogs() = synchronized(mutex) { logs.clear() }
}