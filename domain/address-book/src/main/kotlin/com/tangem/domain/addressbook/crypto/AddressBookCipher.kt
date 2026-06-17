package com.tangem.domain.addressbook.crypto

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.addressbook.error.AddressBookCryptoError
import com.tangem.domain.addressbook.model.AddressBook
import com.tangem.domain.addressbook.model.AddressBookBlob
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.extensions.hexToBytesOrNull
import com.tangem.utils.extensions.toHexString
import kotlinx.serialization.json.Json
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts and decrypts a wallet's [AddressBook] with AES-256-GCM.
 *
 * The symmetric key is derived deterministically from the wallet's public key (see
 * [AddressBookKeyDerivation]), so the same wallet always yields the same key — no key needs to be
 * stored. Each encryption uses a fresh random 12-byte nonce, so encrypting the same book twice
 * produces different blobs that both decrypt back to the original.
 *
 * The produced [AddressBookBlob] keeps the GCM authentication tag in a separate `auth_tag` field
 * (Java appends it to the ciphertext; this class splits it out and re-joins it on decrypt). A
 * tampered ciphertext or tag fails the tag check and surfaces as
 * [AddressBookCryptoError.DecryptionFailed].
 */
class AddressBookCipher {

    private val json = Json { ignoreUnknownKeys = true }
    private val secureRandom = SecureRandom()

    fun encrypt(
        addressBook: AddressBook,
        userWallet: UserWallet,
        updatedAt: DateTime,
    ): Either<AddressBookCryptoError, AddressBookBlob> = either {
        ensure(addressBook.walletId == userWallet.walletId) { AddressBookCryptoError.WalletMismatch }

        val aesKey = deriveKey(userWallet)
        val plaintext = json.encodeToString(AddressBook.serializer(), addressBook).toByteArray(Charsets.UTF_8)

        val nonce = ByteArray(NONCE_SIZE_BYTES).also(secureRandom::nextBytes)
        val cipherWithTag = cipher(Cipher.ENCRYPT_MODE, aesKey, nonce).doFinal(plaintext)

        // Java's GCM doFinal returns ciphertext || authTag — split the trailing tag out for the blob.
        val tagOffset = cipherWithTag.size - TAG_SIZE_BYTES
        val ciphertext = cipherWithTag.copyOfRange(fromIndex = 0, toIndex = tagOffset)
        val authTag = cipherWithTag.copyOfRange(fromIndex = tagOffset, toIndex = cipherWithTag.size)

        AddressBookBlob(
            walletId = addressBook.walletId.stringValue,
            updatedAt = updatedAt.withZone(DateTimeZone.UTC).toString(),
            nonce = nonce.toHexString().lowercase(),
            ciphertext = ciphertext.toHexString().lowercase(),
            authTag = authTag.toHexString().lowercase(),
        )
    }

    fun decrypt(blob: AddressBookBlob, userWallet: UserWallet): Either<AddressBookCryptoError, AddressBook> = either {
        ensure(blob.walletId == userWallet.walletId.stringValue) { AddressBookCryptoError.WalletMismatch }

        val aesKey = deriveKey(userWallet)
        val nonce = blob.nonce.hexToBytesOrNull() ?: raise(AddressBookCryptoError.DecryptionFailed)
        val ciphertext = blob.ciphertext.hexToBytesOrNull() ?: raise(AddressBookCryptoError.DecryptionFailed)
        val authTag = blob.authTag.hexToBytesOrNull() ?: raise(AddressBookCryptoError.DecryptionFailed)

        val plaintext = runCatching {
            cipher(Cipher.DECRYPT_MODE, aesKey, nonce).doFinal(ciphertext + authTag)
        }.getOrElse { raise(AddressBookCryptoError.DecryptionFailed) }

        runCatching {
            json.decodeFromString(AddressBook.serializer(), plaintext.toString(Charsets.UTF_8))
        }.getOrElse { raise(AddressBookCryptoError.MalformedBlob) }
    }

    private fun Raise<AddressBookCryptoError>.deriveKey(userWallet: UserWallet): ByteArray {
        val publicKey = AddressBookKeyDerivation.walletPublicKey(userWallet)
            ?: raise(AddressBookCryptoError.NoWalletPublicKey)
        return AddressBookKeyDerivation.deriveAesKey(publicKey)
    }

    private fun cipher(mode: Int, key: ByteArray, nonce: ByteArray): Cipher {
        return Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
            init(mode, SecretKeySpec(key, AES_ALGORITHM), GCMParameterSpec(TAG_SIZE_BITS, nonce))
        }
    }

    private companion object {
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_ALGORITHM = "AES"
        const val NONCE_SIZE_BYTES = 12
        const val TAG_SIZE_BYTES = 16
        const val TAG_SIZE_BITS = TAG_SIZE_BYTES * 8
    }
}