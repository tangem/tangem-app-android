package com.tangem.feature.swap.domain

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import kotlinx.coroutines.flow.Flow

interface SwapTransactionRepository {

    @Suppress("LongParameterList")
    suspend fun storeTransaction(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        fromAccount: Account?,
        toAccount: Account?,
        transaction: SavedSwapTransactionModel,
    )

    fun getTransactions(
        userWallet: UserWallet,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): Flow<List<SavedSwapTransactionListModel>?>

    suspend fun removeTransaction(userWalletId: UserWalletId, txId: String)

    suspend fun storeTransactionState(
        txId: String,
        status: ExchangeStatusModel,
        accountWithCurrency: Pair<AccountId?, CryptoCurrency>? = null,
    )

    suspend fun storeLastSwappedCryptoCurrencyId(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID)

    suspend fun getLastSwappedCryptoCurrencyId(userWalletId: UserWalletId): String?
}