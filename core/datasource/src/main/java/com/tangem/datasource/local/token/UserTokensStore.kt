package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface UserTokensStore {

    fun get(userWalletId: UserWalletId): Flow<UserTokensResponse>

    suspend fun getSyncOrNull(userWalletId: UserWalletId): UserTokensResponse?

    suspend fun store(userWalletId: UserWalletId, tokens: UserTokensResponse)
}
