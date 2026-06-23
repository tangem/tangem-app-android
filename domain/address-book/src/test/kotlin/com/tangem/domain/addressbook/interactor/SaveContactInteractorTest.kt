package com.tangem.domain.addressbook.interactor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.time.IsoTimestampProvider
import com.tangem.domain.addressbook.usecase.ValidateContactNameUseCase
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SignHashesError
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.utils.extensions.toHexString
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.security.MessageDigest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveContactInteractorTest {

    private val repository: AddressBookRepository = mockk(relaxUnitFun = true)
    private val signUseCase: SignUseCase = mockk()
    private val timestampProvider: IsoTimestampProvider = mockk {
        every { now() } returns NEW_TIMESTAMP
    }
    private val interactor = SaveContactInteractor(
        repository = repository,
        validateContactName = ValidateContactNameUseCase(repository),
        signUseCase = signUseCase,
        timestampProvider = timestampProvider,
    )

    // MockUserWalletFactory builds each wallet key with publicKey = curve.name bytes → secp256k1 key is "Secp256k1"
    private val userWallet: UserWallet = MockUserWalletFactory.create()
    private val secp256k1Key = "Secp256k1".toByteArray()
    private val networkRawId = Network.RawID("ethereum")

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, signUseCase, answers = false)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateContact {

        private val entries = listOf(entry(id = "addr-1", address = "0xabc", memo = "memo"))

        @Test
        fun `GIVEN unique name WHEN createContact THEN generates ids AND persists the signed contact`() = runTest {
            // Arrange
            stubNoExistingContacts()
            val signatures = listOf(byteArrayOf(0x01, 0xAB.toByte()))
            coEvery { signUseCase(hashes = any(), publicKey = any(), userWallet = eq(userWallet)) } returns
                signatures.right()
            val saved = slot<Contact>()
            coEvery { repository.saveContact(capture(saved)) } returns Unit

            // Act
            val result = interactor.createContact(userWallet, name = "Alice", iconColor = "TestColor", entries)

            // Assert
            val contact = result.getOrNull()
            assertThat(contact).isEqualTo(saved.captured)
            assertThat(contact!!.walletId).isEqualTo(userWallet.walletId)
            assertThat(contact.name.value).isEqualTo("Alice")
            assertThat(contact.id.value).isNotEmpty()
            assertThat(contact.createdAt).isEqualTo(NEW_TIMESTAMP)
            assertThat(contact.updatedAt).isEqualTo(NEW_TIMESTAMP)
            assertThat(contact.addressEntries.map { it.signature })
                .containsExactly(signatures[0].toHexString())
        }

        @Test
        fun `GIVEN entries WHEN createContact THEN signs each with the wallet key over the canonical payload`() =
            runTest {
                // Arrange
                stubNoExistingContacts()
                val twoEntries = listOf(
                    entry(id = "addr-1", address = "0xabc", memo = "memo"),
                    entry(id = "addr-2", address = "0xdef", memo = null),
                )
                val signatures = listOf(byteArrayOf(0x01, 0xAB.toByte()), byteArrayOf(0xCD.toByte()))
                val hashesSlot = slot<List<ByteArray>>()
                val publicKeySlot = slot<ByteArray>()
                coEvery {
                    signUseCase(hashes = capture(hashesSlot), publicKey = capture(publicKeySlot), userWallet = eq(userWallet))
                } returns signatures.right()
                val saved = slot<Contact>()
                coEvery { repository.saveContact(capture(saved)) } returns Unit

                // Act
                interactor.createContact(userWallet, name = "Alice", iconColor = "TestColor", twoEntries)

                // Assert
                assertThat(publicKeySlot.captured).isEqualTo(secp256k1Key)
                val persisted = saved.captured
                assertThat(hashesSlot.captured.map { it.toHexString() })
                    .containsExactly(
                        expectedHash(persisted, twoEntries[0]).toHexString(),
                        expectedHash(persisted, twoEntries[1]).toHexString(),
                    )
                    .inOrder()
                assertThat(persisted.addressEntries.map { it.signature })
                    .containsExactly(signatures[0].toHexString(), signatures[1].toHexString())
                    .inOrder()
            }

        @Test
        fun `GIVEN no entries WHEN createContact THEN persists without signing`() = runTest {
            // Arrange
            stubNoExistingContacts()
            val saved = slot<Contact>()
            coEvery { repository.saveContact(capture(saved)) } returns Unit

            // Act
            val result = interactor.createContact(userWallet, name = "Alice", iconColor = "TestColor", emptyList())

            // Assert
            assertThat(result.getOrNull()).isEqualTo(saved.captured)
            assertThat(saved.captured.addressEntries).isEmpty()
            coVerify(exactly = 0) { signUseCase(any<List<ByteArray>>(), any(), any()) }
        }

        @Test
        fun `GIVEN wallet without a secp256k1 key WHEN createContact THEN Signing NoSigningKey without persisting`() =
            runTest {
                // Arrange — a locked hot wallet exposes no key; validation must still pass first
                val lockedWallet = mockk<UserWallet.Hot> {
                    every { walletId } returns userWallet.walletId
                    every { wallets } returns null
                }
                stubNoExistingContacts()

                // Act
                val result = interactor.createContact(lockedWallet, name = "Alice", iconColor = "TestColor", entries)

                // Assert
                assertThat(result.leftOrNull())
                    .isEqualTo(SaveContactError.Signing(SignHashesError.NoSigningKey))
                coVerify(exactly = 0) { repository.saveContact(any()) }
            }

        @Test
        fun `GIVEN signUseCase fails WHEN createContact THEN propagates Signing error without persisting`() = runTest {
            // Arrange
            stubNoExistingContacts()
            coEvery { signUseCase(hashes = any(), publicKey = any(), userWallet = any()) } returns
                SignHashesError.SigningFailed(message = "canceled").left()

            // Act
            val result = interactor.createContact(userWallet, name = "Alice", iconColor = "TestColor", entries)

            // Assert
            assertThat(result.leftOrNull())
                .isEqualTo(SaveContactError.Signing(SignHashesError.SigningFailed(message = "canceled")))
            coVerify(exactly = 0) { repository.saveContact(any()) }
        }

        @Test
        fun `GIVEN duplicate name WHEN createContact THEN Name Duplicate without persisting`() = runTest {
            // Arrange
            every { repository.getContacts(userWallet.walletId) } returns flowOf(listOf(contact(name = "Alice")))

            // Act
            val result = interactor.createContact(userWallet, name = "alice", iconColor = "TestColor", entries)

            // Assert
            assertThat(result.leftOrNull())
                .isEqualTo(SaveContactError.Name(ContactNameValidationError.Duplicate))
            coVerify(exactly = 0) { repository.saveContact(any()) }
        }

        @Test
        fun `GIVEN blank name WHEN createContact THEN Name Format without persisting`() = runTest {
            // Arrange
            stubNoExistingContacts()

            // Act
            val result = interactor.createContact(userWallet, name = "", iconColor = "TestColor", entries)

            // Assert
            assertThat(result.leftOrNull())
                .isEqualTo(SaveContactError.Name(ContactNameValidationError.Format(ContactName.Error.Empty)))
            coVerify(exactly = 0) { repository.saveContact(any()) }
        }

        private fun stubNoExistingContacts() {
            every { repository.getContacts(userWallet.walletId) } returns flowOf(emptyList())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UpdateContact {

        private val updatedEntries = listOf(entry(id = "addr-new", address = "0xnew", memo = "memo"))

        @Test
        fun `GIVEN existing contact WHEN updateContact THEN preserves id AND restamps AND persists without uniqueness check`() =
            runTest {
                // Arrange
                val existing = contact(name = "Alice")
                val signatures = listOf(byteArrayOf(0x01, 0xAB.toByte()))
                coEvery { signUseCase(hashes = any(), publicKey = any(), userWallet = eq(userWallet)) } returns
                    signatures.right()
                val saved = slot<Contact>()
                coEvery { repository.saveContact(capture(saved)) } returns Unit

                // Act
                val result = interactor.updateContact(
                    userWallet = userWallet,
                    contact = existing,
                    name = "Bob",
                    iconColor = "TestColor",
                    addressEntries = updatedEntries,
                )

                // Assert
                val contact = result.getOrNull()
                assertThat(contact).isEqualTo(saved.captured)
                assertThat(contact!!.id).isEqualTo(existing.id)
                assertThat(contact.name.value).isEqualTo("Bob")
                assertThat(contact.createdAt).isEqualTo(ORIGINAL_TIMESTAMP)
                assertThat(contact.updatedAt).isEqualTo(NEW_TIMESTAMP)
                assertThat(contact.addressEntries.map { it.signature })
                    .containsExactly(signatures[0].toHexString())
                coVerify(exactly = 0) { repository.getContacts(any<UserWalletId>()) }
            }

        @Test
        fun `GIVEN signUseCase fails WHEN updateContact THEN propagates Signing error without persisting`() = runTest {
            // Arrange
            coEvery { signUseCase(hashes = any(), publicKey = any(), userWallet = any()) } returns
                SignHashesError.NoSigningKey.left()

            // Act
            val result = interactor.updateContact(
                userWallet = userWallet,
                contact = contact(name = "Alice"),
                name = "Bob",
                iconColor = "TestColor",
                addressEntries = updatedEntries,
            )

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(SaveContactError.Signing(SignHashesError.NoSigningKey))
            coVerify(exactly = 0) { repository.saveContact(any()) }
        }

        @Test
        fun `GIVEN blank name WHEN updateContact THEN Name Format without persisting`() = runTest {
            // Act
            val result = interactor.updateContact(
                userWallet = userWallet,
                contact = contact(name = "Alice"),
                name = "",
                iconColor = "TestColor",
                addressEntries = updatedEntries,
            )

            // Assert
            assertThat(result.leftOrNull())
                .isEqualTo(SaveContactError.Name(ContactNameValidationError.Format(ContactName.Error.Empty)))
            coVerify(exactly = 0) { repository.saveContact(any()) }
        }
    }

    private fun contact(name: String): Contact = Contact(
        id = ContactId("id-$name"),
        walletId = userWallet.walletId,
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = "",
        iconColor = "TestColor",
        createdAt = ORIGINAL_TIMESTAMP,
        updatedAt = ORIGINAL_TIMESTAMP,
        addressEntries = listOf(entry(id = "addr-$name", address = "0xabc", memo = null)),
    )

    private fun entry(id: String, address: String, memo: String?): AddressEntry = AddressEntry(
        id = AddressEntryId(id),
        address = address,
        networkId = networkRawId,
        memo = memo,
        signature = "sig",
        networkName = "Ethereum",
    )

    private fun expectedHash(contact: Contact, entry: AddressEntry): ByteArray {
        val payload = entry.address + entry.networkId.value + entry.memo.orEmpty() +
            contact.id.value + contact.name.value
        return MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
    }

    private companion object {
        const val NEW_TIMESTAMP = "2026-06-10T14:30:00.000Z"
        const val ORIGINAL_TIMESTAMP = "2026-01-01T00:00:00.000Z"
    }
}