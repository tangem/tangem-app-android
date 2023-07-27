package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface UserTokensStore {

    fun get(key: UserWalletId): Flow<UserTokensResponse>

    suspend fun getSyncOrNull(key: UserWalletId): UserTokensResponse?

    suspend fun store(key: UserWalletId, item: UserTokensResponse)
}
