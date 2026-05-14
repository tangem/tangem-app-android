package com.tangem.utils.logging

/**
 * Common contract for application loggers.
 */
internal interface BaseLogger {
    fun v(messageString: String, throwable: Throwable? = null, shouldSanitize: Boolean = true)
    fun d(messageString: String, throwable: Throwable? = null, shouldSanitize: Boolean = true)
    fun i(messageString: String, throwable: Throwable? = null, shouldSanitize: Boolean = true)
    fun w(messageString: String, throwable: Throwable? = null, shouldSanitize: Boolean = true)
    fun e(messageString: String, throwable: Throwable? = null, shouldSanitize: Boolean = true)
    fun a(messageString: String, throwable: Throwable? = null, shouldSanitize: Boolean = true)
}