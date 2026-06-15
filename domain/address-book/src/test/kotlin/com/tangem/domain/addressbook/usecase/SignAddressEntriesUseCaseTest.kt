package com.tangem.domain.addressbook.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SignHashesError
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.utils.extensions.toHexString
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.security.MessageDigest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SignAddressEntriesUseCaseTest {

    private val signUseCase: SignUseCase = mockk()
    private val useCase = SignAddressEntriesUseCase(signUseCase = signUseCase)

    // The mock factory builds each wallet key with publicKey = curve.name bytes, so the secp256k1 key is "Secp256k1"
    private val userWallet: UserWallet = MockUserWalletFactory.create()
    private val secp256k1Key = "Secp256k1".toByteArray()

    @BeforeEach
    fun resetMocks() {
        clearMocks(signUseCase)
    }

    @Test
    fun `GIVEN contact with entries WHEN invoke THEN every entry receives its signature`() = runTest {
        // Arrange
        val contact = contact(
            entry(id = "addr-1", address = "0xabc", memo = "memo"),
            entry(id = "addr-2", address = "0xdef", memo = null),
        )
        val signatures = listOf(byteArrayOf(0x01, 0xAB.toByte()), byteArrayOf(0xCD.toByte()))
        val hashesSlot = slot<List<ByteArray>>()
        val publicKeySlot = slot<ByteArray>()
        coEvery {
            signUseCase(hashes = capture(hashesSlot), publicKey = capture(publicKeySlot), userWallet = eq(userWallet))
        } returns signatures.right()

        // Act
        val result = useCase(userWallet, contact)

        // Assert
        // Signatures are applied in entry order, hex-encoded; all other fields are preserved
        val expected = contact.copy(
            addressEntries = listOf(
                contact.addressEntries[0].copy(signature = signatures[0].toHexString()),
                contact.addressEntries[1].copy(signature = signatures[1].toHexString()),
            ),
        )
        assertThat(result.getOrNull()).isEqualTo(expected)
        // The wallet's primary secp256k1 key is the one signing
        assertThat(publicKeySlot.captured).isEqualTo(secp256k1Key)
        // Each entry is hashed as SHA-256(address + networkId + memo + contactId + name), in order
        assertThat(hashesSlot.captured.map { it.toHexString() })
            .containsExactly(
                expectedHash(contact, contact.addressEntries[0]).toHexString(),
                expectedHash(contact, contact.addressEntries[1]).toHexString(),
            )
            .inOrder()
    }

    @Test
    fun `GIVEN contact with no entries WHEN invoke THEN returns contact unchanged without signing`() = runTest {
        // Arrange
        val contact = contact()

        // Act
        val result = useCase(userWallet, contact)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(contact)
        coVerify(exactly = 0) { signUseCase(any<List<ByteArray>>(), any(), any()) }
    }

    @Test
    fun `GIVEN wallet without a secp256k1 key WHEN invoke THEN returns NoSigningKey without signing`() = runTest {
        // Arrange — a locked hot wallet exposes no key
        val lockedWallet = mockk<UserWallet.Hot> { every { wallets } returns null }
        val contact = contact(entry(id = "addr-1", address = "0xabc", memo = null))

        // Act
        val result = useCase(lockedWallet, contact)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(SignHashesError.NoSigningKey)
        coVerify(exactly = 0) { signUseCase(any<List<ByteArray>>(), any(), any()) }
    }

    @Test
    fun `GIVEN signUseCase returns error WHEN invoke THEN propagates the error`() = runTest {
        // Arrange
        val contact = contact(entry(id = "addr-1", address = "0xabc", memo = null))
        coEvery { signUseCase(any<List<ByteArray>>(), any(), any()) } returns
            SignHashesError.SigningFailed(message = "canceled").left()

        // Act
        val result = useCase(userWallet, contact)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(SignHashesError.SigningFailed(message = "canceled"))
    }

    private fun contact(vararg entries: AddressEntry): Contact = Contact(
        id = ContactId("contact-1"),
        walletId = UserWalletId("011"),
        name = requireNotNull(ContactName("Alice").getOrNull()),
        addressEntries = entries.toList(),
    )

    private fun entry(id: String, address: String, memo: String?): AddressEntry = AddressEntry(
        id = AddressEntryId(id),
        address = address,
        networkId = Network.RawID("ethereum"),
        memo = memo,
        signature = "",
    )

    private fun expectedHash(contact: Contact, entry: AddressEntry): ByteArray {
        val payload = entry.address + entry.networkId.value + entry.memo.orEmpty() +
            contact.id.value + contact.name.value
        return MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
    }
}