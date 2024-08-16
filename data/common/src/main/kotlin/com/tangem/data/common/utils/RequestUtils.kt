package com.tangem.data.common.utils

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

@Suppress("UnconditionalJumpStatementInLoop")
suspend fun <T> retryOnError(priority: Boolean = false, call: suspend () -> T): T {
    while (true) {
        return try {
            call()
        } catch (e: Exception) {
            if (e is CancellationException) {
                currentCoroutineContext().ensureActive()
            }

            Timber.e(e, "Error occurred during retryOnError block")

            if (priority.not()) {
                yield()
                delay(timeMillis = 500)
            }

            continue
        }
    }
}