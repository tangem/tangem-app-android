package com.tangem.domain.addressbook.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetContactsUseCaseTest {

    private val repository: AddressBookRepository = mockk()
    private val useCase = GetContactsUseCase(repository)

    private val alice = contact(name = "Alice", address = "0xaaa")
    private val bob = contact(name = "Bob", address = "0xbbb")

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
        every { repository.getAllContacts() } returns flowOf(listOf(alice, bob))
    }

    @Test
    fun `GIVEN query matches a name WHEN invoke THEN returns only matching contacts`() = runTest {
        // Act
        val result = useCase(query = "ali").first()

        // Assert
        assertThat(result).containsExactly(alice)
    }

    @Test
    fun `GIVEN query matches an address WHEN invoke THEN returns only matching contacts`() = runTest {
        // Act
        val result = useCase(query = "0xbbb").first()

        // Assert
        assertThat(result).containsExactly(bob)
    }

    @Test
    fun `GIVEN EVM address query differing only in case WHEN invoke THEN returns matching contact`() = runTest {
        // Arrange — EVM (ethereum) addresses are case-insensitive, so a lowercased query matches a checksummed address
        val carol = contact(name = "Carol", address = "0xAbCdEf", networkId = "ethereum")
        every { repository.getAllContacts() } returns flowOf(listOf(alice, carol))

        // Act
        val result = useCase(query = "0xabcdef").first()

        // Assert
        assertThat(result).containsExactly(carol)
    }

    @Test
    fun `GIVEN non-EVM address query differing only in case WHEN invoke THEN returns empty`() = runTest {
        // Arrange — non-EVM (solana) addresses are case-sensitive, so a differently-cased query must not match
        val dave = contact(name = "Dave", address = "SoLAnaAddr", networkId = "solana")
        every { repository.getAllContacts() } returns flowOf(listOf(alice, dave))

        // Act
        val result = useCase(query = "solanaaddr").first()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN non-EVM address query with exact case WHEN invoke THEN returns matching contact`() = runTest {
        // Arrange
        val dave = contact(name = "Dave", address = "SoLAnaAddr", networkId = "solana")
        every { repository.getAllContacts() } returns flowOf(listOf(alice, dave))

        // Act
        val result = useCase(query = "SoLAnaAddr").first()

        // Assert
        assertThat(result).containsExactly(dave)
    }

    @Test
    fun `GIVEN query matches a network id WHEN invoke THEN returns only contacts on that network`() = runTest {
        // Arrange
        val ethContact = contact(name = "Eth", address = "0x1", networkId = "ethereum")
        val tronContact = contact(name = "Tron", address = "T1", networkId = "tron")
        every { repository.getAllContacts() } returns flowOf(listOf(ethContact, tronContact))

        // Act
        val result = useCase(query = "tron").first()

        // Assert
        assertThat(result).containsExactly(tronContact)
    }

    @Test
    fun `GIVEN network query with different case WHEN invoke THEN returns matching contact`() = runTest {
        // Arrange
        val tronContact = contact(name = "Tron", address = "T1", networkId = "tron")
        every { repository.getAllContacts() } returns flowOf(listOf(alice, tronContact))

        // Act
        val result = useCase(query = "TRON").first()

        // Assert
        assertThat(result).containsExactly(tronContact)
    }

    @Test
    fun `GIVEN name query with different case WHEN invoke THEN returns matching contact`() = runTest {
        // Act
        val result = useCase(query = "ALICE").first()

        // Assert
        assertThat(result).containsExactly(alice)
    }

    @Test
    fun `GIVEN blank query WHEN invoke THEN returns all contacts unfiltered`() = runTest {
        // Act
        val result = useCase(query = "   ").first()

        // Assert
        assertThat(result).containsExactly(alice, bob)
    }

    @Test
    fun `GIVEN query matches nothing WHEN invoke THEN returns empty list`() = runTest {
        // Act
        val result = useCase(query = "charlie").first()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN contacts with different createdAt WHEN invoke THEN sorted newest first`() = runTest {
        // Arrange
        val older = contact(name = "Older", address = "0x1", createdAt = "2026-01-01T00:00:00.000Z")
        val newer = contact(name = "Newer", address = "0x2", createdAt = "2026-06-01T00:00:00.000Z")
        every { repository.getAllContacts() } returns flowOf(listOf(older, newer))

        // Act
        val result = useCase(query = "").first()

        // Assert
        assertThat(result).containsExactly(newer, older).inOrder()
    }

    @Test
    fun `GIVEN userWalletId WHEN invoke THEN reads single wallet contacts AND not all contacts`() = runTest {
        // Arrange
        val walletId = UserWalletId("011")
        every { repository.getContacts(walletId) } returns flowOf(listOf(alice))

        // Act
        val result = useCase(query = "", userWalletId = walletId).first()

        // Assert
        assertThat(result).containsExactly(alice)
        verify(exactly = 1) { repository.getContacts(walletId) }
        verify(exactly = 0) { repository.getAllContacts() }
    }

    private fun contact(
        name: String,
        address: String,
        createdAt: String = "2026-01-01T00:00:00.000Z",
        networkId: String = "ethereum",
    ): Contact = Contact(
        id = ContactId("id-$name"),
        walletId = UserWalletId("011"),
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = "",
        iconColor = "KekColor",
        createdAt = createdAt,
        updatedAt = "2026-01-01T00:00:00.000Z",
        addresses = listOf(
            AddressEntry(
                id = AddressEntryId("addr-$name"),
                address = address,
                networkId = Network.RawID(networkId),
                memo = null,
                signature = "sig",
            ),
        ),
    )
}