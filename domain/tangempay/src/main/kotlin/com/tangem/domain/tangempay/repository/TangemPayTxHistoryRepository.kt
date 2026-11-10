package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchFlow
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchingContext
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.TangemPayTxHistoryItem

interface TangemPayTxHistoryRepository {
    fun getTxHistoryBatchFlow(
        userWalletId: UserWalletId,
        batchSize: Int,
        context: TangemPayTxHistoryListBatchingContext,
    ): TangemPayTxHistoryListBatchFlow

    /** Loads a single transaction by its id. */
    suspend fun getTransaction(
        userWalletId: UserWalletId,
        transactionId: String,
    ): Either<TangemPayApiError, TangemPayTxHistoryItem?>
}