package com.tangem.tap

import android.app.job.JobScheduler
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.domain.wallets.usecase.ClearAllHotWalletContextualUnlockUseCase
import com.tangem.tap.LockTimerWorker.Companion.TAG
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Suppress("LongParameterList")
@Singleton
internal class LockUserWalletsTimer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val clearAllHotWalletContextualUnlockUseCase: ClearAllHotWalletContextualUnlockUseCase,
    private val passwordRequester: HotWalletPasswordRequester,
    private val appRouter: AppRouter,
    appScope: AppCoroutineScope,
    dispatchers: CoroutineDispatcherProvider,
) : DefaultLifecycleObserver {

    // Rethrows like the former global main scope: a failure must not leave the wallets unlocked
    private val coroutineScope = appScope +
        dispatchers.main +
        FeatureCoroutineExceptionHandler.create(from = "lockUserWalletsTimer")

    private val delayJobHolder = JobHolder()

    private var attachedLifecycle: Lifecycle? = null

    /**

     * destroyed, so the previous lifecycle is detached to stop its events from affecting the timer.
     */
    fun attach(owner: LifecycleOwner) {
        attachedLifecycle?.removeObserver(this)
        attachedLifecycle = owner.lifecycle.apply { addObserver(this@LockUserWalletsTimer) }
    }

    override fun onStart(owner: LifecycleOwner) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        coroutineScope.launch {
            val shouldOpenWelcomeScreenOnResume = settingsRepository.shouldOpenWelcomeScreenOnResume()
            TangemLogger.i(
                """
                Owner resumed
                |- Need to open welcome screen: $shouldOpenWelcomeScreenOnResume
                """.trimIndent(),
            )

            if (shouldOpenWelcomeScreenOnResume) {
                resetToLockScreen()
                settingsRepository.setShouldOpenWelcomeScreenOnResume(value = false)
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        start()
    }

    override fun onStop(owner: LifecycleOwner) {
        TangemLogger.i("Owner stopped")
        delayJobHolder.cancel()

        startTimerWorker()
    }

    fun restart() {
        if (delayJobHolder.isEmpty()) return
        TangemLogger.i(
            """
                Timer restart
                |- Duration millis: ${DURATION.inWholeMilliseconds}
            """.trimIndent(),
        )
        start(log = false)
    }

    private fun startTimerWorker() {
        val lockWorkRequest = OneTimeWorkRequest.Builder(LockTimerWorker::class.java)
            .addTag(TAG)
            .setInitialDelay(DURATION.inWholeSeconds, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(lockWorkRequest)
    }

    private fun start(log: Boolean = true) {
        if (log) {
            TangemLogger.i(
                """
                    Timer start
                    |- Duration millis: ${DURATION.inWholeMilliseconds}
                """.trimIndent(),
            )
        }
        createDelayJob().saveIn(delayJobHolder)
    }

    /**
     * This job used only when app is foreground when background use [JobScheduler]
     */
    private fun createDelayJob(): Job = coroutineScope.launch {
        delay(DURATION)

        val userWallets = userWalletsListRepository.userWalletsSync()
        if (userWallets.isNotEmpty()) {
            userWalletsListRepository.lockAllWallets()
                .onLeft {
                    start()
                }
                .onRight {
                    resetToLockScreen()
                }
        }
    }

    private suspend fun resetToLockScreen() {
        passwordRequester.dismiss()
        clearAllHotWalletContextualUnlockUseCase.invoke()
        appRouter.replaceAll(AppRoute.Welcome())
    }

    private companion object {
        val DURATION = 5.minutes
    }
}