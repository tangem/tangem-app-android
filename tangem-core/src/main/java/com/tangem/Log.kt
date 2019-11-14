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

/**
 * Interface for logging events within the SDK.
 *
 * It allows to use Android logger or to choose another.
 */
interface LoggerInterface {
    fun i(logTag: String, message: String)
    fun e(logTag: String, message: String)
    fun v(logTag: String, message: String)
}