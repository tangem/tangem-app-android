package com.tangem.domain.yield.supply

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

interface YieldSupplyTransactionRepository {

    suspend fun createEnterTransactions(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        maxNetworkFee: BigDecimal,
    ): List<TransactionData.Uncompiled>

    suspend fun createExitTransaction(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled

    suspend fun getYieldContractAddress(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): String?

    suspend fun getEffectiveProtocolBalance(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): BigDecimal?
}