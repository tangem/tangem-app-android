package com.tangem.datasource.local.visa

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.TangemPayTxHistoryItem

interface TangemPayTxHistoryItemsStore {

    suspend fun getSyncOrNull(key: UserWalletId, cursor: String): List<TangemPayTxHistoryItem>?

    suspend fun remove(key: UserWalletId)

    suspend fun store(key: UserWalletId, cursor: String, value: List<TangemPayTxHistoryItem>)
}