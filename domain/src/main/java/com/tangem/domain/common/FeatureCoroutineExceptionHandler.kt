package com.tangem.domain.common

import java.io.PrintWriter
import java.io.StringWriter
import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber

/**
 * Created by Anton Zhilenkov on 30/03/2022.
 */
class FeatureCoroutineExceptionHandler {

    // add an external logger (FbAnalytics) for handling errors
    companion object {
        fun create(from: String): CoroutineExceptionHandler {
            return CoroutineExceptionHandler { _, throwable ->
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val exceptionAsString: String = sw.toString()
                Timber.e(
                    "CoroutineException: from: %s, exception: %s",
                    from,
                    exceptionAsString,
                )
                throw throwable
            }
        }
    }
}
