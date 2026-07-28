package com.tangem.domain.addressbook.verification

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.VerifyMessagesError
import com.tangem.domain.transaction.usecase.VerifySecp256k1MessagesUseCase
import com.tangem.utils.extensions.toHexString
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactSignatureVerifierTest {

    private val verifyMessages: VerifySecp256k1MessagesUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val verifier = ContactSignatureVerifier(
        verifyMessages = verifyMessages,
        userWalletsListRepository = userWalletsListRepository,
    )

    private val walletId = UserWalletId("011")
    private val userWallet: UserWallet = mockk { every { walletId } returns this@ContactSignatureVerifierTest.walletId }

    @BeforeEach
    fun resetMocks() {
        clearMocks(verifyMessages, userWalletsListRepository)
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWallet)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class VerifyContacts {

        @Test
        fun `GIVEN mixed entries WHEN verifyContacts THEN keeps only the valid ones`() =
            runTest {
                // Arrange
                val valid = entry(id = "valid", address = "0xvalid", memo = null, signature = "AABB")
                val invalid = entry(id = "invalid", address = "0xinvalid", memo = null, signature = "CCDD")
                val contact = contact(valid, invalid)
                every { verifyMessages(any(), any(), any()) } returns listOf(true, false).right()

                // Act
                val result = verifier.verifyContacts(listOf(contact))

                // Assert
                assertThat(result).containsExactly(contact.copy(addresses = listOf(valid)))
            }

        @Test
        fun `GIVEN contact with entries WHEN verifyContacts THEN verifies each entry payload and its signature`() =
            runTest {
                // Arrange
                val contact = contact(
                    entry(id = "addr-1", address = "0xabc", memo = "memo", signature = "AABB"),
                    entry(id = "addr-2", address = "0xdef", memo = null, signature = "CCDD"),
                )
                val messagesSlot = slot<List<ByteArray>>()
                val signaturesSlot = slot<List<ByteArray>>()
                every {
                    verifyMessages(eq(userWallet), capture(messagesSlot), capture(signaturesSlot))
                } returns listOf(true, true).right()

                // Act
                verifier.verifyContacts(listOf(contact))

                // Assert
                assertThat(messagesSlot.captured.map { String(it) })
                    .containsExactly(
                        expectedPayload(contact, contact.addresses[0]),
                        expectedPayload(contact, contact.addresses[1]),
                    )
                    .inOrder()
                assertThat(signaturesSlot.captured.map { it.toHexString() }).containsExactly("AABB", "CCDD").inOrder()
            }

        @Test
        fun `GIVEN some entries fail verification WHEN verifyContacts THEN keeps valid ones preserving order`() =
            runTest {
                // Arrange
                val valid1 = entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB")
                val invalid = entry(id = "addr-2", address = "0xdef", memo = null, signature = "CCDD")
                val valid2 = entry(id = "addr-3", address = "0xghi", memo = null, signature = "EEFF")
                val contact = contact(valid1, invalid, valid2)
                every { verifyMessages(any(), any(), any()) } returns listOf(true, false, true).right()

                // Act
                val result = verifier.verifyContacts(listOf(contact)).single()

                // Assert
                assertThat(result.addresses).containsExactly(valid1, valid2).inOrder()
            }

        @Test
        fun `GIVEN malformed signature WHEN verifyContacts THEN that entry is invalid and excluded from verification`() =
            runTest {
                // Arrange
                val malformed = entry(id = "addr-1", address = "0xabc", memo = null, signature = "not-hex")
                val signed = entry(id = "addr-2", address = "0xdef", memo = null, signature = "AABB")
                val contact = contact(malformed, signed)
                val signaturesSlot = slot<List<ByteArray>>()
                every {
                    verifyMessages(eq(userWallet), any(), capture(signaturesSlot))
                } returns listOf(true).right()

                // Act
                val result = verifier.verifyContacts(listOf(contact)).single()

                // Assert
                assertThat(signaturesSlot.captured.map { it.toHexString() }).containsExactly("AABB")
                assertThat(result.addresses).containsExactly(signed)
            }

        @Test
        fun `GIVEN contact with no entries WHEN verifyContacts THEN contact is dropped without verifying`() = runTest {
            // Arrange
            val contact = contact()

            // Act
            val result = verifier.verifyContacts(listOf(contact))

            // Assert
            assertThat(result).isEmpty()
            verify(exactly = 0) { verifyMessages(any(), any(), any()) }
        }

        @Test
        fun `GIVEN all entries invalid WHEN verifyContacts THEN contact is dropped`() = runTest {
            // Arrange
            val contact = contact(
                entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB"),
                entry(id = "addr-2", address = "0xdef", memo = null, signature = "CCDD"),
            )
            every { verifyMessages(any(), any(), any()) } returns listOf(false, false).right()

            // Act
            val result = verifier.verifyContacts(listOf(contact))

            // Assert
            assertThat(result).isEmpty()
        }

        @Test
        fun `GIVEN entry with malformed signature only WHEN verifyContacts THEN contact is dropped`() = runTest {
            // Arrange
            val malformed = entry(id = "addr-1", address = "0xabc", memo = null, signature = "not-hex")
            val contact = contact(malformed)
            every { verifyMessages(any(), any(), any()) } returns emptyList<Boolean>().right()

            // Act
            val result = verifier.verifyContacts(listOf(contact))

            // Assert
            assertThat(result).isEmpty()
        }

        @Test
        fun `GIVEN one contact fully invalid AND another valid WHEN verifyContacts THEN only the valid one is kept`() =
            runTest {
                // Arrange
                val invalidEntry = entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB")
                val validEntry = entry(id = "addr-2", address = "0xdef", memo = null, signature = "CCDD")
                val droppedContact = contact(invalidEntry).copy(id = ContactId("contact-dropped"))
                val keptContact = contact(validEntry).copy(id = ContactId("contact-kept"))
                every { verifyMessages(any(), any(), any()) } returnsMany listOf(
                    listOf(false).right(),
                    listOf(true).right(),
                )

                // Act
                val result = verifier.verifyContacts(listOf(droppedContact, keptContact))

                // Assert
                assertThat(result).containsExactly(keptContact.copy(addresses = listOf(validEntry)))
            }

        @Test
        fun `GIVEN wallet cannot be resolved WHEN verifyContacts THEN contact is dropped`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val contact = contact(entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB"))

            // Act
            val result = verifier.verifyContacts(listOf(contact))

            // Assert
            assertThat(result).isEmpty()
        }

        @Test
        fun `GIVEN verification fails WHEN verifyContacts THEN contact is dropped`() = runTest {
            // Arrange
            val contact = contact(entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB"))
            every { verifyMessages(any(), any(), any()) } returns VerifyMessagesError.NoSigningKey.left()

            // Act
            val result = verifier.verifyContacts(listOf(contact))

            // Assert
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsNameVerified {

        @Test
        fun `GIVEN at least one valid entry WHEN isNameVerified THEN true`() = runTest {
            // Arrange
            val contact = contact(
                entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB"),
                entry(id = "addr-2", address = "0xdef", memo = null, signature = "CCDD"),
            )
            every { verifyMessages(any(), any(), any()) } returns listOf(false, true).right()

            // Act & Assert
            assertThat(verifier.isNameVerified(contact)).isTrue()
        }

        @Test
        fun `GIVEN all entries invalid WHEN isNameVerified THEN false`() = runTest {
            // Arrange
            val contact = contact(entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB"))
            every { verifyMessages(any(), any(), any()) } returns listOf(false).right()

            // Act & Assert
            assertThat(verifier.isNameVerified(contact)).isFalse()
        }

        @Test
        fun `GIVEN contact with no entries WHEN isNameVerified THEN false`() = runTest {
            // Arrange
            val contact = contact()

            // Act & Assert
            assertThat(verifier.isNameVerified(contact)).isFalse()
            verify(exactly = 0) { verifyMessages(any(), any(), any()) }
        }

        @Test
        fun `GIVEN wallet cannot be resolved WHEN isNameVerified THEN false`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val contact = contact(entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB"))

            // Act & Assert
            assertThat(verifier.isNameVerified(contact)).isFalse()
        }

        @Test
        fun `GIVEN verification fails WHEN isNameVerified THEN false`() = runTest {
            // Arrange
            val contact = contact(entry(id = "addr-1", address = "0xabc", memo = null, signature = "AABB"))
            every { verifyMessages(any(), any(), any()) } returns VerifyMessagesError.NoSigningKey.left()

            // Act & Assert
            assertThat(verifier.isNameVerified(contact)).isFalse()
        }
    }

    private fun contact(vararg entries: AddressEntry): Contact = Contact(
        id = ContactId("contact-1"),
        walletId = walletId,
        name = requireNotNull(ContactName("Alice").getOrNull()),
        icon = "",
        iconColor = "KekColor",
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        addresses = entries.toList(),
    )

    private fun entry(id: String, address: String, memo: String?, signature: String): AddressEntry = AddressEntry(
        id = AddressEntryId(id),
        address = address,
        networkId = Network.RawID("ethereum"),
        memo = memo,
        signature = signature,
    )

    private fun expectedPayload(contact: Contact, entry: AddressEntry): String =
        entry.address + entry.networkId.value + entry.memo.orEmpty() + contact.id.value + contact.name.value
}