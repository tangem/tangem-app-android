package com.tangem.data.blockaid

import arrow.core.Either
import com.domain.blockaid.models.transaction.GasEstimationResult
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.blockaid.BlockAidGasEstimate
import com.tangem.domain.models.currency.CryptoCurrency
import javax.inject.Inject

class DefaultBlockAidGasEstimate @Inject constructor(
    private val repository: BlockAidRepository,
) : BlockAidGasEstimate {
    override suspend fun getGasEstimation(
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<TransactionData.Uncompiled>,
    ): Either<Throwable, GasEstimationResult> = Either.catch {
        repository.getGasEstimation(cryptoCurrency = cryptoCurrency, transactionDataList = transactionDataList)
    }
}