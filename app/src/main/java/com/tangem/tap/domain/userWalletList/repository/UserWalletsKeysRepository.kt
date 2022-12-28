package com.tangem.tap.domain.userWalletList.repository

import com.tangem.common.CompletionResult
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey

internal interface UserWalletsKeysRepository {
    /**
     * Obtaining the encryption keys of all user wallets from the biometric vault. Biometric authentication required
     * If that operation runs more than biometric cipher key expiration time then the user will not receive all
     * encryption keys
     * @return [CompletionResult] of operation with stored [UserWalletEncryptionKey] list
     * */
    suspend fun getAll(): CompletionResult<List<UserWalletEncryptionKey>>

    /**
     * Store the encryption keys for user wallets. Biometric authentication not required
     * @param encryptionKeys List of encryption keys for user wallets
     * @return [CompletionResult] of operation
     * */
    suspend fun store(encryptionKeys: List<UserWalletEncryptionKey>): CompletionResult<Unit>

    /**
     * Delete encryption keys for user wallets. Biometric authentication not required
     * @param userWalletsIds List of [UserWalletId] whose encryption keys will be deleted
     * @return [CompletionResult] of operation
     * */
    suspend fun delete(userWalletsIds: List<UserWalletId>): CompletionResult<Unit>

    /**
     * Clear all encryption keys for user wallets. Biometric authentication not required
     * @return [CompletionResult] of operation
     * */
    suspend fun clear(): CompletionResult<Unit>

    /**
     * Determine if the user has saved user wallets
     * @return [Boolean] true if user has saved wallets
     * */
    fun hasSavedEncryptionKeys(): Boolean
}
