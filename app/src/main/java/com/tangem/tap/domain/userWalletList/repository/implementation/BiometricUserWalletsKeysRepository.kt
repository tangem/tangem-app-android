package com.tangem.tap.domain.userWalletList.repository.implementation

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.CompletionResult
import com.tangem.common.biometric.BiometricManager
import com.tangem.common.biometric.BiometricStorage
import com.tangem.common.flatMap
import com.tangem.common.map
import com.tangem.common.mapFailure
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.extensions.replaceByOrAdd
import com.tangem.tap.domain.userWalletList.UserWalletListError
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.repository.UserWalletsKeysRepository

internal class BiometricUserWalletsKeysRepository(
    biometricKeyName: String,
    moshi: Moshi,
    secureStorage: SecureStorage,
    biometricManager: BiometricManager,
) : UserWalletsKeysRepository {
    private val biometricStorage = BiometricStorage(
        biometricKeyName = biometricKeyName,
        biometricManager = biometricManager,
        secureStorage = secureStorage,
    )
    private val walletsKeysAdapter: JsonAdapter<List<UserWalletEncryptionKey>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, UserWalletEncryptionKey::class.java),
    )

    override suspend fun getAll(): CompletionResult<List<UserWalletEncryptionKey>> {
        return biometricStorage.get(key = StorageKey.WalletEncryptionKeys.name)
            .map { encryptionKeys ->
                encryptionKeys.decodeToKeys()
            }
            .mapFailure { error ->
                UserWalletListError.ReceiveEncryptionKeysError(error.cause ?: error)
            }
    }

    override suspend fun save(
        walletId: UserWalletId,
        encryptionKey: ByteArray,
    ): CompletionResult<List<UserWalletEncryptionKey>> {
        return getAll()
            .flatMap { keys ->
                if (keys.any { it.walletId == walletId }) {
                    return@flatMap CompletionResult.Success(Unit)
                }

                val encodedKeys = keys.toMutableList()
                    .apply {
                        replaceByOrAdd(UserWalletEncryptionKey(walletId, encryptionKey)) {
                            it.walletId == walletId
                        }
                    }
                    .encode()

                biometricStorage.store(
                    key = StorageKey.WalletEncryptionKeys.name,
                    data = encodedKeys,
                )
            }
            .flatMap { getAll() }
            .mapFailure { error ->
                UserWalletListError.SaveEncryptionKeysError(error.cause ?: error)
            }
    }

    override suspend fun delete(walletIds: List<UserWalletId>): CompletionResult<List<UserWalletEncryptionKey>> {
        return getAll()
            .map { keys ->
                val keysToRemove = keys.filter { it.walletId in walletIds }.toSet()
                (keys - keysToRemove).encode()
            }
            .flatMap { encodedKeys ->
                biometricStorage.store(
                    key = StorageKey.WalletEncryptionKeys.name,
                    data = encodedKeys,
                )
            }
            .flatMap { getAll() }
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return biometricStorage.delete(key = StorageKey.WalletEncryptionKeys.name)
    }

    private fun List<UserWalletEncryptionKey>.encode(): ByteArray {
        return this.let(walletsKeysAdapter::toJson)
            .encodeToByteArray(throwOnInvalidSequence = true)
    }

    private fun ByteArray?.decodeToKeys(): List<UserWalletEncryptionKey> {
        return this?.decodeToString(throwOnInvalidSequence = true)
            ?.let(walletsKeysAdapter::fromJson)
            .orEmpty()
    }

    private enum class StorageKey {
        WalletEncryptionKeys
    }
}