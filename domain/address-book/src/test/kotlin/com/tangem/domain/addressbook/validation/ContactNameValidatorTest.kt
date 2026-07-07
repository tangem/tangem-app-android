package com.tangem.domain.addressbook.validation

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.error.ContactNameValidationError
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
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactNameValidatorTest {

    private val repository: AddressBookRepository = mockk()
    private val contactSignatureVerifier: ContactSignatureVerifier = mockk()

    private val validator = ContactNameValidator(
        repository = repository,
        contactSignatureVerifier = contactSignatureVerifier,
    )

    private val walletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, contactSignatureVerifier)
    }

    @Test
    fun `GIVEN blank name WHEN validate THEN format error is propagated`() = runTest {
        // Act
        val result = validator.validate(walletId, name = "")

        // Assert
        assertThat(result.leftOrNull())
            .isEqualTo(ContactNameValidationError.Format(ContactName.Error.Empty))
    }

    @Test
    fun `GIVEN same-name verified contact WHEN validate THEN Duplicate rejected case-insensitively`() = runTest {
        // Arrange
        coEvery { repository.getContactsSync(walletId) } returns listOf(contact(name = "Alice"))
        coEvery { contactSignatureVerifier.isNameVerified(any()) } returns true

        // Act
        val result = validator.validate(walletId, name = "alice")

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(ContactNameValidationError.Duplicate)
    }

    @Test
    fun `GIVEN same-name but unverified spoofed contact WHEN validate THEN name is accepted`() = runTest {
        // Arrange — a contact synced from another device whose signature does not verify must not reserve a name
        coEvery { repository.getContactsSync(walletId) } returns listOf(contact(name = "Alice"))
        coEvery { contactSignatureVerifier.isNameVerified(any()) } returns false

        // Act
        val result = validator.validate(walletId, name = "alice")

        // Assert
        assertThat(result.getOrNull()?.value).isEqualTo("alice")
    }

    @Test
    fun `GIVEN no same-name contacts WHEN validate THEN accepted without verifying`() = runTest {
        // Arrange
        coEvery { repository.getContactsSync(walletId) } returns listOf(contact(name = "Alice"))

        // Act
        val result = validator.validate(walletId, name = "Bob")

        // Assert
        assertThat(result.getOrNull()?.value).isEqualTo("Bob")
        coVerify(exactly = 0) { contactSignatureVerifier.isNameVerified(any()) }
    }

    private fun contact(name: String): Contact = Contact(
        id = ContactId("id-$name"),
        walletId = walletId,
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = "",
        iconColor = "KekColor",
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        addresses = listOf(
            AddressEntry(
                id = AddressEntryId("addr-$name"),
                address = "0xabc",
                networkId = Network.RawID("ethereum"),
                memo = null,
                signature = "AABB",
            ),
        ),
    )
}