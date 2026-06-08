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
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateContactUseCaseTest {

    private val repository: AddressBookRepository = mockk(relaxUnitFun = true)
    private val useCase = CreateContactUseCase(
        repository = repository,
        validateContactName = ValidateContactNameUseCase(repository),
    )

    private val walletId = UserWalletId("011")
    private val networkRawId = Network.RawID("ethereum")
    private val networkId = Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None)
    private val network: Network = mockk { every { id } returns networkId }

    private val addressEntries = listOf(
        AddressEntry(
            id = AddressEntryId("addr-1"),
            address = "0xabc",
            networkId = networkRawId,
            memo = "memo",
            signature = "sig",
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `create generates ids and persists the contact`() = runTest {
        every { repository.getContacts(walletId) } returns flowOf(emptyList())
        val saved = slot<Contact>()
        coEvery { repository.saveContact(capture(saved)) } returns Unit

        val result = useCase(
            userWalletId = walletId,
            name = "Alice",
            network = network,
            addressEntries = addressEntries,
        )

        val contact = result.getOrNull()
        assertThat(contact).isEqualTo(saved.captured)
        assertThat(contact!!.walletId).isEqualTo(walletId)
        assertThat(contact.name.value).isEqualTo("Alice")
        assertThat(contact.id.value).isNotEmpty()
        assertThat(contact.addressEntries).isEqualTo(addressEntries)
    }

    @Test
    fun `duplicate name fails without persisting`() = runTest {
        every { repository.getContacts(walletId) } returns flowOf(listOf(contact(name = "Alice")))

        val result = useCase(
            userWalletId = walletId,
            name = "alice",
            network = network,
            addressEntries = addressEntries,
        )

        assertThat(result.leftOrNull())
            .isEqualTo(SaveContactError.Name(ContactNameValidationError.Duplicate))
        coVerify(exactly = 0) { repository.saveContact(any()) }
    }

    @Test
    fun `invalid name fails without persisting`() = runTest {
        every { repository.getContacts(walletId) } returns flowOf(emptyList())

        val result = useCase(
            userWalletId = walletId,
            name = "",
            network = network,
            addressEntries = addressEntries,
        )

        assertThat(result.leftOrNull())
            .isEqualTo(SaveContactError.Name(ContactNameValidationError.Format(ContactName.Error.Empty)))
        coVerify(exactly = 0) { repository.saveContact(any()) }
    }

    private fun contact(name: String): Contact = Contact(
        id = ContactId("id-$name"),
        walletId = walletId,
        name = requireNotNull(ContactName(name).getOrNull()),
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