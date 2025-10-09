package com.tangem.domain.blockaid

import arrow.core.Either
import com.domain.blockaid.models.transaction.GasEstimationResult
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.currency.CryptoCurrency

interface BlockAidGasEstimate {

    suspend fun getGasEstimation(
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<TransactionData.Uncompiled>,
    ): Either<Throwable, GasEstimationResult>
}