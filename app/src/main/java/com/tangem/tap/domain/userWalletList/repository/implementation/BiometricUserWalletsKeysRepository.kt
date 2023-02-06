package com.tangem.tap.domain.userWalletList.repository.implementation

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.CompletionResult
import com.tangem.common.biometric.BiometricManager
import com.tangem.common.biometric.BiometricStorage
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.fold
import com.tangem.common.map
import com.tangem.common.mapFailure
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.userWalletList.UserWalletListError
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.repository.UserWalletsKeysRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal class BiometricUserWalletsKeysRepository(
    moshi: Moshi,
    biometricManager: BiometricManager,
    private val secureStorage: SecureStorage,
) : UserWalletsKeysRepository {
    private val biometricStorage = BiometricStorage(
        biometricManager = biometricManager,
        secureStorage = secureStorage,
    )
    private val encryptionKeyAdapter: JsonAdapter<UserWalletEncryptionKey> = moshi.adapter(
        UserWalletEncryptionKey::class.java,
    )
    private val userWalletsIdsListAdapter: JsonAdapter<List<UserWalletId>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, UserWalletId::class.java),
    )

    override suspend fun getAll(): CompletionResult<List<UserWalletEncryptionKey>> {
        return withContext(Dispatchers.IO) {
            getAllInternal()
                .mapFailure { error ->
                    UserWalletListError.ReceiveEncryptionKeysError(error.cause ?: error)
                }
        }
    }

    override suspend fun save(encryptionKey: UserWalletEncryptionKey): CompletionResult<Unit> {
        return withContext(Dispatchers.IO) {
            storeEncryptionKey(encryptionKey)
                .mapFailure { error ->
                    UserWalletListError.SaveEncryptionKeysError(error.cause ?: error)
                }
        }
    }

    override suspend fun delete(userWalletsIds: List<UserWalletId>): CompletionResult<Unit> {
        return withContext(Dispatchers.IO) {
            userWalletsIds.map { userWalletId ->
                deleteEncryptionKey(userWalletId)
            }
                .fold()
                .map { deleteUserWalletsIds(userWalletsIds) }
        }
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return withContext(Dispatchers.IO) {
            getUserWalletsIds()
                .map { userWalletId ->
                    deleteEncryptionKey(userWalletId)
                }
                .fold()
                .map {
                    clearUserWalletsIds()
                }
        }
    }

    override fun hasSavedEncryptionKeys(): Boolean {
        return runBlocking {
            getUserWalletsIds().isNotEmpty()
        }
    }

    private suspend fun getAllInternal(): CompletionResult<List<UserWalletEncryptionKey>> {
        return getUserWalletsIds()
            .map { userWalletId ->
                // This is possible because the Card SDK cipher key has an expiration time
                // If this operation runs more than that expiration time, the user will not receive all encryption keys
                getEncryptionKey(userWalletId)
                    .doOnFailure { error ->
                        // If the user cancels biometric authentication, cancel the request for all keys
                        if (error is TangemSdkError.BiometricsAuthenticationFailed) {
                            return CompletionResult.Failure(error)
                        }
                    }
            }
            .fold(listOf()) { acc, data ->
                if (data != null) acc + data else acc
            }
    }

    private suspend fun getEncryptionKey(userWalletId: UserWalletId): CompletionResult<UserWalletEncryptionKey?> {
        return biometricStorage.get(StorageKey.WalletEncryptionKey(userWalletId).name)
            .map { it.decodeToKey() }
    }

    private suspend fun storeEncryptionKey(encryptionKey: UserWalletEncryptionKey): CompletionResult<Unit> {
        return biometricStorage.store(
            key = StorageKey.WalletEncryptionKey(encryptionKey.walletId).name,
            data = encryptionKey.encode(),
        )
            .map { storeUserWalletId(encryptionKey.walletId) }
    }

    private suspend fun deleteEncryptionKey(userWalletId: UserWalletId): CompletionResult<Unit> {
        return biometricStorage.delete(StorageKey.WalletEncryptionKey(userWalletId).name)
    }

    private suspend fun getUserWalletsIds(): List<UserWalletId> {
        return withContext(Dispatchers.IO) {
            secureStorage.get(StorageKey.UserWalletIds.name)
                .decodeToUserWalletsIds()
        }
    }

    private suspend fun storeUserWalletId(userWalletId: UserWalletId) {
        val userWalletIds = (getUserWalletsIds() + userWalletId).distinct()

        withContext(Dispatchers.IO) {
            secureStorage.store(userWalletIds.encode(), StorageKey.UserWalletIds.name)
        }
    }

    private suspend fun deleteUserWalletsIds(userWalletsIds: List<UserWalletId>) {
        val remainingIds = getUserWalletsIds() - userWalletsIds.toSet()

        withContext(Dispatchers.IO) {
            secureStorage.store(remainingIds.encode(), StorageKey.UserWalletIds.name)
        }
    }

    private suspend fun clearUserWalletsIds() {
        withContext(Dispatchers.IO) {
            secureStorage.delete(StorageKey.UserWalletIds.name)
        }
    }

    private suspend fun UserWalletEncryptionKey.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            this@encode
                .let(encryptionKeyAdapter::toJson)
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
    }

    private suspend fun ByteArray?.decodeToKey(): UserWalletEncryptionKey? {
        return withContext(Dispatchers.Default) {
            this@decodeToKey
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(encryptionKeyAdapter::fromJson)
        }
    }

    private suspend fun List<UserWalletId>.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            this@encode
                .let(userWalletsIdsListAdapter::toJson)
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
    }

    private suspend fun ByteArray?.decodeToUserWalletsIds(): List<UserWalletId> {
        return withContext(Dispatchers.Default) {
            this@decodeToUserWalletsIds
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(userWalletsIdsListAdapter::fromJson)
                .orEmpty()
        }
    }

    private sealed interface StorageKey {
        val name: String

        class WalletEncryptionKey(userWalletId: UserWalletId) : StorageKey {
            override val name: String = "user_wallet_encryption_key_${userWalletId.stringValue}"
        }

        object UserWalletIds : StorageKey {
            override val name: String = "user_wallets_ids_with_saved_keys"
        }
    }
}
