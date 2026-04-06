package com.tangem.data.tokensync.store

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse

interface TokenSyncStore {

    suspend fun get(): List<UserTokensResponse.Token>

    suspend fun append(tokens: List<UserTokensResponse.Token>)

    suspend fun clear()
}