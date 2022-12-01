package com.tangem.tap.domain.userWalletList.repository

import com.tangem.common.CompletionResult
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey

internal interface UserWalletsKeysRepository {
    suspend fun getAll(): CompletionResult<List<UserWalletEncryptionKey>>
    suspend fun save(walletId: UserWalletId, encryptionKey: ByteArray): CompletionResult<List<UserWalletEncryptionKey>>
    suspend fun delete(walletIds: List<UserWalletId>): CompletionResult<List<UserWalletEncryptionKey>>
    suspend fun clear(): CompletionResult<Unit>
}