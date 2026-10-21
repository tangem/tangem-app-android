package com.tangem.google.auth

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

/**
 * Bridges a suspending caller (the Google authorizer) to the [ActivityResultRegistry] of the root Activity.
 *
 * Callers only see [launch] and [launchAccountPicker]. One resolution runs at a time (auth is user-driven),
 * guarded by [mutex]. Every request always completes: with the delivered result, or as canceled once the
 * result can no longer arrive — see [awaitUnlessUiLost] and [detach].
 */
@Singleton
class GoogleAuthActivityResultBridge @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val host = MutableStateFlow<Host?>(null)
    private val mutex = Mutex()

    private val registrations = mutableListOf<Registration>()

    fun attach(registry: ActivityResultRegistry, lifecycle: Lifecycle) {
        host.value?.let { detach(it.registry) }
        host.value = Host(registry = registry, lifecycle = lifecycle)
    }

    /**
     * Detaches [registry] because the Activity that owns it is going away. A request launched through it can
     * no longer get its result, so it completes as canceled. A no-op for a registry that is not attached.
     */
    fun detach(registry: ActivityResultRegistry) {
        if (host.value?.registry !== registry) return
        host.value = null
        releaseRegistrations { true }
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

            releaseRegistrations { it.isSettled }

            val deferred = CompletableDeferred<ActivityResult>()
            val registration = Registration()
            // A unique key per request, so a result that comes back after its request has ended cannot complete
            // whichever request runs next. Unique across processes too, as the registry restores its keys from
            // the saved state.
            val key = KEY_PREFIX + UUID.randomUUID()
            val launcher = target.registry.register(key = key, contract = contract) { result ->
                registration.isSettled = true
                deferred.complete(result)
            }
            registration.launcher = launcher
            registrations += registration

            val hostWatcher = launch {
                host.first { it !== target }
                deferred.completeAsLost(reason = "host detached")
            }
            try {
                try {
                    launcher.launch(input)
                } catch (e: Exception) {
                    registration.isSettled = true
                    throw e
                }
                // the registry delivers a result before the Activity resumes, so none is still on its way then
                awaitUnlessUiLost(target.lifecycle, resultGracePeriod = Duration.ZERO) { deferred.await() }
                    ?: deferred.run {
                        completeAsLost(reason = "host resumed")
                        await()
                    }
            } finally {
                hostWatcher.cancel()
            }
        }
    }

    /**
     * The registry keeps the key of a launched request until it dispatches the result, and the callback runs
     * before that: `unregister()` called from a request that has just been completed (by its result or as
     * canceled) leaves the key, and a result that arrives after it lands in the registry's saved state. So
     * a request stays registered until its result is consumed, and it is released by the next request or
     * by [detach].
     */
    private fun releaseRegistrations(predicate: (Registration) -> Boolean) {
        val released = registrations.filter(predicate)
        released.forEach { it.launcher.unregister() }
        registrations -= released.toSet()
    }

    private fun CompletableDeferred<ActivityResult>.completeAsLost(reason: String) {
        if (complete(ActivityResult(Activity.RESULT_CANCELED, null))) {
            TangemLogger.i("Google auth result is lost ($reason), the request is completed as canceled")
        }
    }

    private class Host(val registry: ActivityResultRegistry, val lifecycle: Lifecycle)

    private class Registration {
        lateinit var launcher: ActivityResultLauncher<*>
        var isSettled = false
    }

    private companion object {
        const val HOST_WAIT_TIMEOUT_MS = 1000L
        const val KEY_PREFIX = "google_auth_request_"
    }
}

/** No [ActivityResultRegistry] was attached in time, so a resolution intent cannot be shown */
class GoogleAuthLauncherUnavailableException : IllegalStateException("Google auth launcher is not registered")