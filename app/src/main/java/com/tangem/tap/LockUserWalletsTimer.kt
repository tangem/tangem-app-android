package com.tangem.tap

import android.app.job.JobScheduler
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.tangem.common.routing.AppRoute
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.wallets.usecase.ClearAllHotWalletContextualUnlockUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.tap.LockTimerWorker.Companion.TAG
import com.tangem.tap.common.extensions.dispatchNavigationAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

@Suppress("LongParameterList")
internal class LockUserWalletsTimer(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val duration: Duration = with(Duration) { 5.minutes },
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val coroutineScope: CoroutineScope,
    private val clearAllHotWalletContextualUnlockUseCase: ClearAllHotWalletContextualUnlockUseCase,
) : LifecycleOwner by context as LifecycleOwner,
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
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        coroutineScope.launch {
            val shouldOpenWelcomeScreenOnResume = settingsRepository.shouldOpenWelcomeScreenOnResume()
            Timber.i(
                """
                Owner resumed
                |- Need to open welcome screen: $shouldOpenWelcomeScreenOnResume
                """.trimIndent(),
            )

            if (shouldOpenWelcomeScreenOnResume) {
                if (hotWalletFeatureToggles.isHotWalletEnabled) {
                    clearAllHotWalletContextualUnlockUseCase.invoke()
                }
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
        delayJob = null

        startTimerWorker()
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

    private fun startTimerWorker() {
        val lockWorkRequest = OneTimeWorkRequest.Builder(LockTimerWorker::class.java)
            .addTag(TAG)
            .setInitialDelay(duration.inWholeSeconds, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(lockWorkRequest)
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

    /**
     * This job used only when app is foreground when background use [JobScheduler]
     */
    private fun createDelayJob(): Job = coroutineScope.launch {
        val startTime = System.currentTimeMillis()

        delay(duration)

        if (hotWalletFeatureToggles.isHotWalletEnabled) {
            val userWallets = userWalletsListRepository.userWalletsSync()
            if (userWallets.isNotEmpty()) {
                userWalletsListRepository.lockAllWallets()
                    .onLeft {
                        start()
                    }
                    .onRight {
                        clearAllHotWalletContextualUnlockUseCase.invoke()
                        store.dispatchNavigationAction { replaceAll(AppRoute.Welcome()) }
                    }
            }
        } else {
            val userWalletsListManager = userWalletsListManager.asLockable() ?: return@launch

            if (userWalletsListManager.hasUserWallets) {
                val currentTime = System.currentTimeMillis()

                Timber.i(
                    """
                        Finished
                        |- Millis passed: ${currentTime - startTime}
                    """.trimIndent(),
                )

                userWalletsListManager.lock()
                store.dispatchNavigationAction { replaceAll(AppRoute.Welcome()) }
            }
        }
    }
}