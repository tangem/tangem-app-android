package com.tangem.google.auth

import android.app.Activity
import android.content.IntentSender
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges a suspending caller (the Google authorizer) to an [ActivityResultLauncher] owned by the UI.
 *
 * Mirrors the requester-proxy overlays (`ScanFailsRequesterProxy`, `HotWalletPasswordRequesterProxy`):
 * the concrete launcher is registered by a host mounted at the app root, while callers only see
 * [launch]. One resolution runs at a time (auth is user-driven), guarded by [mutex].
 */
@Singleton
class GoogleAuthActivityResultBridge @Inject constructor() {

    private val launcher = MutableStateFlow<ActivityResultLauncher<IntentSenderRequest>?>(null)
    private val mutex = Mutex()

    @Volatile
    private var pending: CompletableDeferred<ActivityResult>? = null

    fun registerLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>?) {
        this.launcher.value = launcher
        if (launcher == null) {
            pending?.complete(ActivityResult(Activity.RESULT_CANCELED, null))
            pending = null
        }
    }

    fun onResult(result: ActivityResult) {
        pending?.complete(result)
        pending = null
    }

    suspend fun launch(intentSender: IntentSender): ActivityResult = mutex.withLock {
        val target = withTimeoutOrNull(LAUNCHER_WAIT_TIMEOUT_MS) {
            launcher.filterNotNull().first()
        } ?: error("Google auth launcher is not mounted")

        val deferred = CompletableDeferred<ActivityResult>()
        pending = deferred
        try {
            target.launch(IntentSenderRequest.Builder(intentSender).build())
            deferred.await()
        } finally {
            // If the caller is cancelled while the consent UI is still up, hold the slot until the
            // stale result is delivered (or the host unmounts) so it can't complete the next request.
            // Bounded so a result that never arrives can't wedge the bridge.
            withContext(NonCancellable) {
                withTimeoutOrNull(RESULT_DRAIN_TIMEOUT_MS) { deferred.join() }
            }
            pending = null
        }
    }

    private companion object {
        const val LAUNCHER_WAIT_TIMEOUT_MS = 1000L
        const val RESULT_DRAIN_TIMEOUT_MS = 60_000L
    }
}