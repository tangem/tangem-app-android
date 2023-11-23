package com.tangem.tap.domain.userWalletList.repository.implementation

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.*
import com.tangem.common.authentication.AuthenticatedStorage
import com.tangem.common.core.TangemSdkError
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.repository.UserWalletsKeysRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class BiometricUserWalletsKeysRepository(
    moshi: Moshi,
    private val authenticatedStorage: AuthenticatedStorage,
    private val secureStorage: SecureStorage,
) : UserWalletsKeysRepository {

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
                        is TangemSdkError.AuthenticationLockout ->
                            UserWalletsListError.BiometricsAuthenticationLockout(isPermanent = false)
                        is TangemSdkError.AuthenticationPermanentLockout ->
                            UserWalletsListError.BiometricsAuthenticationLockout(isPermanent = true)
                        is TangemSdkError.KeystoreInvalidated ->
                            UserWalletsListError.AllKeysInvalidated
                        is TangemSdkError.AuthenticationUnavailable ->
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

    override suspend fun delete(userWalletsIds: List<UserWalletId>) {
        return withContext(Dispatchers.IO) {
            userWalletsIds.forEach { userWalletId ->
                deleteEncryptionKey(userWalletId)
            }

            deleteUserWalletsIds(userWalletsIds)
        }
    }

    override suspend fun clear() {
        return withContext(Dispatchers.IO) {
            getUserWalletsIds()
                .forEach { userWalletId ->
                    deleteEncryptionKey(userWalletId)
                }

            clearUserWalletsIds()
        }
    }

    override fun hasSavedEncryptionKeys(): Boolean {
        return runBlocking {
            getUserWalletsIds().isNotEmpty()
        }
    }

    private suspend fun getAllInternal(): CompletionResult<List<UserWalletEncryptionKey>> {
        return getUserWalletsIds()
            .also {
                Timber.d(
                    """
                        Receiving encryption keys
                        |- User wallet IDs: $it
                    """.trimIndent()
                )
            }
            .map { userWalletId ->
                getEncryptionKey(userWalletId)
                    .doOnFailure { error ->
                        when (error) {
                            is TangemSdkError.UserCanceledAuthentication -> {
                                // If the user cancels biometric authentication, then cancel operation with error
                                return CompletionResult.Failure(error)
                            }
                            is TangemSdkError.KeystoreInvalidated -> {
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
            .fold<UserWalletEncryptionKey?, List<UserWalletEncryptionKey>>(listOf()) { acc, data ->
                if (data != null) acc + data else acc
            }
            .doOnFailure {
                Timber.e(it, "Unable to receive encryption keys")
            }
            .doOnSuccess {
                Timber.d(
                    """
                        Encryption keys received
                        |- Keys: $it
                    """.trimIndent()
                )
            }
    }

    private suspend fun getEncryptionKey(userWalletId: UserWalletId): CompletionResult<UserWalletEncryptionKey?> {
        Timber.d(
            """
            Get encryption key
            |- User wallet ID: $userWalletId
            """.trimIndent()
        )

        return catching { authenticatedStorage.get(StorageKey.UserWalletEncryptionKey(userWalletId).name) }
            .map { it.decodeToKey() }
            .doOnFailure {
                Timber.e(
                    """
                        Unable to get encryption key
                        |- Error: $it
                    """.trimIndent()
                )
            }
            .doOnSuccess {
                Timber.d(
                    """
                        Encryption key received
                        |- Key: $it
                    """.trimIndent()
                )
            }
    }

    private suspend fun storeEncryptionKey(encryptionKey: UserWalletEncryptionKey): CompletionResult<Unit> {
        Timber.d(
            """
                Save encryption key
                |- Key: $encryptionKey
            """.trimIndent()
        )

        return catching {
            authenticatedStorage.store(
                key = StorageKey.UserWalletEncryptionKey(encryptionKey.walletId).name,
                data = encryptionKey.encode(),
            )
        }
            .map { storeUserWalletId(encryptionKey.walletId) }
            .doOnFailure {
                Timber.e(
                    """
                        Unable to save encryption key
                        |- Error: $it
                    """.trimIndent()
                )
            }
            .doOnSuccess {
                Timber.d("Encryption key saved")
            }
    }

    private fun deleteEncryptionKey(userWalletId: UserWalletId) {
        Timber.d(
            """
                Delete encryption key
                |- User wallet ID: $userWalletId
            """.trimIndent()
        )

        return authenticatedStorage.delete(StorageKey.UserWalletEncryptionKey(userWalletId).name)
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
                .also {
                    Timber.d(
                        """
                            Encoding encryption key
                            |- Key: $encryptionKey
                        """.trimIndent()
                    )
                }
                .let(encryptionKeyAdapter::toJson)
                .also {
                    Timber.d(
                        """
                            Encryption key converted to JSON
                            |- JSON: $it
                        """.trimIndent()
                    )
                }
                .encodeToByteArray(throwOnInvalidSequence = false)
                .also {
                    Timber.d(
                        """
                            Encryption key JSON encoded to bytes
                            |- Key bytes: $it
                        """.trimIndent()
                    )
                }
        }
    }

    private suspend fun ByteArray?.decodeToKey(): UserWalletEncryptionKey? {
        return withContext(Dispatchers.Default) {
            this@decodeToKey
                .also {
                    Timber.d(
                        """
                            Decoding encryption key bytes
                            |- Key bytes: $it
                        """.trimIndent()
                    )
                }
                ?.decodeToString(throwOnInvalidSequence = false)
                .also {
                    Timber.d(
                        """
                            Decoded encryption key bytes
                            |- Decoded key: $it
                        """.trimIndent()
                    )
                }
                ?.let(encryptionKeyAdapter::fromJson)
                .also {
                    Timber.d(
                        """
                            Converted encryption key from JSON
                            |- Key: $it
                        """.trimIndent()
                    )
                }
        }
    }

    private suspend fun List<UserWalletId>.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            this@encode
                .also {
                    Timber.d(
                        """
                            Encoding list of saved IDs
                            |- List: $it
                        """.trimIndent()
                    )
                }
                .let(userWalletsIdsListAdapter::toJson)
                .also {
                    Timber.d(
                        """
                            List of IDs converted to JSON
                            |- JSON: $it
                        """.trimIndent()
                    )
                }
                .encodeToByteArray(throwOnInvalidSequence = false)
                .also {
                    Timber.d(
                        """
                            List of IDs in JSON encoded to bytes
                            |- IDs bytes: $it
                        """.trimIndent()
                    )
                }
        }
    }

    private suspend fun ByteArray?.decodeToUserWalletsIds(): List<UserWalletId> {
        return withContext(Dispatchers.Default) {
            this@decodeToUserWalletsIds
                .also {
                    Timber.d(
                        """
                            Decoding saved IDs bytes
                            |- Bytes: $it
                        """.trimIndent()
                    )
                }
                ?.decodeToString(throwOnInvalidSequence = false)
                .also {
                    Timber.d(
                        """
                            Saved IDs decoded
                            |- IDs: $it
                        """.trimIndent()
                    )
                }
                ?.let(userWalletsIdsListAdapter::fromJson)
                .also {
                    Timber.d(
                        """
                            Saved IDs converted from JSON
                            |- IDs: $it
                        """.trimIndent()
                    )
                }
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