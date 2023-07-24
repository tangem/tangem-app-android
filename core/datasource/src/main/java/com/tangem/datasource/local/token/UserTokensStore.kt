package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import kotlinx.coroutines.flow.Flow

interface UserTokensStore {

    fun get(userWalletId: String): Flow<UserTokensResponse>

    suspend fun getSync(userWalletId: String): UserTokensResponse?

    suspend fun store(userWalletId: String, tokens: UserTokensResponse)
}