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

    private fun contact(name: String, address: String): Contact = Contact(
        id = ContactId("id-$name"),
        walletId = UserWalletId("011"),
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = "",
        iconColor = "KekColor",
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        addressEntries = listOf(
            AddressEntry(
                id = AddressEntryId("addr-$name"),
                address = address,
                networkId = Network.RawID("ethereum"),
                memo = null,
                signature = "sig",
            ),
        ),
    )
}