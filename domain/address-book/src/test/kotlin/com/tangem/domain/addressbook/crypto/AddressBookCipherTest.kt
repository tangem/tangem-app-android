package com.tangem.domain.addressbook.crypto

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.addressbook.error.AddressBookCryptoError
import com.tangem.domain.addressbook.model.AddressBook
import com.tangem.domain.addressbook.model.AddressBookBlob
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.extensions.toHexString
import arrow.core.Either
import io.mockk.every
import io.mockk.mockk
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Test
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class AddressBookCipherTest {

    private val cipher = AddressBookCipher()

    private val wallet: UserWallet.Cold = MockUserWalletFactory.create()
    private val updatedAt: DateTime = DateTime.parse("2026-05-22T09:00:00.000Z")

    @Test
    fun `GIVEN multi-contact book WHEN encrypt then decrypt THEN original book is restored`() {
        // Arrange
        val book = addressBook(
            contact(
                name = "Alice",
                iconColor = "TestColor1",
                entries = arrayOf(entry("addr-1", "0xabc", memo = "memo")),
            ),
            contact(
                name = "Bob",
                iconColor = "TestColor2",
                entries = arrayOf(entry("addr-2", "0xdef", memo = null)),
            ),
        )

        // Act
        val blob = cipher.encrypt(book, wallet, updatedAt).rightValue()
        val decrypted = cipher.decrypt(blob, wallet).rightValue()

        // Assert
        assertThat(decrypted).isEqualTo(book)
    }

    @Test
    fun `GIVEN empty book WHEN encrypt then decrypt THEN empty book is restored`() {
        // Arrange
        val book = addressBook()

        // Act
        val blob = cipher.encrypt(book, wallet, updatedAt).rightValue()
        val decrypted = cipher.decrypt(blob, wallet).rightValue()

        // Assert
        assertThat(decrypted).isEqualTo(book)
    }

    @Test
    fun `GIVEN a book WHEN encrypt THEN blob metadata and field sizes match the spec`() {
        // Arrange
        val book = addressBook(
            contact(
                name = "Alice",
                iconColor = "TestColor",
                entries = arrayOf(entry("addr-1", "0xabc", memo = null)),
            )
        )

        // Act
        val blob = cipher.encrypt(book, wallet, updatedAt).rightValue()

        // Assert
        assertThat(blob.version).isEqualTo(AddressBookBlob.CURRENT_VERSION)
        assertThat(blob.walletId).isEqualTo(wallet.walletId.stringValue)
        assertThat(blob.updatedAt).isEqualTo("2026-05-22T09:00:00.000Z")
        assertThat(blob.nonce).hasLength(NONCE_HEX_LENGTH) // 12 bytes
        assertThat(blob.authTag).hasLength(TAG_HEX_LENGTH) // 16 bytes
        assertThat(blob.ciphertext).isNotEmpty()
    }

    @Test
    fun `GIVEN updatedAt in a non-UTC zone WHEN encrypt THEN it is normalized to UTC ISO-8601`() {
        // Arrange
        val book = addressBook()
        val nonUtc = DateTime.parse("2026-05-22T09:00:00.000Z").withZone(DateTimeZone.forOffsetHours(3))

        // Act
        val blob = cipher.encrypt(book, wallet, nonUtc).rightValue()

        // Assert
        assertThat(blob.updatedAt).isEqualTo("2026-05-22T09:00:00.000Z")
    }

    @Test
    fun `GIVEN same book encrypted twice WHEN compared THEN nonce differs but both decrypt to original`() {
        // Arrange
        val book = addressBook(
            contact(
                name = "Alice",
                iconColor = "TestColor",
                entries = arrayOf(entry("addr-1", "0xabc", memo = null)),
            )
        )

        // Act
        val first = cipher.encrypt(book, wallet, updatedAt).rightValue()
        val second = cipher.encrypt(book, wallet, updatedAt).rightValue()

        // Assert
        assertThat(first.nonce).isNotEqualTo(second.nonce)
        assertThat(first.ciphertext).isNotEqualTo(second.ciphertext)
        assertThat(cipher.decrypt(first, wallet).rightValue()).isEqualTo(book)
        assertThat(cipher.decrypt(second, wallet).rightValue()).isEqualTo(book)
    }

    @Test
    fun `GIVEN book whose walletId differs from the wallet WHEN encrypt THEN WalletMismatch`() {
        // Arrange
        val book = addressBook().copy(walletId = UserWalletId("deadbeef"))

        // Act
        val result = cipher.encrypt(book, wallet, updatedAt)

        // Assert
        assertThat(result.leftValue()).isEqualTo(AddressBookCryptoError.WalletMismatch)
    }

    @Test
    fun `GIVEN blob WHEN decrypt with a wallet of different id THEN WalletMismatch`() {
        // Arrange
        val blob = cipher.encrypt(addressBook(), wallet, updatedAt).rightValue()
        val otherWallet = wallet.copy(walletId = UserWalletId("deadbeef"))

        // Act
        val result = cipher.decrypt(blob, otherWallet)

        // Assert
        assertThat(result.leftValue()).isEqualTo(AddressBookCryptoError.WalletMismatch)
    }

    @Test
    fun `GIVEN blob with tampered ciphertext WHEN decrypt THEN DecryptionFailed`() {
        // Arrange
        val blob = cipher.encrypt(addressBook(), wallet, updatedAt).rightValue()
        val tampered = blob.copy(ciphertext = blob.ciphertext.flipFirstHexNibble())

        // Act
        val result = cipher.decrypt(tampered, wallet)

        // Assert
        assertThat(result.leftValue()).isEqualTo(AddressBookCryptoError.DecryptionFailed)
    }

    @Test
    fun `GIVEN blob with tampered auth tag WHEN decrypt THEN DecryptionFailed`() {
        // Arrange
        val blob = cipher.encrypt(addressBook(), wallet, updatedAt).rightValue()
        val tampered = blob.copy(authTag = blob.authTag.flipFirstHexNibble())

        // Act
        val result = cipher.decrypt(tampered, wallet)

        // Assert
        assertThat(result.leftValue()).isEqualTo(AddressBookCryptoError.DecryptionFailed)
    }

    @Test
    fun `GIVEN blob with non-hex nonce WHEN decrypt THEN DecryptionFailed`() {
        // Arrange
        val blob = cipher.encrypt(addressBook(), wallet, updatedAt).rightValue()
        val tampered = blob.copy(nonce = "zzzz")

        // Act
        val result = cipher.decrypt(tampered, wallet)

        // Assert
        assertThat(result.leftValue()).isEqualTo(AddressBookCryptoError.DecryptionFailed)
    }

    @Test
    fun `GIVEN valid blob holding non-AddressBook plaintext WHEN decrypt THEN MalformedBlob`() {
        // Arrange — encrypt arbitrary bytes with the real derived key so only the payload is wrong
        val blob = encryptRawPayload(plaintext = "{}".toByteArray(Charsets.UTF_8), userWallet = wallet)

        // Act
        val result = cipher.decrypt(blob, wallet)

        // Assert
        assertThat(result.leftValue()).isEqualTo(AddressBookCryptoError.MalformedBlob)
    }

    @Test
    fun `GIVEN locked wallet without a public key WHEN encrypt THEN NoWalletPublicKey`() {
        // Arrange — a Cold wallet whose card exposes no wallets (locked), so no key can be derived
        val lockedWallet = mockk<UserWallet.Cold> {
            every { walletId } returns wallet.walletId
            every { scanResponse } returns mockk {
                every { card } returns mockk { every { wallets } returns emptyList() }
            }
        }
        val book = addressBook(walletId = wallet.walletId)

        // Act
        val result = cipher.encrypt(book, lockedWallet, updatedAt)

        // Assert
        assertThat(result.leftValue()).isEqualTo(AddressBookCryptoError.NoWalletPublicKey)
    }

    // region cross-platform vectors
    // Shared known-answer vector, identical to iOS CommonAddressBookEncryptionServiceTests. Asserting the
    // same bytes on both platforms guarantees a blob sealed on one opens on the other. Do not change these
    // constants without changing the iOS suite in lockstep.

    @Test
    fun `GIVEN shared cross-platform public key WHEN deriveAesKey THEN matches the iOS vector`() {
        // Arrange
        val publicKey = VECTOR_PUBLIC_KEY_HEX.hexToBytes()

        // Act
        val aesKey = AddressBookKeyDerivation.deriveAesKey(publicKey)

        // Assert
        assertThat(aesKey).hasLength(AES_256_KEY_BYTES)
        assertThat(aesKey.toHexString().lowercase()).isEqualTo(VECTOR_KEY_HEX)
    }

    @Test
    fun `GIVEN a blob sealed on the other platform WHEN decrypt with the derived key THEN restores the plaintext`() {
        // Arrange — open the iOS-produced AES-256-GCM box with the key derived from the shared seed
        val aesKey = AddressBookKeyDerivation.deriveAesKey(VECTOR_PUBLIC_KEY_HEX.hexToBytes())

        // Act
        val plaintext = aesGcmOpen(
            key = aesKey,
            nonce = VECTOR_NONCE_HEX.hexToBytes(),
            ciphertext = VECTOR_CIPHERTEXT_HEX.hexToBytes(),
            authTag = VECTOR_TAG_HEX.hexToBytes(),
        )

        // Assert
        assertThat(plaintext.toString(Charsets.UTF_8)).isEqualTo(VECTOR_PLAINTEXT)
    }
    // endregion

    // region helpers
    private fun addressBook(vararg contacts: Contact): AddressBook =
        AddressBook(walletId = wallet.walletId, contacts = contacts.toList())

    private fun addressBook(walletId: UserWalletId): AddressBook =
        AddressBook(walletId = walletId, contacts = emptyList())

    private fun contact(name: String, iconColor: String, vararg entries: AddressEntry): Contact = Contact(
        id = ContactId("contact-$name"),
        walletId = wallet.walletId,
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = "",
        iconColor = iconColor,
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-05-22T09:00:00.000Z",
        addressEntries = entries.toList(),
    )

    private fun entry(id: String, address: String, memo: String?): AddressEntry = AddressEntry(
        id = AddressEntryId(id),
        address = address,
        networkId = Network.RawID("ethereum"),
        memo = memo,
        signature = "",
        networkName = "Ethereum",
    )

    private fun String.flipFirstHexNibble(): String = (if (first() == '0') '1' else '0') + substring(1)

    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    /** Raw AES-256-GCM open, mirroring [AddressBookCipher]'s transformation and tag size. */
    private fun aesGcmOpen(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, authTag: ByteArray): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        }.doFinal(ciphertext + authTag)

    private fun <T> Either<AddressBookCryptoError, T>.rightValue(): T =
        getOrNull() ?: error("Expected Either.Right but was $this")

    private fun <T> Either<AddressBookCryptoError, T>.leftValue(): AddressBookCryptoError =
        leftOrNull() ?: error("Expected Either.Left but was $this")

    /** Mirrors [AddressBookCipher.encrypt] but accepts arbitrary plaintext, to forge a decryptable-but-malformed blob. */
    private fun encryptRawPayload(plaintext: ByteArray, userWallet: UserWallet): AddressBookBlob {
        val aesKey = AddressBookKeyDerivation.deriveAesKey(AddressBookKeyDerivation.walletPublicKey(userWallet)!!)
        val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        val cipherWithTag = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        }.doFinal(plaintext)
        val tagOffset = cipherWithTag.size - TAG_BYTES
        return AddressBookBlob(
            walletId = userWallet.walletId.stringValue,
            updatedAt = updatedAt.toString(),
            nonce = nonce.toHexString().lowercase(),
            ciphertext = cipherWithTag.copyOfRange(0, tagOffset).toHexString().lowercase(),
            authTag = cipherWithTag.copyOfRange(tagOffset, cipherWithTag.size).toHexString().lowercase(),
        )
    }
    // endregion

    private companion object {
        const val NONCE_BYTES = 12
        const val TAG_BYTES = 16
        const val TAG_BITS = TAG_BYTES * 8
        const val NONCE_HEX_LENGTH = NONCE_BYTES * 2
        const val TAG_HEX_LENGTH = TAG_BYTES * 2
        const val AES_256_KEY_BYTES = 32

        // Shared cross-platform known-answer vector (see iOS CommonAddressBookEncryptionServiceTests).
        const val VECTOR_PUBLIC_KEY_HEX = "0374d0f81f42ddfe34114d533e95e6ae5fe6ea271c96f1fa505199fdc365ae9720"
        const val VECTOR_KEY_HEX = "59b85ce53fac0a8493d9d8d9c0d32adb5f586741dd8bbfd9348a3212e493730d"
        const val VECTOR_NONCE_HEX = "000102030405060708090a0b"
        const val VECTOR_CIPHERTEXT_HEX = "f4ee0f404e747b5b5cca730c44baf86ca3d8f6fbdf66ff2fe98d3b8f88cb23df7ff55b52205f32c8ab"
        const val VECTOR_TAG_HEX = "6c4b71b27958f43afc6633850369a17a"
        const val VECTOR_PLAINTEXT = "Tangem Address Book cross-platform vector"
    }
}