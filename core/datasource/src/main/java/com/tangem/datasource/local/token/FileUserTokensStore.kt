package com.tangem.datasource.local.token

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.files.FileReader
import com.tangem.datasource.local.datastore.FileDataStore
import kotlinx.coroutines.flow.Flow

internal class FileUserTokensStore(fileReader: FileReader, moshi: Moshi) : UserTokensStore {

    private val store = FileDataStore<UserTokensResponse>(
        fileNameProvider = { key -> "user_tokens_$key" },
        fileReader = fileReader,
        adapter = moshi.adapter(UserTokensResponse::class.java),
    )

    override fun get(userWalletId: String): Flow<UserTokensResponse> {
        return store.get(userWalletId)
    }

    override suspend fun getSyncOrNull(userWalletId: String): UserTokensResponse? {
        return store.getSyncOrNull(userWalletId)
    }

    override suspend fun store(userWalletId: String, tokens: UserTokensResponse) {
        store.store(userWalletId, tokens)
    }
}