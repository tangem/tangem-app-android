package com.tangem.google.auth

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges a suspending caller (the Google authorizer) to the [ActivityResultRegistry] of the root Activity.
 *
 * Callers only see [launch] and [launchAccountPicker]. One resolution runs at a time (auth is user-driven),
 * guarded by [mutex]. Every request always completes: with the delivered result, or as canceled once the
 * result can no longer arrive — see [HostObserver] and [detach].
 */
@Singleton
class GoogleAuthActivityResultBridge @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val host = MutableStateFlow<Host?>(null)
    private val mutex = Mutex()

    private var pending: PendingRequest? = null

    fun attach(registry: ActivityResultRegistry, lifecycle: Lifecycle) {
        host.value?.let { detach(it.registry) }

        val observer = HostObserver()
        lifecycle.addObserver(observer)
        host.value = Host(registry = registry, lifecycle = lifecycle, observer = observer)
    }

    /**
     * Detaches [registry] because the Activity that owns it is going away. A request launched through it can
     * no longer get its result, so it completes as canceled. A no-op for a registry that is not attached.
     */
    fun detach(registry: ActivityResultRegistry) {
        val current = host.value?.takeIf { it.registry === registry } ?: return
        current.lifecycle.removeObserver(current.observer)
        host.value = null
        pending?.cancel(reason = "host detached")
    }

    /**
     * Runs [intentSender] through the attached registry and awaits its result.
     *
     * @throws GoogleAuthLauncherUnavailableException when no registry is attached within
     * [HOST_WAIT_TIMEOUT_MS] — deliberately not a `CancellationException`, so the caller can report
     * a real error instead of dying silently.
     */
    suspend fun launch(intentSender: IntentSender): ActivityResult = awaitResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        input = IntentSenderRequest.Builder(intentSender).build(),
    )

    /**
     * Runs the account picker [intent] through the attached registry and awaits its result.
     *
     * @throws GoogleAuthLauncherUnavailableException on the same terms as [launch]
     */
    suspend fun launchAccountPicker(intent: Intent): ActivityResult = awaitResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        input = intent,
    )

    private suspend fun <I> awaitResult(
        contract: ActivityResultContract<I, ActivityResult>,
        input: I,
    ): ActivityResult = mutex.withLock {
        withContext(dispatchers.mainImmediate) {
            val target = withTimeoutOrNull(HOST_WAIT_TIMEOUT_MS) { host.filterNotNull().first() }
                ?: throw GoogleAuthLauncherUnavailableException()

            val deferred = CompletableDeferred<ActivityResult>()
            // A unique key per request: a result that comes back after its request has ended lands on an
            // unregistered key and is parked by the registry unclaimed, instead of completing whichever request
            // runs next. Unique across processes too, as the registry restores its keys from the saved state.
            val key = KEY_PREFIX + UUID.randomUUID()
            val launcher = target.registry.register(key = key, contract = contract) { deferred.complete(it) }
            val request = PendingRequest(
                deferred = deferred,
                isHostLeft = !target.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
            )
            pending = request
            try {
                launcher.launch(input)
                deferred.await()
            } finally {
                if (pending === request) pending = null
                launcher.unregister()
            }
        }
    }

    /**
     * The registry delivers a result before the Activity resumes, so a request still waiting when the
     * Activity resumes after having left the foreground never gets one: the Google UI is no longer on top.
     *
     * That is what happens when the app is minimized while the Google UI is shown and opened again from the
     * launcher: `MainActivity` is `singleInstance`, so the Google UI runs in a task of its own and the
     * launcher brings back only the app's task, leaving the Google one hidden behind it.
     */
    private inner class HostObserver : LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val request = pending ?: return
            when (event) {
                Lifecycle.Event.ON_PAUSE -> request.isHostLeft = true
                Lifecycle.Event.ON_RESUME -> if (request.isHostLeft) request.cancel(reason = "host resumed")
                else -> Unit
            }
        }
    }

    private class Host(
        val registry: ActivityResultRegistry,
        val lifecycle: Lifecycle,
        val observer: LifecycleEventObserver,
    )

    private class PendingRequest(
        private val deferred: CompletableDeferred<ActivityResult>,
        var isHostLeft: Boolean,
    ) {

        fun cancel(reason: String) {
            if (deferred.complete(ActivityResult(Activity.RESULT_CANCELED, null))) {
                TangemLogger.i("Google auth result is lost ($reason), the request is completed as canceled")
            }
        }
    }

    private companion object {
        const val HOST_WAIT_TIMEOUT_MS = 1000L
        const val KEY_PREFIX = "google_auth_request_"
    }
}

/** No [ActivityResultRegistry] was attached in time, so a resolution intent cannot be shown */
class GoogleAuthLauncherUnavailableException : IllegalStateException("Google auth launcher is not registered")