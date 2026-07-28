package com.tangem.domain.addressbook.interactor

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.usecase.GetContactsUseCase
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
class GetVerifiedContactsInteractorTest {

    private val getContacts: GetContactsUseCase = mockk()
    private val contactSignatureVerifier: ContactSignatureVerifier = mockk()

    private val interactor = GetVerifiedContactsInteractor(
        getContacts = getContacts,
        contactSignatureVerifier = contactSignatureVerifier,
    )

    private val walletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(getContacts, contactSignatureVerifier)
    }

    @Test
    fun `GIVEN contacts WHEN getVerifiedContacts THEN maps them through the verifier`() = runTest {
        // Arrange
        val contact = contact()
        val verified = contact.copy(addresses = emptyList())
        every { getContacts(query = "query", userWalletId = walletId) } returns flowOf(listOf(contact))
        coEvery { contactSignatureVerifier.verifyContacts(listOf(contact)) } returns listOf(verified)

        // Act
        val result = interactor.getVerifiedContacts(query = "query", userWalletId = walletId).first()

        // Assert
        assertThat(result).containsExactly(verified)
        coVerify(exactly = 1) { contactSignatureVerifier.verifyContacts(listOf(contact)) }
    }

    private fun contact(): Contact = Contact(
        id = ContactId("contact-1"),
        walletId = walletId,
        name = requireNotNull(ContactName("Alice").getOrNull()),
        icon = "",
        iconColor = "KekColor",
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        addresses = listOf(
            AddressEntry(
                id = AddressEntryId("addr-1"),
                address = "0xabc",
                networkId = Network.RawID("ethereum"),
                memo = null,
                signature = "AABB",
            ),
        ),
    )
}