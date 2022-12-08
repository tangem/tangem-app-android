package com.tangem.tap.domain.userWalletList.repository.implementation

import android.security.keystore.KeyProperties
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.common.mapFailure
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.common.util.UserWalletId
import com.tangem.domain.common.util.encryptionKey
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.userWalletList.UserWalletListError
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.model.UserWalletSensitiveInformation
import com.tangem.tap.domain.userWalletList.repository.UserWalletsSensitiveInformationRepository
import com.tangem.tap.domain.userWalletList.utils.sensitiveInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val cipher: Cipher by lazy {
        Cipher.getInstance("$algorithm/$blockMode/$encryptionPadding")
    }

    override suspend fun save(userWallet: UserWallet): CompletionResult<Unit> {
        return catching {
            withContext(Dispatchers.Default) {
                userWallet.sensitiveInformation
                    .let(sensitiveInformationAdapter::toJson)
                    .encodeToByteArray(throwOnInvalidSequence = true)
                    .encryptAndStoreIv(
                        walletId = userWallet.walletId,
                        encryptionKey = userWallet.scanResponse.card.encryptionKey,
                    )
                    .let { encryptedInformation ->
                        withContext(Dispatchers.IO) {
                            secureStorage.store(
                                data = encryptedInformation,
                                account = StorageKey.SensitiveInformation(userWallet.walletId).name,
                            )
                        }
                    }
            }
        }
            .mapFailure { error ->
                UserWalletListError.SaveSensitiveInformationError(error.cause ?: error)
            }
    }

    override suspend fun getAll(
        encryptionKeys: List<UserWalletEncryptionKey>,
    ): CompletionResult<Map<UserWalletId, UserWalletSensitiveInformation>> {
        return catching {
            if (encryptionKeys.isEmpty()) {
                return@catching emptyMap()
            }

            val keyToEncryptedInformation = withContext(Dispatchers.IO) {
                encryptionKeys.associateWith { encryptionKey ->
                    secureStorage.get(StorageKey.SensitiveInformation(encryptionKey.walletId).name)
                }
            }

            withContext(Dispatchers.Default) {
                val keyToInformation =
                    mutableMapOf<UserWalletId, UserWalletSensitiveInformation>()

                keyToEncryptedInformation.forEach { (key, encryptedInformation) ->
                    val information = encryptedInformation
                        ?.getIvAndDecrypt(
                            walletId = key.walletId,
                            encryptionKey = key.encryptionKey,
                        )
                        ?.decodeToString(throwOnInvalidSequence = true)
                        ?.let(sensitiveInformationAdapter::fromJson)

                    if (information != null) {
                        keyToInformation[key.walletId] = information
                    }
                }

                keyToInformation
            }
        }
            .mapFailure { error ->
                UserWalletListError.ReceiveSensitiveInformationError(error.cause ?: error)
            }
    }

    override suspend fun delete(walletIds: List<UserWalletId>): CompletionResult<Unit> = catching {
        walletIds
            .forEach { walletId ->
                secureStorage.delete(StorageKey.SensitiveInformation(walletId).name)
            }
    }

    private fun ByteArray.encryptAndStoreIv(walletId: UserWalletId, encryptionKey: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(encryptionKey, algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedData = cipher.doFinal(this)
        secureStorage.store(data = cipher.iv, account = StorageKey.SensitiveInformationIv(walletId).name)
        return encryptedData
    }

    private fun ByteArray.getIvAndDecrypt(walletId: UserWalletId, encryptionKey: ByteArray): ByteArray? {
        val iv = secureStorage.get(StorageKey.SensitiveInformationIv(walletId).name)
            ?: error("IV not found")
        val ivParam = IvParameterSpec(iv)
        val secretKeySpec = SecretKeySpec(encryptionKey, algorithm)
        return cipher
            .also { it.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParam) }
            .doFinal(this)
    }

    private sealed interface StorageKey {
        val name: String

        class SensitiveInformation(walletId: UserWalletId) : StorageKey {
            override val name: String = "user_wallet_sensitive_information_${walletId.stringValue}"
        }

        class SensitiveInformationIv(walletId: UserWalletId) : StorageKey {
            override val name: String = "user_wallet_sensitive_information_iv_${walletId.stringValue}"
        }
    }

    companion object {
        private const val algorithm = KeyProperties.KEY_ALGORITHM_AES
        private const val blockMode = KeyProperties.BLOCK_MODE_CBC
        private const val encryptionPadding = KeyProperties.ENCRYPTION_PADDING_PKCS7
    }
}
