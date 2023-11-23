package com.tangem.tap.domain.userWalletList.repository.implementation

import android.security.keystore.KeyProperties
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.common.extensions.filterNotNull
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.model.UserWalletSensitiveInformation
import com.tangem.tap.domain.userWalletList.repository.UserWalletsSensitiveInformationRepository
import com.tangem.tap.domain.userWalletList.utils.sensitiveInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class DefaultUserWalletsSensitiveInformationRepository(
    moshi: Moshi,
    private val secureStorage: SecureStorage,
) : UserWalletsSensitiveInformationRepository {
    private val sensitiveInformationAdapter: JsonAdapter<UserWalletSensitiveInformation> = moshi.adapter(
        UserWalletSensitiveInformation::class.java,
    )
    private val encryptedSensitiveInformationMapAdapter: JsonAdapter<Map<String, ByteArray>> = moshi.adapter(
        Types.newParameterizedType(Map::class.java, String::class.java, ByteArray::class.java),
    )

    private val cipher: Cipher by lazy {
        Cipher.getInstance("$algorithm/$blockMode/$encryptionPadding")
    }

    override suspend fun save(userWallet: UserWallet, encryptionKey: ByteArray?): CompletionResult<Unit> {
        Timber.d(
            """
                Saving sensitive info
                |- User wallet ID: ${userWallet.walletId}
                |- Encryption key: $encryptionKey
            """.trimIndent()
        )

        if (encryptionKey == null) return CompletionResult.Success(Unit) // Encryption key is null, do nothing
        return catching {
            val encryptedSensitiveInformation = userWallet.sensitiveInformation
                .encode()
                .encryptAndStoreIv(userWallet.walletId.stringValue, encryptionKey)

            getAllEncrypted().toMutableMap()
                .apply { set(userWallet.walletId.stringValue, encryptedSensitiveInformation) }
                .let { saveInternal(it) }
        }
            .doOnFailure {
                Timber.e(it, "Unable to save sensitive info")
            }
            .doOnSuccess {
                Timber.d("Sensitive info saved")
            }
    }

    override suspend fun getAll(
        encryptionKeys: List<UserWalletEncryptionKey>,
    ): CompletionResult<Map<UserWalletId, UserWalletSensitiveInformation>> {
        if (encryptionKeys.isEmpty()) {
            return CompletionResult.Success(emptyMap())
        }

        return catching {
            val encryptedSensitiveInformation = getAllEncrypted()

            encryptionKeys
                .associateBy { it.walletId }
                .mapValues { (userWalletId, encryptionKey) ->
                    Timber.d(
                        """
                            Receiving sensitive info
                            |- User wallet ID: $userWalletId
                            |- Encryption key: $encryptionKey
                        """.trimIndent()
                    )

                    try {
                        encryptedSensitiveInformation[userWalletId.stringValue]
                            ?.getIvAndDecrypt(userWalletId.stringValue, encryptionKey.encryptionKey)
                            .decodeToSensitiveInformation()
                            .also {
                                Timber.d(
                                    """
                                    Sensitive info received
                                    |- Info: $it
                                """.trimIndent()
                                )
                            }
                    } catch (e: Throwable) {
                        Timber.e(e, "Unable to receive sensitive info")
                        throw e
                    }
                }
                .filterNotNull()
        }
    }

    override suspend fun delete(userWalletsIds: List<UserWalletId>): CompletionResult<Unit> {
        return catching { deleteInternal(userWalletsIds) }
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return catching { clearInternal() }
    }

    private suspend fun getAllEncrypted(): Map<String, ByteArray> {
        return withContext(Dispatchers.IO) {
            secureStorage.get(StorageKey.UserWalletsSensitiveInformation.name)
                .decodeToEncryptedSensitiveInformation()
        }
    }

    private suspend fun saveInternal(sensitiveInformation: Map<String, ByteArray>) {
        Timber.d(
            """
                Saving sensitive information
                |- Info: $sensitiveInformation
            """.trimIndent()
        )

        return withContext(Dispatchers.IO) {
            secureStorage.store(
                account = StorageKey.UserWalletsSensitiveInformation.name,
                data = sensitiveInformation.encode(),
            )
        }
    }

    private suspend fun deleteInternal(userWalletsIds: List<UserWalletId>) {
        return saveInternal(
            sensitiveInformation = getAllEncrypted() - userWalletsIds.map { it.stringValue }.toSet(),
        )
    }

    private suspend fun clearInternal() {
        withContext(Dispatchers.IO) {
            secureStorage.delete(StorageKey.UserWalletsSensitiveInformation.name)
        }
    }

    private suspend fun ByteArray?.decodeToEncryptedSensitiveInformation(): Map<String, ByteArray> {
        return withContext(Dispatchers.Default) {
            this@decodeToEncryptedSensitiveInformation
                .also {
                    Timber.d(
                        """
                            Decoding encrypted user wallets sensitive info
                            |- Info bytes: $it
                        """.trimIndent()
                    )
                }
                ?.decodeToString(throwOnInvalidSequence = false)
                .also {
                    Timber.d(
                        """
                            Encrypted user wallets sensitive info were decoded to string
                            |- Info string: $it
                        """.trimIndent()
                    )
                }
                ?.let(encryptedSensitiveInformationMapAdapter::fromJson)
                .also {
                    Timber.d(
                        """
                            User wallets sensitive info were converted from JSON
                            |- Encrypted info: $it
                        """.trimIndent()
                    )
                }
                .orEmpty()
        }
    }

    private suspend fun ByteArray?.decodeToSensitiveInformation(): UserWalletSensitiveInformation? {
        return withContext(Dispatchers.Default) {
            this@decodeToSensitiveInformation
                .also {
                    Timber.d(
                        """
                            Decoding decrypted user wallet sensitive info
                            |- Info bytes: $it
                        """.trimIndent()
                    )
                }
                ?.decodeToString(throwOnInvalidSequence = false)
                .also {
                    Timber.d(
                        """
                            User wallet sensitive info was decoded to string
                            |- Info string: $it
                        """.trimIndent()
                    )
                }
                ?.let(sensitiveInformationAdapter::fromJson)
                .also {
                    Timber.d(
                        """
                            User wallet sensitive info was converted from JSON
                            |- Info: $it
                        """.trimIndent()
                    )
                }
        }
    }

    private suspend fun UserWalletSensitiveInformation.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            this@encode
                .also {
                    Timber.d(
                        """
                            Encoding decrypted user wallet sensitive info
                            |- Info: $it
                        """.trimIndent()
                    )
                }
                .let(sensitiveInformationAdapter::toJson)
                .also {
                    Timber.d(
                        """
                            User wallet sensitive info was converted to JSON
                            |- JSON: $it
                        """.trimIndent()
                    )
                }
                ?.encodeToByteArray(throwOnInvalidSequence = false)
                .also {
                    Timber.d(
                        """
                            User wallet sensitive info JSON was encoded to bytes
                            |- Info bytes: $it
                        """.trimIndent()
                    )
                }
                .let {
                    it ?: error("User wallet sensitive info is null after encoding")
                }
        }
    }

    private suspend fun Map<String, ByteArray>.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            this@encode
                .also {
                    Timber.d(
                        """
                            Encoding encrypted user wallets sensitive information
                            |- Info: $it
                        """.trimIndent()
                    )
                }
                .let(encryptedSensitiveInformationMapAdapter::toJson)
                .also {
                    Timber.d(
                        """
                            User wallets sensitive info were converted to JSON
                            |- JSON: $it
                        """.trimIndent()
                    )
                }
                ?.encodeToByteArray(throwOnInvalidSequence = false)
                .also {
                    Timber.d(
                        """
                            JSON with user wallets sensitive info was encoded to bytes
                            |- Info bytes: $it
                        """.trimIndent()
                    )
                }
                .let {
                    it ?: error("User wallets sensitive info is null after encoding")
                }
        }
    }

    private suspend fun ByteArray.encryptAndStoreIv(userWalletId: String, encryptionKey: ByteArray): ByteArray {
        Timber.d(
            """
                Encrypting sensitive info
                |- User wallet ID: $userWalletId
                |- Encoded info bytes: $this
                |- Key: $encryptionKey
            """.trimIndent()
        )

        return withContext(Dispatchers.Default) {
            val secretKey = SecretKeySpec(encryptionKey, algorithm)
            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            } catch (e: Throwable) {
                Timber.e(e, "Unable to init cipher for sensitive info encryption")
                throw e
            }
            val encryptedData = try {
                cipher.doFinal(this@encryptAndStoreIv)
            } catch (e: Throwable) {
                Timber.e(e, "Unable to encrypt sensitive info")
                throw e
            }

            Timber.d(
                """
                    Sensitive info was encrypted
                    |- Encrypted info bytes: $encryptedData
                    |- IV: ${cipher.iv}
                    |- IV storage key: ${StorageKey.SensitiveInformationIv(userWalletId).name}
                """.trimIndent()
            )

            secureStorage.store(data = cipher.iv, account = StorageKey.SensitiveInformationIv(userWalletId).name)
            encryptedData
        }
    }

    private suspend fun ByteArray.getIvAndDecrypt(userWalletId: String, encryptionKey: ByteArray): ByteArray? {
        Timber.d(
            """
                Decrypting sensitive info
                |- User wallet ID: $userWalletId
                |- Encoded info bytes: $this
                |- Key: $encryptionKey
            """.trimIndent()
        )

        return withContext(Dispatchers.Default) {
            val iv = secureStorage.get(StorageKey.SensitiveInformationIv(userWalletId).name)
            if (iv == null) {
                Timber.e("Unable to find IV for sensitive info decryption")
                error("IV not found")
            } else {
                Timber.d(
                    """
                        IV for sensitive info decryption was found
                        |- IV: $iv
                        |- IV storage key: ${StorageKey.SensitiveInformationIv(userWalletId).name}
                    """.trimIndent()
                )
            }

            val ivParam = IvParameterSpec(iv)
            val secretKeySpec = SecretKeySpec(encryptionKey, algorithm)

            try {
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParam)
            } catch (e: Throwable) {
                Timber.e(e, "Unable to init cipher for sensitive info decryption")
                throw e
            }

            val decryptedInfo = try {
                cipher.doFinal(this@getIvAndDecrypt)
            } catch (e: Throwable) {
                Timber.e(e, "Unable to decrypt sensitive info")
                throw e
            }

            Timber.d(
                """
                    Sensitive info decrypted
                    |- Decrypted info bytes: $decryptedInfo
                """.trimIndent()
            )


            decryptedInfo
        }
    }

    private sealed interface StorageKey {
        val name: String

        object UserWalletsSensitiveInformation : StorageKey {
            override val name: String = "user_wallets_sensitive_information"
        }

        class SensitiveInformationIv(userWalletId: String) : StorageKey {
            override val name: String = "user_wallet_sensitive_information_iv_$userWalletId"
        }
    }

    companion object {
        private const val algorithm = KeyProperties.KEY_ALGORITHM_AES
        private const val blockMode = KeyProperties.BLOCK_MODE_CBC
        private const val encryptionPadding = KeyProperties.ENCRYPTION_PADDING_PKCS7
    }
}