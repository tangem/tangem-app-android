package com.tangem.tap.domain.userWalletList.repository.implementation

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.*
import com.tangem.common.biometric.BiometricManager
import com.tangem.common.biometric.BiometricStorage
import com.tangem.common.core.TangemSdkError
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.domain.wallets.models.UserWalletId
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
                    when (error) {
                        is TangemSdkError.BiometricsAuthenticationLockout ->
                            UserWalletsListError.BiometricsAuthenticationLockout(isPermanent = false)
                        is TangemSdkError.BiometricsAuthenticationPermanentLockout ->
                            UserWalletsListError.BiometricsAuthenticationLockout(isPermanent = true)
                        is TangemSdkError.BiometricCryptographyKeyInvalidated ->
                            UserWalletsListError.EncryptionKeyInvalidated
                        is TangemSdkError.BiometricsUnavailable ->
                            UserWalletsListError.BiometricsAuthenticationDisabled
                        else -> error
                    }
                }
        }
    }

    override suspend fun save(encryptionKey: UserWalletEncryptionKey): CompletionResult<Unit> {
        return withContext(Dispatchers.IO) {
            storeEncryptionKey(encryptionKey)
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
                // It is possible to request multiple user wallet keys from biometric storage because
                // the biometric cryptography key has an expiration time.
                // If this operation runs more than that expiration time, then the user will have to re-authorize
                // to receive all user wallets encryption keys
                getEncryptionKey(userWalletId)
                    .flatMapOnFailure { error ->
                        when (error) {
                            is TangemSdkError.InvalidBiometricCryptographyKey,
                            is TangemSdkError.BiometricCryptographyOperationFailed,
                            -> {
                                // These errors can be skipped as the user has the option to re-save their wallets
                                // in case they occur
                                CompletionResult.Success(data = null)
                            }
                            else -> CompletionResult.Failure(error)
                        }
                    }
                    .doOnFailure { error ->
                        when (error) {
                            is TangemSdkError.UserCanceledBiometricsAuthentication -> {
                                // If the user cancels biometric authentication, then cancel operation with error
                                return CompletionResult.Failure(error)
                            }
                            is TangemSdkError.BiometricCryptographyKeyInvalidated -> {
                                // If the biometric cryptography key was invalidated,
                                // then delete all user wallets encryption keys and cancel operation with error
                                getUserWalletsIds().forEach { userWalletId ->
                                    deleteEncryptionKey(userWalletId)
                                }
                                return CompletionResult.Failure(error)
                            }
                        }
                    }
            }
            .fold(listOf()) { acc, data ->
                if (data != null) acc + data else acc
            }
    }

    private suspend fun getEncryptionKey(userWalletId: UserWalletId): CompletionResult<UserWalletEncryptionKey?> {
        return biometricStorage.get(StorageKey.UserWalletEncryptionKey(userWalletId).name)
            .map { it.decodeToKey() }
    }

    private suspend fun storeEncryptionKey(encryptionKey: UserWalletEncryptionKey): CompletionResult<Unit> {
        return biometricStorage.store(
            key = StorageKey.UserWalletEncryptionKey(encryptionKey.walletId).name,
            data = encryptionKey.encode(),
        )
            .map { storeUserWalletId(encryptionKey.walletId) }
    }

    private suspend fun deleteEncryptionKey(userWalletId: UserWalletId): CompletionResult<Unit> {
        return biometricStorage.delete(StorageKey.UserWalletEncryptionKey(userWalletId).name)
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

        class UserWalletEncryptionKey(userWalletId: UserWalletId) : StorageKey {
            override val name: String = "user_wallet_encryption_key_${userWalletId.stringValue}"
        }

        object UserWalletIds : StorageKey {
            override val name: String = "user_wallets_ids_with_saved_keys"
        }
    }
}
