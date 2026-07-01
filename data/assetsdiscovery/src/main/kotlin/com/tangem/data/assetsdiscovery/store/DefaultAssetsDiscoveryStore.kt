package com.tangem.data.assetsdiscovery.store

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import kotlinx.coroutines.flow.firstOrNull

internal class DefaultAssetsDiscoveryStore(
    private val persistenceStore: DataStore<List<UserTokensResponse.Token>>,
) : AssetsDiscoveryStore {

    override suspend fun get(): List<UserTokensResponse.Token> {
        return persistenceStore.data.firstOrNull().orEmpty()
    }

    override suspend fun append(tokens: List<UserTokensResponse.Token>) {
        persistenceStore.updateData { existing ->
            (existing + tokens).distinct()
        }
    }

    override suspend fun removeMatching(predicate: (UserTokensResponse.Token) -> Boolean) {
        persistenceStore.updateData { existing ->
            existing.filterNot(predicate)
        }
    }

    override suspend fun clear() {
        persistenceStore.updateData { emptyList() }
    }
}