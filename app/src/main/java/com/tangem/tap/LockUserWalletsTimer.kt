package com.tangem.tap

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.utils.popTo
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

    override fun onResume(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            val wasApplicationStopped = settingsRepository.wasApplicationStopped()
            val shouldOpenWelcomeScreenOnResume = settingsRepository.shouldOpenWelcomeScreenOnResume()

            Timber.d(
                """
                Owner resumed
                |- Was stopped: $wasApplicationStopped
                |- Need to open welcome screen: $shouldOpenWelcomeScreenOnResume
                """.trimIndent(),
            )

            settingsRepository.setWasApplicationStopped(value = false)

            start()

            if (shouldOpenWelcomeScreenOnResume) {
                store.dispatchNavigationAction { popTo<AppRoute.Welcome>() }
                settingsRepository.setShouldOpenWelcomeScreenOnResume(value = false)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        Timber.d("Owner stopped")

        owner.lifecycleScope.launch {
            settingsRepository.setWasApplicationStopped(value = true)
        }
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
            val userWalletsListManager = userWalletsListManager.asLockable() ?: return@launch

            if (userWalletsListManager.hasUserWallets) {
                val currentTime = System.currentTimeMillis()
                val wasApplicationStopped = settingsRepository.wasApplicationStopped()

                Timber.d(
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
                    store.dispatchNavigationAction { popTo<AppRoute.Welcome>() }
                }
            }
        }
    }
}
