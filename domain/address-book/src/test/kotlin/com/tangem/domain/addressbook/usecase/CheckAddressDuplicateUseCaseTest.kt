package com.tangem.domain.addressbook.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.model.*
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.verification.ContactSignatureVerifier
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckAddressDuplicateUseCaseTest {

    private val repository: AddressBookRepository = mockk()
    private val contactSignatureVerifier: ContactSignatureVerifier = mockk()
    private val useCase = CheckAddressDuplicateUseCase(repository, contactSignatureVerifier)

    private val walletId = UserWalletId("0001")

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, contactSignatureVerifier)
        // Default: every stored address verifies, so the use case sees the contacts unchanged.
        coEvery { contactSignatureVerifier.verifyContacts(any()) } answers { firstArg<List<Contact>>() }
    }

    @Test
    fun `GIVEN network and address already saved WHEN invoke THEN returns owning contact name`() = runTest {
        // Arrange
        coEvery { repository.getContactsSync(walletId) } returns listOf(contact("Binance", "0xAAA", ETHEREUM))

        // Act
        val result = useCase(walletId, networkId = ETHEREUM, address = "0xAAA")

        // Assert
        assertThat(result).isEqualTo("Binance")
    }

    @Test
    fun `GIVEN same address in a different network WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { repository.getContactsSync(walletId) } returns listOf(contact("Binance", "0xAAA", ETHEREUM))

        // Act
        val result = useCase(walletId, networkId = TRON, address = "0xAAA")

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN the pair belongs to the excluded contact WHEN invoke THEN returns null`() = runTest {
        // Arrange
        val contact = contact("Binance", "0xAAA", ETHEREUM, id = "id-1")
        coEvery { repository.getContactsSync(walletId) } returns listOf(contact)

        // Act
        val result = useCase(walletId, networkId = ETHEREUM, address = "0xAAA", excludeContactId = ContactId("id-1"))

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN free pair WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { repository.getContactsSync(walletId) } returns listOf(contact("Binance", "0xAAA", ETHEREUM))

        // Act
        val result = useCase(walletId, networkId = ETHEREUM, address = "0xBBB")

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN the pair belongs only to an unverified entry WHEN invoke THEN returns null`() = runTest {
        // Arrange
        val contact = contact("Binance", "0xAAA", ETHEREUM)
        coEvery { repository.getContactsSync(walletId) } returns listOf(contact)
        // Verification strips the unverified address, so the pair is no longer held by any contact.
        coEvery { contactSignatureVerifier.verifyContacts(listOf(contact)) } returns emptyList()

        // Act
        val result = useCase(walletId, networkId = ETHEREUM, address = "0xAAA")

        // Assert
        assertThat(result).isNull()
    }

    private fun contact(name: String, address: String, networkId: String, id: String = "id-$name"): Contact = Contact(
        id = ContactId(id),
        walletId = walletId,
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = "",
        iconColor = "Azure",
        createdAt = "2026-01-01T00:00:00.000Z",
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

    private companion object {
        const val ETHEREUM = "ethereum"
        const val TRON = "tron"
    }
}