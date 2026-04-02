package com.tangem.utils.coroutines

import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.PrintWriter
import java.io.StringWriter

/**
[REDACTED_AUTHOR]
 */
object FeatureCoroutineExceptionHandler {

    // add an external logger (FbAnalytics) for handling errors
    fun create(from: String): CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val exceptionAsString: String = sw.toString()
        TangemLogger.i("CoroutineException: from: $from, exception: $exceptionAsString")
        throw throwable
    }
}