package com.tangem.utils.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.Level
import java.util.logging.Logger

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
            //it delegates logging to android Logger, cause cant use timber in java module
            Logger.getLogger("CoroutineExceptHandler").log(
                Level.INFO, "CoroutineException: from: $from, exception: $exceptionAsString",
            )
            throw throwable
        }
    }
}