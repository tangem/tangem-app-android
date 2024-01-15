package com.tangem.feature.swap.domain

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import kotlinx.coroutines.flow.Flow

interface SwapTransactionRepository {

    suspend fun storeTransaction(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        transaction: SavedSwapTransactionModel,
    )

    suspend fun getTransactions(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
        scanResponse: ScanResponse,
    ): Flow<List<SavedSwapTransactionListModel>?>

    suspend fun removeTransaction(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        txId: String,
    )

    suspend fun storeTransactionState(txId: String, status: ExchangeStatusModel)

    suspend fun storeLastSwappedCryptoCurrencyId(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID)

    suspend fun getLastSwappedCryptoCurrencyId(userWalletId: UserWalletId): String?
}
