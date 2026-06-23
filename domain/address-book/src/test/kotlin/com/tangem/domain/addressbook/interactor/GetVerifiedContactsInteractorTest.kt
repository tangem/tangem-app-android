package com.tangem.domain.addressbook.interactor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.model.VerifiedContact
import com.tangem.domain.addressbook.usecase.GetContactsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.VerifyMessagesError
import com.tangem.domain.transaction.usecase.VerifySecp256k1MessagesUseCase
import com.tangem.utils.extensions.toHexString
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVerifiedContactsInteractorTest {

    private val getContacts: GetContactsUseCase = mockk()
    private val verifyMessages: VerifySecp256k1MessagesUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val interactor = GetVerifiedContactsInteractor(
        getContacts = getContacts,
        verifyMessages = verifyMessages,
        userWalletsListRepository = userWalletsListRepository,
    )

    private val walletId = UserWalletId("011")
    private val userWallet: UserWallet = mockk { every { walletId } returns this@GetVerifiedContactsInteractorTest.walletId }

    @BeforeEach
    fun resetMocks() {
        clearMocks(getContacts, verifyMessages, userWalletsListRepository)
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWallet)
    }

    @Test
    fun `GIVEN mixed entries WHEN invoke THEN displays only valid AND keeps invalid for analytics`() = runTest {
        // Arrange
        val valid = entry(id = "valid", address = "0xvalid", memo = null, signature = "AABB")
        val invalid = entry(id = "invalid", address = "0xinvalid", memo = null, signature = "CCDD")
        val contact = contact(valid, invalid)
        stubContacts(contact)
        every { verifyMessages(any(), any(), any()) } returns listOf(true, false).right()

        // Act
        val result = interactor(query = "").first()

        // Assert
        assertThat(result).containsExactly(
            VerifiedContact(
                contact = contact.copy(addressEntries = listOf(valid)),
                invalidEntries = listOf(invalid),
            ),
        )
    }

    @Test
    fun `GIVEN contact with entries WHEN invoke THEN verifies each entry payload and its signature`() = runTest {
        // Arrange
        val contact = contact(
            entry(id = "addr-1", address = "0xabc", memo = "memo", signature = "AABB"),
            entry(id = "addr-2", address = "0xdef", memo = null, signature = "CCDD"),
        )
        stubContacts(contact)
        val messagesSlot = slot<List<ByteArray>>()
        val signaturesSlot = slot<List<ByteArray>>()
        every {
            verifyMessages(eq(userWallet), capture(messagesSlot), capture(signaturesSlot))
        } returns listOf(true, true).right()

        // Act
        interactor(query = "").first()

        // Assert
        assertThat(messagesSlot.captured.map { String(it) })
            .containsExactly(
                expectedPayload(contact, contact.addressEntries[0]),
                expectedPayload(contact, contact.addressEntries[1]),
            )
            .inOrder()
        assertThat(signaturesSlot.captured.map { it.toHexString() }).containsExactly("AABB", "CCDD").inOrder()
    }

    @Test
    fun `GIVEN some entries fail verification WHEN invoke THEN partitions them preserving order`() = runTest {
        // Arrange
        val valid1 = entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB")
        val invalid = entry(id = "addr-2", address = "0xdef", memo = null, signature = "CCDD")
        val valid2 = entry(id = "addr-3", address = "0xghi", memo = null, signature = "EEFF")
        val contact = contact(valid1, invalid, valid2)
        stubContacts(contact)
        every { verifyMessages(any(), any(), any()) } returns listOf(true, false, true).right()

        // Act
        val result = interactor(query = "").first().single()

        // Assert
        assertThat(result.contact.addressEntries).containsExactly(valid1, valid2).inOrder()
        assertThat(result.invalidEntries).containsExactly(invalid)
    }

    @Test
    fun `GIVEN malformed signature WHEN invoke THEN that entry is invalid and excluded from verification`() = runTest {
        // Arrange
        val malformed = entry(id = "addr-1", address = "0xabc", memo = null, signature = "not-hex")
        val signed = entry(id = "addr-2", address = "0xdef", memo = null, signature = "AABB")
        val contact = contact(malformed, signed)
        stubContacts(contact)
        val signaturesSlot = slot<List<ByteArray>>()
        every {
            verifyMessages(eq(userWallet), any(), capture(signaturesSlot))
        } returns listOf(true).right()

        // Act
        val result = interactor(query = "").first().single()

        // Assert
        assertThat(signaturesSlot.captured.map { it.toHexString() }).containsExactly("AABB")
        assertThat(result.contact.addressEntries).containsExactly(signed)
        assertThat(result.invalidEntries).containsExactly(malformed)
    }

    @Test
    fun `GIVEN contact with no entries WHEN invoke THEN keeps contact without verifying`() = runTest {
        // Arrange
        val contact = contact()
        stubContacts(contact)

        // Act
        val result = interactor(query = "").first().single()

        // Assert
        assertThat(result.contact.addressEntries).isEmpty()
        assertThat(result.invalidEntries).isEmpty()
        verify(exactly = 0) { verifyMessages(any(), any(), any()) }
    }

    @Test
    fun `GIVEN wallet cannot be resolved WHEN invoke THEN contact is dropped`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        stubContacts(contact(entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB")))

        // Act
        val result = interactor(query = "").first()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN verification fails WHEN invoke THEN contact is dropped`() = runTest {
        // Arrange
        stubContacts(contact(entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB")))
        every { verifyMessages(any(), any(), any()) } returns VerifyMessagesError.NoSigningKey.left()

        // Act
        val result = interactor(query = "").first()

        // Assert
        assertThat(result).isEmpty()
    }

    private fun stubContacts(vararg contacts: Contact) {
        every { getContacts(query = "", userWalletId = null) } returns flowOf(contacts.toList())
    }

    private fun contact(vararg entries: AddressEntry): Contact = Contact(
        id = ContactId("contact-1"),
        walletId = walletId,
        name = requireNotNull(ContactName("Alice").getOrNull()),
        icon = "",
        iconColor = "KekColor",
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        addressEntries = entries.toList(),
    )

    private fun entry(id: String, address: String, memo: String?, signature: String): AddressEntry = AddressEntry(
        id = AddressEntryId(id),
        address = address,
        networkId = Network.RawID("ethereum"),
        networkName = "Ethereum",
        memo = memo,
        signature = signature,
    )

    private fun expectedPayload(contact: Contact, entry: AddressEntry): String =
        entry.address + entry.networkId.value + entry.memo.orEmpty() + contact.id.value + contact.name.value
}