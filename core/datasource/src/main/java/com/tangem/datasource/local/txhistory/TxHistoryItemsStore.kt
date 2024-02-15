package com.tangem.datasource.local.txhistory

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.wallets.models.UserWalletId

interface TxHistoryItemsStore {

    suspend fun getSyncOrNull(key: Key, page: Page): PaginationWrapper<TxHistoryItem>?

    suspend fun remove(key: Key)

    suspend fun store(key: Key, value: PaginationWrapper<TxHistoryItem>)

    data class Key(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
    )
}