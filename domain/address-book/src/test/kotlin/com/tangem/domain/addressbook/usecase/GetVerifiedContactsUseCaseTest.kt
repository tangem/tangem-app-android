package com.tangem.domain.addressbook.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.model.*
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.VerifyMessagesError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVerifiedContactsUseCaseTest {

    private val getContacts: GetContactsUseCase = mockk()
    private val verifyAddressEntries: VerifyAddressEntriesUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val useCase = GetVerifiedContactsUseCase(
        getContacts = getContacts,
        verifyAddressEntries = verifyAddressEntries,
        userWalletsListRepository = userWalletsListRepository,
    )

    private val walletId = UserWalletId("011")
    private val userWallet: UserWallet = mockk { every { walletId } returns this@GetVerifiedContactsUseCaseTest.walletId }

    private val validEntry = entry(id = "valid", address = "0xvalid")
    private val invalidEntry = entry(id = "invalid", address = "0xinvalid")
    private val contact = contact(name = "Alice", entries = listOf(validEntry, invalidEntry))

    @BeforeEach
    fun resetMocks() {
        clearMocks(getContacts, verifyAddressEntries, userWalletsListRepository)
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWallet)
    }

    @Test
    fun `GIVEN mixed entries WHEN invoke THEN displays only valid AND keeps invalid for analytics`() = runTest {
        // Arrange
        every { getContacts(query = "", userWalletId = null) } returns flowOf(listOf(contact))
        every { verifyAddressEntries(userWallet, contact) } returns
            AddressEntriesVerification(valid = listOf(validEntry), invalid = listOf(invalidEntry)).right()

        // Act
        val result = useCase(query = "").first()

        // Assert
        assertThat(result).containsExactly(
            VerifiedContact(
                contact = contact.copy(addressEntries = listOf(validEntry)),
                invalidEntries = listOf(invalidEntry),
            ),
        )
    }

    @Test
    fun `GIVEN wallet cannot be resolved WHEN invoke THEN contact is dropped`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        every { getContacts(query = "", userWalletId = null) } returns flowOf(listOf(contact))

        // Act
        val result = useCase(query = "").first()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN verification fails WHEN invoke THEN contact is dropped`() = runTest {
        // Arrange
        every { getContacts(query = "", userWalletId = null) } returns flowOf(listOf(contact))
        every { verifyAddressEntries(userWallet, contact) } returns VerifyMessagesError.NoSigningKey.left()

        // Act
        val result = useCase(query = "").first()

        // Assert
        assertThat(result).isEmpty()
    }

    private fun entry(id: String, address: String): AddressEntry = AddressEntry(
        id = AddressEntryId(id),
        address = address,
        networkId = Network.RawID("ethereum"),
        memo = null,
        signature = "sig-$id",
        networkName = "Ethereum",
    )

    private fun contact(name: String, entries: List<AddressEntry>): Contact = Contact(
        id = ContactId("id-$name"),
        walletId = walletId,
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = "",
        iconColor = "KekColor",
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        addressEntries = entries,
    )
}