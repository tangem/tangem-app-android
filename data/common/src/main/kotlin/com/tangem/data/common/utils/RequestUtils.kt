package com.tangem.data.common.utils

import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import kotlin.coroutines.cancellation.CancellationException

@Suppress("UnconditionalJumpStatementInLoop", "MagicNumber")
suspend fun <T> retryOnError(priority: Boolean = false, startRetryDelay: Int = 500, call: suspend () -> T): T {
    var currentDelay = startRetryDelay
    var priorityCounter = 5
    while (true) {
        return try {
            call()
        } catch (e: Exception) {
            if (e is CancellationException) {
                currentCoroutineContext().ensureActive()
            }

            TangemLogger.e("Error occurred during retryOnError block", e)

            if (priority && priorityCounter > 0) {
                --priorityCounter
            } else {
                yield()
                delay(timeMillis = currentDelay.toLong())
                currentDelay *= 2
            }

            continue
        }
    }
}