package com.tangem.data.tokensync.store

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import kotlinx.coroutines.flow.firstOrNull

internal class DefaultTokenSyncStore(
    private val persistenceStore: DataStore<List<UserTokensResponse.Token>>,
) : TokenSyncStore {

    override suspend fun get(): List<UserTokensResponse.Token> {
        return persistenceStore.data.firstOrNull().orEmpty()
    }

    override suspend fun append(tokens: List<UserTokensResponse.Token>) {
        persistenceStore.updateData { existing ->
            (existing + tokens).distinct()
        }
    }

    override suspend fun clear() {
        persistenceStore.updateData { emptyList() }
    }
}