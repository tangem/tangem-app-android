package com.tangem.tap.domain.userWalletList.repository.implementation

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.operations.AESCipherOperations
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.tap.common.extensions.filterNotNull
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.model.UserWalletSensitiveInformation
import com.tangem.tap.domain.userWalletList.repository.UserWalletsSensitiveInformationRepository
import com.tangem.tap.domain.userWalletList.utils.sensitiveInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.crypto.spec.SecretKeySpec

internal class DefaultUserWalletsSensitiveInformationRepository(
    moshi: Moshi,
    private val secureStorage: SecureStorage,
) : UserWalletsSensitiveInformationRepository {

    private val sensitiveInformationAdapter: JsonAdapter<UserWalletSensitiveInformation> by lazy {
        moshi.adapter(UserWalletSensitiveInformation::class.java)
    }

    private val encryptedSensitiveInformationMapAdapter: JsonAdapter<Map<String, ByteArray>> by lazy {
        moshi.adapter(
            Types.newParameterizedType(Map::class.java, String::class.java, ByteArray::class.java),
        )
    }

    override suspend fun save(userWallet: UserWallet, encryptionKey: ByteArray?): CompletionResult<Unit> {
        if (encryptionKey == null) {
            return CompletionResult.Success(Unit) // Encryption key is null, do nothing
        }

        return catching {
            val encryptedSensitiveInformation = userWallet.sensitiveInformation
                .encode()
                .encryptAndStoreIv(userWallet.walletId.stringValue, encryptionKey)

            getAllEncrypted().toMutableMap()
                .apply { set(userWallet.walletId.stringValue, encryptedSensitiveInformation) }
                .let { saveInternal(it) }
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
                        ?.decodeToSensitiveInformation()
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
                ?.decodeToEncryptedSensitiveInformation()
                .orEmpty()
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

    private suspend fun ByteArray.decodeToEncryptedSensitiveInformation(): Map<String, ByteArray>? {
        return withContext(Dispatchers.Default) {
            encryptedSensitiveInformationMapAdapter.fromJson(
                this@decodeToEncryptedSensitiveInformation.decodeToString(throwOnInvalidSequence = true),
            )
        }
    }

    private suspend fun ByteArray.decodeToSensitiveInformation(): UserWalletSensitiveInformation? {
        return withContext(Dispatchers.Default) {
            try {
                sensitiveInformationAdapter.fromJson(
                    this@decodeToSensitiveInformation.decodeToString(throwOnInvalidSequence = true),
                )
            } catch (e: CharacterCodingException) {
                Timber.e(e, "Unable to decode sensitive information")

                null
            }
        }
    }

    private suspend fun UserWalletSensitiveInformation.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            sensitiveInformationAdapter.toJson(this@encode)
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
    }

    private suspend fun Map<String, ByteArray>.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            encryptedSensitiveInformationMapAdapter.toJson(this@encode)
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
    }

    private suspend fun ByteArray.encryptAndStoreIv(userWalletId: String, encryptionKey: ByteArray): ByteArray {
        return withContext(Dispatchers.Default) {
            val secretKey = SecretKeySpec(encryptionKey, AESCipherOperations.KEY_ALGORITHM)
            val cipher = AESCipherOperations.initEncryptionCipher(secretKey)
            val encryptedData = AESCipherOperations.encrypt(cipher, decryptedData = this@encryptAndStoreIv)

            secureStorage.store(cipher.iv, StorageKey.SensitiveInformationIv(userWalletId).name)

            encryptedData
        }
    }

    private suspend fun ByteArray.getIvAndDecrypt(userWalletId: String, encryptionKey: ByteArray): ByteArray {
        return withContext(Dispatchers.Default) {
            val secretKeySpec = SecretKeySpec(encryptionKey, AESCipherOperations.KEY_ALGORITHM)
            val iv = secureStorage.get(StorageKey.SensitiveInformationIv(userWalletId).name)
                ?: error("IV for user wallet $userWalletId not found")
            val cipher = AESCipherOperations.initDecryptionCipher(secretKeySpec, iv)

            AESCipherOperations.decrypt(cipher, encryptedData = this@getIvAndDecrypt)
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
}