package com.tangem.domain.transaction

import com.tangem.domain.transaction.models.tron.TronGaslessEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.domain.transaction.models.tron.TronGaslessSubmitResult
import com.tangem.domain.transaction.models.tron.TronGaslessToken

interface TronGaslessTransactionRepository {
    suspend fun getSupportedTokens(): List<TronGaslessToken>
    suspend fun estimate(params: TronGaslessEstimateParams): TronGaslessQuote
    suspend fun submit(quoteId: String, signedCompensationTx: String, signedOriginalTx: String): TronGaslessSubmitResult
}