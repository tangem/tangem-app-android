package com.tangem.data.blockaid

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.DAppData
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.GasEstimationResult
import com.domain.blockaid.models.transaction.TransactionData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.blockchain.common.TransactionData as SDKTransactionData

interface BlockAidRepository {

    suspend fun verifyDAppDomain(data: DAppData): CheckDAppResult

    suspend fun verifyTransaction(data: TransactionData): CheckTransactionResult

    suspend fun getGasEstimation(
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<SDKTransactionData.Uncompiled>,
    ): GasEstimationResult
}