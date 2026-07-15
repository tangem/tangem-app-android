package com.tangem.domain.addressbook.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.verification.ContactSignatureVerifier
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetContactByIdUseCaseTest {

    private val repository: AddressBookRepository = mockk()
    private val contactSignatureVerifier: ContactSignatureVerifier = mockk()
    private val useCase = GetContactByIdUseCase(repository, contactSignatureVerifier)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, contactSignatureVerifier)
    }

    @Test
    fun `GIVEN matching id WHEN invoke THEN emits verified contact with only valid addresses`() = runTest {
        // Arrange
        val valid = entry("addr-valid")
        val invalid = entry("addr-invalid")
        val stored = contact("id-2", "Bob", valid, invalid)
        val verified = stored.copy(addresses = listOf(valid))
        every { repository.getAllContacts() } returns flowOf(listOf(contact("id-1", "Alice"), stored))
        coEvery { contactSignatureVerifier.verifyContacts(listOf(stored)) } returns listOf(verified)

        // Act
        val result = useCase(ContactId("id-2")).first()

        // Assert
        assertThat(result).isEqualTo(verified)
    }

    @Test
    fun `GIVEN contact has no verified addresses WHEN invoke THEN emits null`() = runTest {
        // Arrange
        val stored = contact("id-2", "Bob", entry("addr-invalid"))
        every { repository.getAllContacts() } returns flowOf(listOf(stored))
        coEvery { contactSignatureVerifier.verifyContacts(listOf(stored)) } returns emptyList()

        // Act
        val result = useCase(ContactId("id-2")).first()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN no matching id WHEN invoke THEN emits null without verifying`() = runTest {
        // Arrange
        every { repository.getAllContacts() } returns flowOf(listOf(contact("id-1", "Alice")))

        // Act
        val result = useCase(ContactId("missing")).first()

        // Assert
        assertThat(result).isNull()
        coVerify(exactly = 0) { contactSignatureVerifier.verifyContacts(any()) }
    }

    private fun contact(id: String, name: String, vararg addresses: AddressEntry): Contact = Contact(
        id = ContactId(id),
        walletId = UserWalletId("0001"),
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = "",
        iconColor = "Azure",
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        addresses = addresses.toList(),
    )

    private fun entry(id: String): AddressEntry = AddressEntry(
        id = AddressEntryId(id),
        address = "0x$id",
        networkId = Network.RawID("ethereum"),
        memo = null,
        signature = "AABB",
    )
}