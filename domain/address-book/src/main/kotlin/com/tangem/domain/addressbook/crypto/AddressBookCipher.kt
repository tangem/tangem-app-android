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
import com.tangem.utils.logging.TangemLogger
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
 * The produced [AddressBookBlob] keeps the GCM authentication tag in a separate `authTag` field
 * (Java appends it to the ciphertext; this class splits it out and re-joins it on decrypt). A
 * tampered ciphertext or tag fails the tag check and surfaces as
 * [AddressBookCryptoError.DecryptionFailed].
 */
class AddressBookCipher {

    private val json = Json { ignoreUnknownKeys = true }
    private val secureRandom = SecureRandom()
    private val logger = TangemLogger.withTag(LOG_TAG)

    fun encrypt(
        addressBook: AddressBook,
        userWallet: UserWallet,
        updatedAt: DateTime,
    ): Either<AddressBookCryptoError, AddressBookBlob> = either {
        val aesKey = deriveKey(userWallet)
        val plaintext = json.encodeToString(AddressBook.serializer(), addressBook).toByteArray(Charsets.UTF_8)

        val nonce = ByteArray(NONCE_SIZE_BYTES).also(secureRandom::nextBytes)
        val cipherWithTag = cipher(Cipher.ENCRYPT_MODE, aesKey, nonce).doFinal(plaintext)

        // Java's GCM doFinal returns ciphertext || authTag — split the trailing tag out for the blob.
        val tagOffset = cipherWithTag.size - TAG_SIZE_BYTES
        val ciphertext = cipherWithTag.copyOfRange(fromIndex = 0, toIndex = tagOffset)
        val authTag = cipherWithTag.copyOfRange(fromIndex = tagOffset, toIndex = cipherWithTag.size)

        AddressBookBlob(
            walletId = userWallet.walletId.stringValue,
            updatedAt = updatedAt.withZone(DateTimeZone.UTC).toString(),
            nonce = nonce.toHexString().lowercase(),
            ciphertext = ciphertext.toHexString().lowercase(),
            authTag = authTag.toHexString().lowercase(),
        ).also { blob ->
            // Metadata only — the plaintext (contact names/addresses/memos) is never logged.
            logger.i(
                "Encrypted address book for wallet ${blob.walletId}: contacts=${addressBook.contacts.size}, " +
                    "plaintextBytes=${plaintext.size}, " +
                    "nonceLen=${blob.nonce.length}, ciphertextLen=${blob.ciphertext.length}, " +
                    "authTagLen=${blob.authTag.length}",
            )
        }
    }

    fun decrypt(blob: AddressBookBlob, userWallet: UserWallet): Either<AddressBookCryptoError, AddressBook> = either {
        // Metadata only — helps QA correlate a failing blob with what was received from the backend/other platform.
        logger.i(
            "Decrypting address book for wallet ${blob.walletId}: " +
                "updatedAt=${blob.updatedAt}, nonceLen=${blob.nonce.length}, " +
                "ciphertextLen=${blob.ciphertext.length}, authTagLen=${blob.authTag.length}",
        )

        ensure(blob.walletId == userWallet.walletId.stringValue) {
            logger.e(
                "Wallet mismatch decrypting address book: blob wallet=${blob.walletId}, " +
                    "target wallet=${userWallet.walletId.stringValue}",
            )
            AddressBookCryptoError.WalletMismatch
        }

        val aesKey = deriveKey(userWallet)
        val nonce = blob.nonce.hexToBytesOrNull() ?: raiseNonHex(blob, field = "nonce", value = blob.nonce)
        val ciphertext = blob.ciphertext.hexToBytesOrNull() ?: raiseNonHex(
            blob = blob,
            field = "ciphertext",
            value = blob.ciphertext,
        )
        val authTag = blob.authTag.hexToBytesOrNull() ?: raiseNonHex(blob, field = "authTag", value = blob.authTag)

        val plaintext = runCatching {
            cipher(Cipher.DECRYPT_MODE, aesKey, nonce).doFinal(ciphertext + authTag)
        }.getOrElse { error ->
            logger.e(
                "Failed to AES-GCM decrypt address book for wallet ${blob.walletId} " +
                    "(nonceBytes=${nonce.size}, ciphertextBytes=${ciphertext.size}, authTagBytes=${authTag.size}): " +
                    "${error.safeDescription()}. Usually a wrong key/nonce/tag or a corrupted blob.",
                error,
            )
            raise(AddressBookCryptoError.DecryptionFailed)
        }

        runCatching {
            json.decodeFromString(AddressBook.serializer(), plaintext.toString(Charsets.UTF_8))
        }.getOrElse { error ->
            // Decryption succeeded, so this is a payload schema/format mismatch — the prime suspect for a book

            // reason (missing/extra field, wrong type, JSON path) but strips any raw plaintext the exception echoes.
            logger.e(
                "Failed to parse decrypted address book for wallet ${blob.walletId} " +
                    "(plaintextBytes=${plaintext.size}): ${error.safeDescription()}. " +
                    "Decryption OK → cross-platform payload schema/format mismatch.",
            )
            raise(AddressBookCryptoError.MalformedBlob)
        }.also { addressBook ->
            logger.i(
                messageString = "Decrypted address book for wallet ${blob.walletId}: " +
                    "contacts=${addressBook.contacts.size}",
            )
        }
    }

    private fun Raise<AddressBookCryptoError>.raiseNonHex(
        blob: AddressBookBlob,
        field: String,
        value: String,
    ): Nothing {
        logger.e(
            "Address book blob has non-hex $field for wallet ${blob.walletId} " +
                "(${field}Len=${value.length}).",
        )
        raise(AddressBookCryptoError.DecryptionFailed)
    }

    /**
     * A log-safe, one-line description of a failure: exception type + message, with any raw decrypted payload
     * stripped. kotlinx.serialization appends the offending input after a `JSON input:` marker; everything before
     * it (the reason and the `at path: …` location) is structural and safe to log.
     */
    private fun Throwable.safeDescription(): String {
        val safeMessage = message.orEmpty().substringBefore("JSON input:").trim()
        return buildString {
            append(this@safeDescription::class.simpleName)
            if (safeMessage.isNotEmpty()) append(": ").append(safeMessage)
        }
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
        const val LOG_TAG = "AddressBook"
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_ALGORITHM = "AES"
        const val NONCE_SIZE_BYTES = 12
        const val TAG_SIZE_BYTES = 16
        const val TAG_SIZE_BITS = TAG_SIZE_BYTES * 8
    }
}