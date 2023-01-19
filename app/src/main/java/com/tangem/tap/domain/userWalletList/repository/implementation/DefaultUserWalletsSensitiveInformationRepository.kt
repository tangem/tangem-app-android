package com.tangem.tap.domain.userWalletList.repository.implementation

import android.security.keystore.KeyProperties
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.common.mapFailure
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.extensions.filterNotNull
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
    private val encryptedSensitiveInformationMapAdapter: JsonAdapter<Map<String, ByteArray>> = moshi.adapter(
        Types.newParameterizedType(Map::class.java, String::class.java, ByteArray::class.java),
    )

    private val cipher: Cipher by lazy {
        Cipher.getInstance("$algorithm/$blockMode/$encryptionPadding")
    }

    override suspend fun save(userWallet: UserWallet, encryptionKey: ByteArray?): CompletionResult<Unit> {
        if (encryptionKey == null) return CompletionResult.Success(Unit) // Encryption key is null, do nothing
        return catching {
            val encryptedSensitiveInformation = userWallet.sensitiveInformation
                .encode()
                .encryptAndStoreIv(userWallet.walletId.stringValue, encryptionKey)

            getAllEncrypted().toMutableMap()
                .apply { set(userWallet.walletId.stringValue, encryptedSensitiveInformation) }
                .let { saveInternal(it) }
        }
            .mapFailure { error ->
                UserWalletListError.SaveSensitiveInformationError(error.cause ?: error)
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
                    encryptedSensitiveInformation[userWalletId.stringValue]
                        ?.getIvAndDecrypt(userWalletId.stringValue, encryptionKey.encryptionKey)
                        .decodeToSensitiveInformation()
                }
                .filterNotNull()
        }
            .mapFailure { error ->
                UserWalletListError.ReceiveSensitiveInformationError(error.cause ?: error)
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
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(encryptedSensitiveInformationMapAdapter::fromJson)
                .orEmpty()
        }
    }

    private suspend fun ByteArray?.decodeToSensitiveInformation(): UserWalletSensitiveInformation? {
        return withContext(Dispatchers.Default) {
            this@decodeToSensitiveInformation
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(sensitiveInformationAdapter::fromJson)
        }
    }

    private suspend fun UserWalletSensitiveInformation.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            this@encode
                .let(sensitiveInformationAdapter::toJson)
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
    }

    private suspend fun Map<String, ByteArray>.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            this@encode
                .let(encryptedSensitiveInformationMapAdapter::toJson)
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
    }

    private suspend fun ByteArray.encryptAndStoreIv(userWalletId: String, encryptionKey: ByteArray): ByteArray {
        return withContext(Dispatchers.Default) {
            val secretKey = SecretKeySpec(encryptionKey, algorithm)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedData = cipher.doFinal(this@encryptAndStoreIv)
            secureStorage.store(data = cipher.iv, account = StorageKey.SensitiveInformationIv(userWalletId).name)
            encryptedData
        }
    }

    private suspend fun ByteArray.getIvAndDecrypt(
        userWalletId: String,
        encryptionKey: ByteArray,
    ): ByteArray? {
        return withContext(Dispatchers.Default) {
            val iv = secureStorage.get(StorageKey.SensitiveInformationIv(userWalletId).name)
                ?: error("IV not found")
            val ivParam = IvParameterSpec(iv)
            val secretKeySpec = SecretKeySpec(encryptionKey, algorithm)
            cipher
                .also { it.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParam) }
                .doFinal(this@getIvAndDecrypt)
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
