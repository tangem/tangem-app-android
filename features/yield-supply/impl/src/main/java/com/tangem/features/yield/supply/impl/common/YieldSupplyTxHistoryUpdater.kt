package com.tangem.features.yield.supply.impl.common

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The token history picks up a pending transaction only when it reloads, and leaving the Yield Mode screen does not
 * reload it. So a sent enter or exit reloads it right away, to show the pending row, and again once the transactions
 * are no longer pending, so the row stops reading as in progress. The pending copy is dropped then, while the
 * explorer may still lag behind, so one more reload follows to bring in the confirmed row.
 * Runs on the app scope: the sheet that sent the transactions is closed straight after.
 */
internal class YieldSupplyTxHistoryUpdater @Inject constructor(
    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter,
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val appCoroutineScope: AppCoroutineScope,
) {

    fun scheduleUpdates(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency, txIds: List<String>) {
        appCoroutineScope.launch {
            txHistoryContentUpdateEmitter.triggerUpdate()
            awaitNotPending(userWalletId, cryptoCurrency, txIds)
            txHistoryContentUpdateEmitter.triggerUpdate()
            delay(EXPLORER_LAG_MS)
            txHistoryContentUpdateEmitter.triggerUpdate()
        }
    }

    private suspend fun awaitNotPending(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        txIds: List<String>,
    ) {
        repeat(MAX_CHECKS) {
            delay(CHECK_INTERVAL_MS)
            val pendingTxHashes = yieldSupplyRepository.getPendingTxHashes(userWalletId, cryptoCurrency).toSet()
            if (txIds.none { txId -> txId in pendingTxHashes }) return
        }
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 3_000L
        const val MAX_CHECKS = 40
        const val EXPLORER_LAG_MS = 10_000L
    }
}