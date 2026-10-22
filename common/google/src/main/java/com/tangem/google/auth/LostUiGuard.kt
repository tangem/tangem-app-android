package com.tangem.google.auth

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
import kotlin.time.Duration

/**
 * Runs [block], which waits on a Google UI shown over the host, and gives up with `null` once that UI is gone.
 *
 * `MainActivity` is `singleInstance`, so the Google UI runs in a task of its own: the app minimized while it is
 * shown and opened again from the launcher comes back without it, and [block] would wait forever. The Google UI
 * keeps the host paused while it is on top, so the host resuming after it left the foreground means the UI is
 * gone. Stop and start alone mean nothing: the screen turned off and on over the Google UI starts the host
 * behind it without resuming it. A host that is not started at the call has left the foreground already.
 *
 * [resultGracePeriod] lets in a result that is still on its way when the host resumes.
 *
 * Must be called on the main thread, as [lifecycle] is.
 */
internal suspend fun <T : Any> awaitUnlessUiLost(
    lifecycle: Lifecycle,
    resultGracePeriod: Duration,
    block: suspend () -> T,
): T? = coroutineScope {
    val uiLost = CompletableDeferred<Unit>()
    var isHostLeft = !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> isHostLeft = true
            Lifecycle.Event.ON_RESUME -> if (isHostLeft) uiLost.complete(Unit)
            else -> Unit
        }
    }
    lifecycle.addObserver(observer)
    try {
        val result = async { block() }
        val lostResult = async {
            uiLost.await()
            delay(resultGracePeriod)
            null
        }
        select {
            result.onAwait { it }
            lostResult.onAwait { it }
        }.also { coroutineContext.cancelChildren() }
    } finally {
        lifecycle.removeObserver(observer)
    }
}