package com.tangem.tap

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LockTimerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        TangemLogger.i("onStart job")
        userWalletsListRepository.lockAllWallets().onRight {
            settingsRepository.setShouldOpenWelcomeScreenOnResume(value = true)
        }
        TangemLogger.i("onStart job complete")
        return Result.success()
    }

    companion object {
        const val TAG = "LOCK_TIMER"
    }
}