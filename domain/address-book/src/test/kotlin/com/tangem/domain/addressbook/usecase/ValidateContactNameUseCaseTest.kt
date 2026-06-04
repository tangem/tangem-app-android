package com.tangem.domain.addressbook.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.error.ContactNameValidationError
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidateContactNameUseCaseTest {

    private val repository: AddressBookRepository = mockk(relaxUnitFun = true)
    private val useCase = ValidateContactNameUseCase(repository)

    private val walletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `format error is propagated`() = runTest {
        every { repository.getContacts(walletId) } returns flowOf(emptyList())

        val result = useCase(walletId, name = "")

        assertThat(result.leftOrNull())
            .isEqualTo(ContactNameValidationError.Format(ContactName.Error.Empty))
    }

    @Test
    fun `duplicate name in same wallet is rejected case-insensitively`() = runTest {
        every { repository.getContacts(walletId) } returns flowOf(listOf(contact(name = "Alice")))

        val result = useCase(walletId, name = "alice")

        assertThat(result.leftOrNull()).isEqualTo(ContactNameValidationError.Duplicate)
    }

    @Test
    fun `unique name is accepted`() = runTest {
        every { repository.getContacts(walletId) } returns flowOf(listOf(contact(name = "Alice")))

        val result = useCase(walletId, name = "Bob")

        assertThat(result.getOrNull()?.value).isEqualTo("Bob")
    }

    private fun contact(name: String): Contact = Contact(
        id = ContactId("id-$name"),
        walletId = walletId,
        name = requireNotNull(ContactName(name).getOrNull()),
        addressEntries = listOf(
            AddressEntry(
                id = AddressEntryId("addr-$name"),
                address = "0xabc",
                networkId = Network.RawID("ethereum"),
                memo = null,
                signature = "sig",
            ),
        ),
    )
}