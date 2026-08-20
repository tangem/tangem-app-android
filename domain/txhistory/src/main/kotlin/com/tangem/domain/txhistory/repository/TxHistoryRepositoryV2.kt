package com.tangem.domain.txhistory.repository

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.TxHistoryListBatchFlow
import com.tangem.domain.txhistory.model.TxHistoryListBatchingContext
import kotlinx.coroutines.flow.Flow

interface TxHistoryRepositoryV2 {

    fun getTxHistoryBatchFlow(batchSize: Int, context: TxHistoryListBatchingContext): TxHistoryListBatchFlow

    /**
     * Reactive stream of express (swap & onramp) operations relevant to [currency] of wallet [userWalletId].
     *
     * Asset-scoped (owner address + network + contract) and windowed by [fromOnChainTimestampMillis] — the oldest
     * loaded on-chain timestamp, `0` = no lower bound — to cap the working set; in-progress operations are always
     * included regardless of the bound. Re-emits live as the express DB is updated.
     *

     * merge tolerates before filtering.
     */
    fun getExpressHistory(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        fromOnChainTimestampMillis: Long,
    ): Flow<List<ExpressTx>>

    /**
     * Reactive express history for [currency] paginated via the unified history index: the [limit] most recent index
     * rows define the window (their oldest sort time), which bounds [getExpressHistory]. Grow [limit] to load more.
     *
     * Used as the standalone backbone when there is no on-chain history source for the currency.
     */
    fun getIndexedExpressHistory(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        limit: Int,
    ): Flow<ExpressHistoryPage>

    /**
     * Reactive single express operation by its [txId], resolved from whichever of the swap/onramp tables holds
     * it — the two id spaces are disjoint, so at most one table ever matches. Used by the details sheet to bridge
     * a deal not yet present in [getExpressHistory]'s window (e.g. opened from a push deep link before pagination
     * catches up); emits nothing while the id is not found in either table.
     *
     * A swap's direction is resolved the same way [getExpressHistory] windows it: by matching [currency]'s
     * addresses (default + used dynamic ones) against the row's `from_address`.
     */
    fun getExpressTxById(userWalletId: UserWalletId, currency: CryptoCurrency, txId: String): Flow<ExpressTx>
}

data class ExpressHistoryPage(
    val items: List<ExpressTx>,
    val hasMore: Boolean,
)