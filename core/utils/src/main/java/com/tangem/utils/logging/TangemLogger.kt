package com.tangem.utils.logging

import co.touchlab.kermit.Logger

/**
 * Application-level logger that wraps Kermit [Logger] with the same API.
 * All modules should use [TangemLogger] instead of importing Kermit directly.
 */
object TangemLogger {

    fun v(messageString: String, throwable: Throwable? = null) {
        Logger.v(messageString, throwable)
    }

    fun d(messageString: String, throwable: Throwable? = null) {
        Logger.d(messageString, throwable)
    }

    fun i(messageString: String, throwable: Throwable? = null) {
        Logger.i(messageString, throwable)
    }

    fun w(messageString: String, throwable: Throwable? = null) {
        Logger.w(messageString, throwable)
    }

    fun e(messageString: String, throwable: Throwable? = null) {
        Logger.e(messageString, throwable)
    }

    fun a(messageString: String, throwable: Throwable? = null) {
        Logger.a(messageString, throwable)
    }

    fun withTag(tag: String): TaggedLogger = TaggedLogger(tag)

    class TaggedLogger internal constructor(private val tag: String) {

        fun v(messageString: String, throwable: Throwable? = null) {
            Logger.withTag(tag).v(messageString, throwable)
        }

        fun d(messageString: String, throwable: Throwable? = null) {
            Logger.withTag(tag).d(messageString, throwable)
        }

        fun i(messageString: String, throwable: Throwable? = null) {
            Logger.withTag(tag).i(messageString, throwable)
        }

        fun w(messageString: String, throwable: Throwable? = null) {
            Logger.withTag(tag).w(messageString, throwable)
        }

        fun e(messageString: String, throwable: Throwable? = null) {
            Logger.withTag(tag).e(messageString, throwable)
        }

        fun a(messageString: String, throwable: Throwable? = null) {
            Logger.withTag(tag).a(messageString, throwable)
        }
    }
}