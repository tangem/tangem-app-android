package com.tangem.domain.transaction

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldlending.providers.ethereum.EthereumLendingStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yieldlending.YieldLendingStatus

interface YieldLendingTransactionRepository {

    suspend fun getYieldContractAddress(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): String

    suspend fun getYieldTokenStatus(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): EthereumLendingStatus?

    suspend fun createDeployTransaction(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldLendingStatus: YieldLendingStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled

    suspend fun createInitTokenTransaction(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldContractAddress: String,
        yieldLendingStatus: YieldLendingStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled

    suspend fun createEnterTransaction(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldLendingStatus: YieldLendingStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled

    suspend fun createExitTransaction(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldLendingStatus: YieldLendingStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled
}