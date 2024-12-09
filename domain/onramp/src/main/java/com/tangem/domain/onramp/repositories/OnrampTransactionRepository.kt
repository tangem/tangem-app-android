package com.tangem.domain.onramp.repositories

import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface OnrampTransactionRepository {

    suspend fun storeTransaction(transaction: OnrampTransaction)

    suspend fun getTransactionById(externalTxId: String): OnrampTransaction?

    fun getTransactions(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): Flow<List<OnrampTransaction>>

    suspend fun updateTransactionStatus(externalTxId: String, status: OnrampStatus.Status)

    suspend fun removeTransaction(externalTxId: String)
}