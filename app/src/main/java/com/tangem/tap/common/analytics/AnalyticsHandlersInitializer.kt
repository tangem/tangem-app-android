package com.tangem.tap.common.analytics

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.Lazy
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates analytics SDK handlers (and so initializes the SDKs) off the main thread at app start.
 *
 * SDKs hook the activity lifecycle when they are initialized (push-opened tracking, foreground sessions, app open),
 * so an activity that can start the process from outside the app (today only `MainActivity`) must call
 * [awaitInitialized] before its `super.onCreate`. Services and receivers don't need to wait: events are queued until
 * the handlers are ready.
 */
@Singleton
class AnalyticsHandlersInitializer @Inject constructor(
    private val handlers: Lazy<Set<@JvmSuppressWildcards AnalyticsHandler>>,
    private val appScope: AppCoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val initialized = CountDownLatch(1)

    @Volatile
    private var isStarted = false

    fun initialize() {
        isStarted = true
        appScope.launch(dispatchers.io) {
            try {
                handlers.get()
            } catch (e: Exception) {
                TangemLogger.e("Analytics handlers initialization failed", e)
            } finally {
                initialized.countDown()
            }
        }
    }

    /**
     * Blocks until [initialize] has finished; a hung SDK must not freeze the first screen, hence the timeout. Returns
     * immediately if [initialize] was never called (e.g. a test application that skips app initialization).
     */
    fun awaitInitialized() {
        if (!isStarted) return
        if (!initialized.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            TangemLogger.e("Analytics handlers initialization did not finish in $AWAIT_TIMEOUT_SECONDS s")
        }
    }

    private companion object {
        const val AWAIT_TIMEOUT_SECONDS = 3L
    }
}