package com.tangem.tap.domain.userWalletList.repository.implementation

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.CompletionResult
import com.tangem.common.biometric.BiometricManager
import com.tangem.common.biometric.BiometricStorage
import com.tangem.common.map
import com.tangem.common.mapFailure
import com.tangem.common.services.secure.SecureStorage
import com.tangem.tap.domain.userWalletList.UserWalletListError
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.repository.UserWalletsKeysRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class BiometricUserWalletsKeysRepository(
    moshi: Moshi,
    secureStorage: SecureStorage,
    biometricManager: BiometricManager,
) : UserWalletsKeysRepository {
    private val biometricStorage = BiometricStorage(
        biometricManager = biometricManager,
        secureStorage = secureStorage,
    )
    private val walletsKeysAdapter: JsonAdapter<List<UserWalletEncryptionKey>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, UserWalletEncryptionKey::class.java),
    )

    override suspend fun getAll(): CompletionResult<List<UserWalletEncryptionKey>> {
        return withContext(Dispatchers.IO) {
            biometricStorage.get(key = StorageKey.WalletEncryptionKeys.name)
                .map { encryptionKeys ->
                    encryptionKeys.decodeToKeys()
                }
                .mapFailure { error ->
                    UserWalletListError.ReceiveEncryptionKeysError(error.cause ?: error)
                }
        }
    }

    override suspend fun store(encryptionKeys: List<UserWalletEncryptionKey>): CompletionResult<Unit> {
        return withContext(Dispatchers.IO) {
            biometricStorage.store(
                key = StorageKey.WalletEncryptionKeys.name,
                data = encryptionKeys.encode(),
            )
                .mapFailure { error ->
                    UserWalletListError.SaveEncryptionKeysError(error.cause ?: error)
                }
        }
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return withContext(Dispatchers.IO) {
            biometricStorage.delete(key = StorageKey.WalletEncryptionKeys.name)
        }
    }

    private suspend fun List<UserWalletEncryptionKey>.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            this@encode
                .let(walletsKeysAdapter::toJson)
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
    }

    private suspend fun ByteArray?.decodeToKeys(): List<UserWalletEncryptionKey> {
        return withContext(Dispatchers.Default) {
            this@decodeToKeys
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(walletsKeysAdapter::fromJson)
                .orEmpty()
        }
    }

    private enum class StorageKey {
        WalletEncryptionKeys
    }
}
