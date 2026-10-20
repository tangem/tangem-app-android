package com.tangem.data.wallets.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tangem.domain.wallets.backup.WalletCardsBackupDeliveryScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class DefaultWalletCardsBackupDeliveryScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : WalletCardsBackupDeliveryScheduler {

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<WalletCardsBackupDeliveryWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "wallet_cards_backup_delivery"
        const val BACKOFF_DELAY_SECONDS = 30L
    }
}