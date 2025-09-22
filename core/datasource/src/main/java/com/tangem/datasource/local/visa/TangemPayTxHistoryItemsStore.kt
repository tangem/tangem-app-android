package com.tangem.datasource.local.visa

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.VisaTxHistoryItem

interface TangemPayTxHistoryItemsStore {

    suspend fun getSyncOrNull(key: UserWalletId, offset: Int): List<VisaTxHistoryItem>?

    suspend fun remove(key: UserWalletId)

    suspend fun store(key: UserWalletId, offset: Int, value: List<VisaTxHistoryItem>)
}