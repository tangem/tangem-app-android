package com.tangem.domain.addressbook.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
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
    private val useCase = GetContactByIdUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN matching id WHEN invoke THEN emits that contact`() = runTest {
        // Arrange
        val target = contact("id-2", "Bob")
        every { repository.getAllContacts() } returns flowOf(listOf(contact("id-1", "Alice"), target))

        // Act
        val result = useCase(ContactId("id-2")).first()

        // Assert
        assertThat(result).isEqualTo(target)
    }

    @Test
    fun `GIVEN no matching id WHEN invoke THEN emits null`() = runTest {
        // Arrange
        every { repository.getAllContacts() } returns flowOf(listOf(contact("id-1", "Alice")))

        // Act
        val result = useCase(ContactId("missing")).first()

        // Assert
        assertThat(result).isNull()
    }

    private fun contact(id: String, name: String): Contact = Contact(
        id = ContactId(id),
        walletId = UserWalletId("0001"),
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = "",
        iconColor = "Azure",
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        addressEntries = emptyList(),
    )
}