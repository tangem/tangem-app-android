package com.tangem.datasource.local.token

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.files.FileReader
import com.tangem.datasource.local.datastore.FileDataStore
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

internal class FileUserTokensStore(fileReader: FileReader, moshi: Moshi) : UserTokensStore {

    private val store = FileDataStore<UserTokensResponse>(
        fileNameProvider = { key -> "user_tokens_$key" },
        fileReader = fileReader,
        adapter = moshi.adapter(UserTokensResponse::class.java),
    )

    override fun get(userWalletId: UserWalletId): Flow<UserTokensResponse> {
        return store.get(userWalletId.stringValue)
    }

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): UserTokensResponse? {
        return store.getSyncOrNull(userWalletId.stringValue)
    }

    override suspend fun store(userWalletId: UserWalletId, tokens: UserTokensResponse) {
        store.store(userWalletId.stringValue, tokens)
    }
}
