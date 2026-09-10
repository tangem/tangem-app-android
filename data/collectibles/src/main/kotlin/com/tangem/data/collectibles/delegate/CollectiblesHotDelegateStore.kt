package com.tangem.data.collectibles.delegate

import com.tangem.domain.models.account.AccountId

internal interface CollectiblesHotDelegateStore {

    suspend fun get(accountId: AccountId): CollectiblesHotDelegateRecord?

    suspend fun put(accountId: AccountId, record: CollectiblesHotDelegateRecord)

    suspend fun remove(accountId: AccountId)
}