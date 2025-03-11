package com.tangem.tap

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tangem.common.routing.AppRoute
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.tap.common.extensions.dispatchNavigationAction
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.time.Duration

internal class LockUserWalletsTimer(
    owner: LifecycleOwner,
    private val settingsRepository: SettingsRepository,
    private val duration: Duration = with(Duration) { 10.minutes },
    private val userWalletsListManager: UserWalletsListManager,
    private val coroutineScope: CoroutineScope,
) : LifecycleOwner by owner,
    DefaultLifecycleObserver {

    private var delayJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        coroutineScope.launch {
            val wasApplicationStopped = settingsRepository.wasApplicationStopped()
            val shouldOpenWelcomeScreenOnResume = settingsRepository.shouldOpenWelcomeScreenOnResume()

            Timber.i(
                """
                Owner resumed
                |- Was stopped: $wasApplicationStopped
                |- Need to open welcome screen: $shouldOpenWelcomeScreenOnResume
                """.trimIndent(),
            )

            settingsRepository.setWasApplicationStopped(value = false)

            if (shouldOpenWelcomeScreenOnResume) {
                store.dispatchNavigationAction { replaceAll(AppRoute.Welcome()) }
                settingsRepository.setShouldOpenWelcomeScreenOnResume(value = false)
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        start()
    }

    override fun onStop(owner: LifecycleOwner) {
        Timber.i("Owner stopped")

        coroutineScope.launch {
            settingsRepository.setWasApplicationStopped(value = true)
        }
    }

    fun restart() {
        if (delayJob == null) return
        Timber.i(
            """
                Timer restart
                |- Duration millis: ${duration.inWholeMilliseconds}
            """.trimIndent(),
        )
        start(log = false)
    }

    private fun start(log: Boolean = true) {
        if (log) {
            Timber.i(
                """
                    Timer start
                    |- Duration millis: ${duration.inWholeMilliseconds}
                """.trimIndent(),
            )
        }
        delayJob = createDelayJob()
    }

    private fun createDelayJob(): Job = coroutineScope.launch {
        val startTime = System.currentTimeMillis()

        delay(duration)

        val userWalletsListManager = userWalletsListManager.asLockable() ?: return@launch

        if (userWalletsListManager.hasUserWallets) {
            val currentTime = System.currentTimeMillis()
            val wasApplicationStopped = settingsRepository.wasApplicationStopped()

            Timber.i(
                """
                        Finished
                        |- App is stopped: $wasApplicationStopped
                        |- Millis passed: ${currentTime - startTime}
                """.trimIndent(),
            )

            userWalletsListManager.lock()
            if (wasApplicationStopped) {
                settingsRepository.setShouldOpenWelcomeScreenOnResume(value = true)
            } else {
                store.dispatchNavigationAction { replaceAll(AppRoute.Welcome()) }
            }
        }
    }
}