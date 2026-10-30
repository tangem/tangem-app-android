package com.tangem.core.res

/**
 * Reports a string resource that failed to format. String helpers here are top-level functions without DI, so the app
 * installs the reporter at startup; until then a failure is only logged.
 */
fun interface StringResourceErrorReporter {

    fun report(exception: Throwable)

    companion object {

        @Volatile
        internal var instance: StringResourceErrorReporter? = null
            private set

        fun install(reporter: StringResourceErrorReporter) {
            instance = reporter
        }
    }
}