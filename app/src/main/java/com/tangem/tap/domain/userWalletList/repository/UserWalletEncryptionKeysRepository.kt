package com.tangem.tap.domain.userWalletList.repository

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.authentication.storage.AuthenticatedStorage
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.hot.sdk.android.crypto.AESEncryptionProtocol
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class UserWalletEncryptionKeysRepository(
    moshi: Moshi,
    private val authenticatedStorage: AuthenticatedStorage,
    private val dispatchers: CoroutineDispatcherProvider,
    private val secureStorage: SecureStorage,
) {

    private val encryptionKeyAdapter: JsonAdapter<UserWalletEncryptionKey> = moshi.adapter(
        UserWalletEncryptionKey::class.java,
    )
    private val userWalletsIdsListAdapter: JsonAdapter<List<UserWalletId>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, UserWalletId::class.java),
    )

    suspend fun save(
        encryptionKey: UserWalletEncryptionKey,
        removeUnsecured: Boolean = true,
        method: EncryptionMethod,
    ) = withContext(dispatchers.io) {
        if (removeUnsecured) {
            secureStorage.delete(StorageKey.UserWalletEncryptionKeyUnsecured(encryptionKey.walletId).name)
        }

        when (method) {
            EncryptionMethod.Unsecured -> {
                secureStorage.store(
                    account = StorageKey.UserWalletEncryptionKeyUnsecured(encryptionKey.walletId).name,
                    data = encryptionKey.encode(),
                )
            }
            is EncryptionMethod.Password -> {
                val encodedWithPass = AESEncryptionProtocol.encryptWithPassword(
                    password = method.password,
                    content = encryptionKey.encode(),
                )
                secureStorage.store(
                    account = StorageKey.UserWalletEncryptionKeyEncrypted(encryptionKey.walletId).name,
                    data = encodedWithPass,
                )
            }
            EncryptionMethod.Biometric -> {
                authenticatedStorage.store(
                    keyAlias = StorageKey.UserWalletEncryptionKey(encryptionKey.walletId).name,
                    data = encryptionKey.encode(),
                )
            }
        }

        storeUserWalletId(userWalletId = encryptionKey.walletId)
    }

    fun removeBiometricKey(userWalletId: UserWalletId) {
        authenticatedStorage.delete(StorageKey.UserWalletEncryptionKey(userWalletId).name)
    }

    suspend fun getAllUnsecured(): List<UserWalletEncryptionKey> = withContext(dispatchers.io) {
        getUserWalletsIds().mapNotNull { userWalletId ->
            secureStorage.get(account = StorageKey.UserWalletEncryptionKeyUnsecured(userWalletId).name).decodeToKey()
        }
    }

    suspend fun getEncryptedWithPassword(userWalletId: UserWalletId, password: CharArray): UserWalletEncryptionKey? =
        withContext(dispatchers.io) {
            val encrypted = secureStorage.get(
                account = StorageKey.UserWalletEncryptionKeyEncrypted(userWalletId).name,
            ) ?: return@withContext null

            withContext(dispatchers.default) {
                AESEncryptionProtocol.decryptWithPassword(password, encrypted).decodeToKey()
            }
        }

    suspend fun getAllBiometric(): List<UserWalletEncryptionKey> = withContext(dispatchers.io) {
        val keys = getUserWalletsIds().map { userWalletId ->
            StorageKey.UserWalletEncryptionKey(userWalletId).name
        }

        authenticatedStorage.get(keys).mapNotNull {
            it.value.decodeToKey()
        }
    }

    suspend fun delete(userWalletIds: List<UserWalletId>) {
        if (userWalletIds.isEmpty()) return

        withContext(dispatchers.io) {
            userWalletIds.forEach { userWalletId ->
                secureStorage.delete(StorageKey.UserWalletEncryptionKeyUnsecured(userWalletId).name)
                secureStorage.delete(StorageKey.UserWalletEncryptionKeyEncrypted(userWalletId).name)
                authenticatedStorage.delete(StorageKey.UserWalletEncryptionKey(userWalletId).name)
            }

            val userWalletsIds = getUserWalletsIds().filterNot { it in userWalletIds }
            secureStorage.store(userWalletsIds.encode(), StorageKey.UserWalletIds.name)
        }
    }

    suspend fun clear() {
        withContext(dispatchers.io) {
            val userWalletsIds = getUserWalletsIds()
            userWalletsIds.forEach { userWalletId ->
                secureStorage.delete(StorageKey.UserWalletEncryptionKeyUnsecured(userWalletId).name)
                secureStorage.delete(StorageKey.UserWalletEncryptionKeyEncrypted(userWalletId).name)
                authenticatedStorage.delete(StorageKey.UserWalletEncryptionKey(userWalletId).name)
            }
            secureStorage.delete(StorageKey.UserWalletIds.name)
        }
    }

    private suspend fun getUserWalletsIds(): List<UserWalletId> {
        return withContext(dispatchers.io) {
            secureStorage.get(StorageKey.UserWalletIds.name)
                .decodeToUserWalletsIds()
        }
    }

    private suspend fun storeUserWalletId(userWalletId: UserWalletId) {
        val userWalletIds = (getUserWalletsIds() + userWalletId).distinct()

        withContext(dispatchers.io) {
            secureStorage.store(userWalletIds.encode(), StorageKey.UserWalletIds.name)
        }
    }

    private suspend fun UserWalletEncryptionKey.encode(): ByteArray {
        return withContext(dispatchers.default) {
            this@encode
                .let(encryptionKeyAdapter::toJson)
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
    }

    private suspend fun ByteArray?.decodeToKey(): UserWalletEncryptionKey? {
        return withContext(dispatchers.default) {
            this@decodeToKey
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(encryptionKeyAdapter::fromJson)
        }
    }

    private suspend fun List<UserWalletId>.encode(): ByteArray {
        return withContext(dispatchers.default) {
            this@encode
                .let(userWalletsIdsListAdapter::toJson)
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
    }

    private suspend fun ByteArray?.decodeToUserWalletsIds(): List<UserWalletId> {
        return withContext(dispatchers.default) {
            this@decodeToUserWalletsIds
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(userWalletsIdsListAdapter::fromJson)
                .orEmpty()
        }
    }

    sealed class EncryptionMethod {
        data object Unsecured : EncryptionMethod()
        data object Biometric : EncryptionMethod()
        class Password(val password: CharArray) : EncryptionMethod()
    }

    private sealed interface StorageKey {
        val name: String

        class UserWalletEncryptionKeyUnsecured(userWalletId: UserWalletId) : StorageKey {
            override val name: String = "user_wallet_encryption_key_unsecured_${userWalletId.stringValue}"
        }

        class UserWalletEncryptionKey(userWalletId: UserWalletId) : StorageKey {
            override val name: String = "user_wallet_encryption_key_${userWalletId.stringValue}"
        }

        class UserWalletEncryptionKeyEncrypted(userWalletId: UserWalletId) : StorageKey {
            override val name: String = "user_wallet_encryption_key_encrypted_${userWalletId.stringValue}"
        }

        object UserWalletIds : StorageKey {
            override val name: String = "user_wallets_ids_with_saved_keys"
        }
    }
}