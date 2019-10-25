package com.tangem

object Log {

    private var loggerInstance: LoggerInterface? = null

    fun i(logTag: String, message: String) {
        loggerInstance?.i(logTag, message)
    }

    fun e(logTag: String, message: String) {
        loggerInstance?.e(logTag, message)
    }

    fun v(logTag: String, message: String) {

        loggerInstance?.v(logTag, message)
    }

    fun setLogger(logger: LoggerInterface) {
        loggerInstance = logger
    }
}

interface LoggerInterface {
    fun i(logTag: String, message: String)
    fun e(logTag: String, message: String)
    fun v(logTag: String, message: String)
}