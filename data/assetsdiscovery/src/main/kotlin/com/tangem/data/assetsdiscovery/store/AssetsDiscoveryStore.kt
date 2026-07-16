package com.tangem.data.assetsdiscovery.store

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse

interface AssetsDiscoveryStore {

    suspend fun get(): List<UserTokensResponse.Token>

    suspend fun append(tokens: List<UserTokensResponse.Token>)

    suspend fun removeMatching(predicate: (UserTokensResponse.Token) -> Boolean)

    suspend fun clear()
}