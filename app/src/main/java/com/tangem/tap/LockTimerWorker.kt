package com.tangem.tap

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class LockTimerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.i("onStart job")
        userWalletsListRepository.lockAllWallets().onRight {
            settingsRepository.setShouldOpenWelcomeScreenOnResume(value = true)
        }
        Timber.i("onStart job complete")
        return Result.success()
    }

    companion object {
        const val TAG = "LOCK_TIMER"
    }
}