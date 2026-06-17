package com.tangem.domain.addressbook.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.VerifyMessagesError
import com.tangem.domain.transaction.usecase.VerifySecp256k1MessagesUseCase
import com.tangem.utils.extensions.toHexString
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerifyAddressEntriesUseCaseTest {

    private val verifyMessagesUseCase: VerifySecp256k1MessagesUseCase = mockk()
    private val useCase = VerifyAddressEntriesUseCase(verifyMessagesUseCase = verifyMessagesUseCase)

    private val userWallet: UserWallet = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(verifyMessagesUseCase)
    }

    @Test
    fun `GIVEN contact with entries WHEN invoke THEN verifies each entry payload and its signature`() {
        // Arrange
        val contact = contact(
            entry(id = "addr-1", address = "0xabc", memo = "memo", signature = "AABB"),
            entry(id = "addr-2", address = "0xdef", memo = null, signature = "CCDD"),
        )
        val messagesSlot = slot<List<ByteArray>>()
        val signaturesSlot = slot<List<ByteArray>>()
        every {
            verifyMessagesUseCase(eq(userWallet), capture(messagesSlot), capture(signaturesSlot))
        } returns listOf(true, true).right()

        // Act
        val result = useCase(userWallet, contact)

        // Assert
        // Each entry is verified against address + networkId + memo + contactId + name
        assertThat(messagesSlot.captured.map { String(it) })
            .containsExactly(
                expectedPayload(contact, contact.addressEntries[0]),
                expectedPayload(contact, contact.addressEntries[1]),
            )
            .inOrder()
        // Hex signatures are decoded to bytes, in entry order
        assertThat(signaturesSlot.captured.map { it.toHexString() }).containsExactly("AABB", "CCDD").inOrder()
    }

    @Test
    fun `GIVEN some entries fail verification WHEN invoke THEN partitions them preserving order`() {
        // Arrange
        val valid1 = entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB")
        val invalid = entry(id = "addr-2", address = "0xdef", memo = null, signature = "CCDD")
        val valid2 = entry(id = "addr-3", address = "0xghi", memo = null, signature = "EEFF")
        val contact = contact(valid1, invalid, valid2)
        every { verifyMessagesUseCase(any(), any(), any()) } returns listOf(true, false, true).right()

        // Act
        val result = useCase(userWallet, contact).getOrNull()

        // Assert
        assertThat(result!!.valid).containsExactly(valid1, valid2).inOrder()
        assertThat(result.invalid).containsExactly(invalid)
        assertThat(result.areAllInvalid).isFalse()
    }

    @Test
    fun `GIVEN malformed signature WHEN invoke THEN that entry is invalid and excluded from verification`() {
        // Arrange
        val malformed = entry(id = "addr-1", address = "0xabc", memo = null, signature = "not-hex")
        val signed = entry(id = "addr-2", address = "0xdef", memo = null, signature = "AABB")
        val contact = contact(malformed, signed)
        val signaturesSlot = slot<List<ByteArray>>()
        every {
            verifyMessagesUseCase(eq(userWallet), any(), capture(signaturesSlot))
        } returns listOf(true).right()

        // Act
        val result = useCase(userWallet, contact).getOrNull()

        // Assert
        // Only the well-formed entry is passed to verification
        assertThat(signaturesSlot.captured.map { it.toHexString() }).containsExactly("AABB")
        assertThat(result!!.valid).containsExactly(signed)
        assertThat(result.invalid).containsExactly(malformed)
    }

    @Test
    fun `GIVEN every entry is invalid WHEN invoke THEN allInvalid is true`() {
        // Arrange
        val entry1 = entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB")
        val entry2 = entry(id = "addr-2", address = "0xdef", memo = null, signature = "CCDD")
        val contact = contact(entry1, entry2)
        every { verifyMessagesUseCase(any(), any(), any()) } returns listOf(false, false).right()

        // Act
        val result = useCase(userWallet, contact).getOrNull()

        // Assert
        assertThat(result!!.valid).isEmpty()
        assertThat(result.invalid).containsExactly(entry1, entry2).inOrder()
        assertThat(result.areAllInvalid).isTrue()
    }

    @Test
    fun `GIVEN contact with no entries WHEN invoke THEN returns empty partition without verifying`() {
        // Arrange
        val contact = contact()

        // Act
        val result = useCase(userWallet, contact).getOrNull()

        // Assert
        assertThat(result!!.valid).isEmpty()
        assertThat(result.invalid).isEmpty()
        assertThat(result.areAllInvalid).isFalse()
        verify(exactly = 0) { verifyMessagesUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN verifyMessagesUseCase returns error WHEN invoke THEN propagates the error`() {
        // Arrange
        val contact = contact(entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB"))
        every { verifyMessagesUseCase(any(), any(), any()) } returns VerifyMessagesError.NoSigningKey.left()

        // Act
        val result = useCase(userWallet, contact)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VerifyMessagesError.NoSigningKey)
    }

    private fun contact(vararg entries: AddressEntry): Contact = Contact(
        id = ContactId("contact-1"),
        walletId = UserWalletId("011"),
        name = requireNotNull(ContactName("Alice").getOrNull()),
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        addressEntries = entries.toList(),
    )

    private fun entry(id: String, address: String, memo: String?, signature: String): AddressEntry = AddressEntry(
        id = AddressEntryId(id),
        address = address,
        networkId = Network.RawID("ethereum"),
        memo = memo,
        signature = signature,
    )

    private fun expectedPayload(contact: Contact, entry: AddressEntry): String =
        entry.address + entry.networkId.value + entry.memo.orEmpty() + contact.id.value + contact.name.value
}