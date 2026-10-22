package com.tangem.data.wallets.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tangem.core.local.connection.NetworkConnectionManager
import com.tangem.domain.wallets.backup.WalletCardsBackupDeliveryScheduler
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

internal class DefaultWalletCardsBackupDeliveryScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectionManager: NetworkConnectionManager,
    private val appScope: AppCoroutineScope,
) : WalletCardsBackupDeliveryScheduler {

    private val isObservingConnection = AtomicBoolean(false)

    override fun schedule() {
        enqueue(requiredNetworkType = NetworkType.CONNECTED)
        TangemLogger.i("Pending cards backup delivery scheduled")

        observeConnection()
    }

    private fun observeConnection() {
        if (isObservingConnection.getAndSet(true)) return

        appScope.launch {
            networkConnectionManager.isOnline
                .filter { isOnline -> isOnline }
                .collect {
                    enqueue(requiredNetworkType = NetworkType.NOT_REQUIRED)
                    TangemLogger.i("Connection is back, pending cards backup delivery started")
                }
        }
    }

    private fun enqueue(requiredNetworkType: NetworkType) {
        val request = OneTimeWorkRequestBuilder<WalletCardsBackupDeliveryWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(requiredNetworkType).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "wallet_cards_backup_delivery"
        const val BACKOFF_DELAY_SECONDS = 30L
    }
}