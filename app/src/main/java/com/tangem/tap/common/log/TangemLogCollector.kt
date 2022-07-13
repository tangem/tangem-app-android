package com.tangem.tap.common.log

import com.tangem.Log
import com.tangem.TangemSdkLogger
import java.text.SimpleDateFormat
import java.util.Date

class TangemLogCollector : TangemSdkLogger {
    private val dateFormatter = SimpleDateFormat("HH:mm:ss.SSS")
    private val logs = mutableListOf<String>()
    private val mutex = Object()

    override fun log(message: () -> String, level: Log.Level) {
        val time = dateFormatter.format(Date())
        synchronized(mutex) {
            logs.add("$time: ${message()}\n")
        }
    }

    fun getLogs(): List<String> = synchronized(mutex) { logs.toList() }

    fun clearLogs() {
        synchronized(mutex) { logs.clear() }
    }
}