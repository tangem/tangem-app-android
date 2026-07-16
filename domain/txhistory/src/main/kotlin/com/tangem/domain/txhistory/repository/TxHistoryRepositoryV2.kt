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

     * (the oldest loaded on-chain timestamp; `0` = no lower bound) to cap the working set; in-progress
     * operations are always included regardless of the bound. Re-emits live as the express DB is updated.
     */
    fun getExpressHistory(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        fromCreatedAtMillis: Long,
    ): Flow<List<ExpressTx>>
}