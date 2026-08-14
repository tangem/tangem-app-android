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
 * the concrete launcher is registered by the root Activity, while callers only see [launch]. One
 * resolution runs at a time (auth is user-driven), guarded by [mutex].
 */
@Singleton
class GoogleAuthActivityResultBridge @Inject constructor() {

    private val launcher = MutableStateFlow<ActivityResultLauncher<IntentSenderRequest>?>(null)
    private val mutex = Mutex()

    @Volatile
    private var pending: CompletableDeferred<ActivityResult>? = null

    fun registerLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        this.launcher.value = launcher
    }

    /**
     * Detaches the launcher because the UI that owns it is going away. A request still waiting for a
     * result is completed as canceled, so its caller does not hang until [RESULT_DRAIN_TIMEOUT_MS].
     */
    fun unregisterLauncher() {
        launcher.value = null
        pending?.complete(ActivityResult(Activity.RESULT_CANCELED, null))
        pending = null
    }

    fun onResult(result: ActivityResult) {
        pending?.complete(result)
        pending = null
    }

    /**
     * Runs [intentSender] through the registered launcher and awaits its result.
     *
     * @throws GoogleAuthLauncherUnavailableException when no launcher shows up within
     * [LAUNCHER_WAIT_TIMEOUT_MS] — deliberately not a `CancellationException`, so the caller can report
     * a real error instead of dying silently.
     */
    suspend fun launch(intentSender: IntentSender): ActivityResult = mutex.withLock {
        val target = withTimeoutOrNull(LAUNCHER_WAIT_TIMEOUT_MS) { launcher.filterNotNull().first() }
            ?: throw GoogleAuthLauncherUnavailableException()

        val deferred = CompletableDeferred<ActivityResult>()
        pending = deferred
        try {
            target.launch(IntentSenderRequest.Builder(intentSender).build())
            deferred.await()
        } finally {
            // If the caller is cancelled while the consent UI is still up, hold the slot until the
            // stale result is delivered (or the launcher is unregistered) so it can't complete the
            // next request. Bounded so a result that never arrives can't wedge the bridge.
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

/** No [ActivityResultLauncher] was registered in time, so a resolution intent cannot be shown */
class GoogleAuthLauncherUnavailableException : IllegalStateException("Google auth launcher is not registered")