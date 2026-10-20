package com.tangem.data.wallets.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tangem.domain.wallets.usecase.SendPendingWalletCardsBackupUseCase
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
internal class WalletCardsBackupDeliveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sendPendingWalletCardsBackupUseCase: SendPendingWalletCardsBackupUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = sendPendingWalletCardsBackupUseCase().fold(
        ifLeft = { error ->
            TangemLogger.e("Pending cards backup reports deferred, attempt $runAttemptCount: $error")
            Result.retry()
        },
        ifRight = { Result.success() },
    )
}