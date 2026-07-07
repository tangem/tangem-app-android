package com.tangem.domain.addressbook.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.time.IsoTimestampProvider
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateContactUseCaseTest {

    private val repository: AddressBookRepository = mockk(relaxUnitFun = true)
    private val newTimestamp = "2026-06-10T14:30:00.000Z"
    private val originalTimestamp = "2026-01-01T00:00:00.000Z"
    private val timestampProvider: IsoTimestampProvider = mockk {
        every { now() } returns newTimestamp
    }
    private val useCase = UpdateContactUseCase(
        repository = repository,
        timestampProvider = timestampProvider,
    )

    private val walletId = UserWalletId("011")
    private val networkRawId = Network.RawID("ethereum")

    private val updatedEntries = listOf(
        AddressEntry(
            id = AddressEntryId("addr-new"),
            address = "0xnew",
            networkId = networkRawId,
            memo = "memo",
            signature = "sig2",
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `update preserves id and persists changes without checking uniqueness`() = runTest {
        val existing = contact(name = "Alice")
        val saved = slot<Contact>()
        coEvery { repository.saveContact(capture(saved)) } returns Unit

        val result = useCase(
            contact = existing,
            name = "Bob",
            addressEntries = updatedEntries,
        )

        val contact = result.getOrNull()
        assertThat(contact).isEqualTo(saved.captured)
        assertThat(contact!!.id).isEqualTo(existing.id)
        assertThat(contact.name.value).isEqualTo("Bob")
        assertThat(contact.addressEntries).isEqualTo(updatedEntries)
        assertThat(contact.createdAt).isEqualTo(originalTimestamp) // preserved
        assertThat(contact.updatedAt).isEqualTo(newTimestamp) // restamped
        coVerify(exactly = 0) { repository.getContacts(any<UserWalletId>()) }
    }

    @Test
    fun `invalid name fails without persisting`() = runTest {
        val result = useCase(
            contact = contact(name = "Alice"),
            name = "",
            addressEntries = updatedEntries,
        )

        assertThat(result.leftOrNull())
            .isEqualTo(SaveContactError.Name(ContactNameValidationError.Format(ContactName.Error.Empty)))
        coVerify(exactly = 0) { repository.saveContact(any()) }
    }

    private fun contact(name: String): Contact = Contact(
        id = ContactId("id-$name"),
        walletId = walletId,
        name = requireNotNull(ContactName(name).getOrNull()),
        createdAt = originalTimestamp,
        updatedAt = originalTimestamp,
        addressEntries = listOf(
            AddressEntry(
                id = AddressEntryId("addr-$name"),
                address = "0xabc",
                networkId = networkRawId,
                memo = null,
                signature = "sig",
            ),
        ),
    )
}