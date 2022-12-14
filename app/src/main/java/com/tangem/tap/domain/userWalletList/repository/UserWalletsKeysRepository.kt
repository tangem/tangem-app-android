package com.tangem.tap.domain.userWalletList.repository

import com.tangem.common.CompletionResult
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey

internal interface UserWalletsKeysRepository {
    suspend fun getAll(): CompletionResult<List<UserWalletEncryptionKey>>
    suspend fun store(encryptionKeys: List<UserWalletEncryptionKey>): CompletionResult<Unit>
    suspend fun clear(): CompletionResult<Unit>
}
