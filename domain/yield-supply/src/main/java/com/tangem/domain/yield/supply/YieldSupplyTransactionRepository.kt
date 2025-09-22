package com.tangem.domain.yield.supply

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus

interface YieldSupplyTransactionRepository {

    suspend fun createEnterTransactions(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): List<TransactionData.Uncompiled>

    suspend fun createExitTransaction(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldSupplyStatus: YieldSupplyStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled
}