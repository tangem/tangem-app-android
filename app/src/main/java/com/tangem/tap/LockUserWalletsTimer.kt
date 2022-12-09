package com.tangem.tap

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration

internal class LockUserWalletsTimer(
    owner: LifecycleOwner,
    private val duration: Duration = with(Duration) { 5.minutes },
) : LifecycleOwner by owner,
    DefaultLifecycleObserver {

    private var delayJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }
    private var isStopped = false
    private var openWelcomeScreenWhenResumed = false

    init {
        lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        Timber.d(
            """
            Owner resumed
            |- Was stopped: $isStopped
            |- Need to open welcome screen: $openWelcomeScreenWhenResumed
        """.trimIndent(),
        )
        isStopped = false
        start()
        if (openWelcomeScreenWhenResumed) {
            store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Welcome))
            openWelcomeScreenWhenResumed = false
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        Timber.d("Owner stopped")
        isStopped = true
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Timber.d("Owner destroyed")
        stop()
    }

    fun restart() {
        if (delayJob == null) return
        Timber.d(
            """
                Timer restart
                |- Duration millis: ${duration.inWholeMilliseconds}
            """.trimIndent(),
        )
        start(log = false)
    }

    private fun start(log: Boolean = true) {
        if (log) {
            Timber.d(
                """
                    Timer start
                    |- Duration millis: ${duration.inWholeMilliseconds}
                """.trimIndent(),
            )
        }
        delayJob = createDelayJob()
    }

    private fun stop(log: Boolean = true) {
        if (log) {
            Timber.d(
                """
                    Timer stop
                    |- Was started: ${delayJob?.isActive ?: false}
                """.trimIndent(),
            )
        }
        delayJob = null
    }

    private fun createDelayJob(): Job = lifecycleScope.launch(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        delay(duration)
        if (isActive) {
            val userWalletsListManager = userWalletsListManagerSafe ?: return@launch
            if (userWalletsListManager.hasSavedUserWallets) {
                val currentTime = System.currentTimeMillis()
                Timber.d(
                    """
                        Finished
                        |- App is stopped: $isStopped
                        |- Millis passed: ${currentTime - startTime}
                    """.trimIndent(),
                )
                userWalletsListManager.lock()
                if (isStopped) {
                    openWelcomeScreenWhenResumed = true
                } else {
                    store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Welcome))
                }
            }
        }
    }
}