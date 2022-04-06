package com.tangem.domain.common

import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter

/**
[REDACTED_AUTHOR]
 */
class FeatureCoroutineExceptionHandler {

    // add an external logger (FbAnalytics) for handling errors
    companion object {
        fun create(from: String): CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val exceptionAsString: String = sw.toString()
            Timber.e("CoroutineException: from: %s, exception: %s", from, exceptionAsString)
            throw throwable
        }
    }
}