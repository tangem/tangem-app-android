package com.tangem.data.addressbook

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.addressbook.store.AddressBookBlobStore
import com.tangem.domain.addressbook.crypto.AddressBookCipher
import com.tangem.domain.addressbook.error.AddressBookCryptoError
import com.tangem.domain.addressbook.model.AddressBook
import com.tangem.domain.addressbook.model.AddressBookBlob
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.time.IsoTimestampProvider
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultAddressBookRepositoryTest {

    private val blobStore: AddressBookBlobStore = mockk()
    private val cipher: AddressBookCipher = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val timestampProvider: IsoTimestampProvider = mockk()

    private val userWallet: UserWallet = mockk {
        every { walletId } returns UserWalletId(WALLET_A)
    }

    private val repository = DefaultAddressBookRepository(
        blobStore = blobStore,
        cipher = cipher,
        userWalletsListRepository = userWalletsListRepository,
        timestampProvider = timestampProvider,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun setup() {
        clearMocks(blobStore, cipher, userWalletsListRepository, timestampProvider)
        every { timestampProvider.now() } returns TIMESTAMP
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWallet)
    }

    @Test
    fun `GIVEN decryptable blob WHEN getContacts THEN emits decrypted contacts`() = runTest {
        // Arrange
        val contact = createContact(id = "c1", name = "Alice")
        val blob = createBlob()
        every { blobStore.getBlob(UserWalletId(WALLET_A)) } returns flowOf(blob)
        every { cipher.decrypt(blob, userWallet) } returns AddressBook(UserWalletId(WALLET_A), listOf(contact)).right()

        // Act
        val result = repository.getContacts(UserWalletId(WALLET_A)).first()

        // Assert
        assertThat(result).containsExactly(contact)
    }

    @Test
    fun `GIVEN multiple wallets WHEN getAllContacts THEN emits contacts from all wallets`() = runTest {
        // Arrange
        val contact = createContact(id = "c1", name = "Alice")
        val blob = createBlob()
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every { blobStore.getBlobs(setOf(UserWalletId(WALLET_A))) } returns flowOf(listOf(blob))
        every { cipher.decrypt(blob, userWallet) } returns AddressBook(UserWalletId(WALLET_A), listOf(contact)).right()

        // Act
        val result = repository.getAllContacts().first()

        // Assert
        assertThat(result).containsExactly(contact)
    }

    @Test
    fun `GIVEN no blob WHEN getContacts THEN emits empty`() = runTest {
        // Arrange
        every { blobStore.getBlob(UserWalletId(WALLET_A)) } returns flowOf(null)

        // Act
        val result = repository.getContacts(UserWalletId(WALLET_A)).first()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN decryption fails WHEN getContacts THEN emits empty`() = runTest {
        // Arrange
        val blob = createBlob()
        every { blobStore.getBlob(UserWalletId(WALLET_A)) } returns flowOf(blob)
        every { cipher.decrypt(blob, userWallet) } returns AddressBookCryptoError.DecryptionFailed.left()

        // Act
        val result = repository.getContacts(UserWalletId(WALLET_A)).first()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN new contact WHEN saveContact THEN encrypts merged book and stores blob`() = runTest {
        // Arrange
        val existing = createContact(id = "c1", name = "Alice")
        val added = createContact(id = "c2", name = "Bob")
        val storedBlob = createBlob()
        coEvery { blobStore.getBlobSync(UserWalletId(WALLET_A)) } returns storedBlob
        every { cipher.decrypt(storedBlob, userWallet) } returns
            AddressBook(UserWalletId(WALLET_A), listOf(existing)).right()
        val bookSlot = slot<AddressBook>()
        val newBlob = createBlob()
        every { cipher.encrypt(capture(bookSlot), userWallet, any()) } returns newBlob.right()
        coEvery { blobStore.storeBlob(newBlob) } returns Unit

        // Act
        repository.saveContact(added)

        // Assert
        assertThat(bookSlot.captured.contacts).containsExactly(existing, added)
        coVerify(exactly = 1) { blobStore.storeBlob(newBlob) }
    }

    @Test
    fun `GIVEN existing contact id WHEN saveContact THEN replaces it`() = runTest {
        // Arrange
        val original = createContact(id = "c1", name = "Alice")
        val updated = createContact(id = "c1", name = "Alice Updated")
        val storedBlob = createBlob()
        coEvery { blobStore.getBlobSync(UserWalletId(WALLET_A)) } returns storedBlob
        every { cipher.decrypt(storedBlob, userWallet) } returns
            AddressBook(UserWalletId(WALLET_A), listOf(original)).right()
        val bookSlot = slot<AddressBook>()
        every { cipher.encrypt(capture(bookSlot), userWallet, any()) } returns createBlob().right()
        coEvery { blobStore.storeBlob(any()) } returns Unit

        // Act
        repository.saveContact(updated)

        // Assert
        assertThat(bookSlot.captured.contacts).containsExactly(updated)
    }

    @Test
    fun `GIVEN contact in wallet WHEN deleteContact THEN re-stores book without it`() = runTest {
        // Arrange
        val kept = createContact(id = "c1", name = "Alice")
        val removed = createContact(id = "c2", name = "Bob")
        val storedBlob = createBlob()
        coEvery { blobStore.getBlobSync(UserWalletId(WALLET_A)) } returns storedBlob
        every { cipher.decrypt(storedBlob, userWallet) } returns
            AddressBook(UserWalletId(WALLET_A), listOf(kept, removed)).right()
        val bookSlot = slot<AddressBook>()
        val newBlob = createBlob()
        every { cipher.encrypt(capture(bookSlot), userWallet, any()) } returns newBlob.right()
        coEvery { blobStore.storeBlob(newBlob) } returns Unit

        // Act
        repository.deleteContact(ContactId("c2"))

        // Assert
        assertThat(bookSlot.captured.contacts).containsExactly(kept)
        coVerify(exactly = 1) { blobStore.storeBlob(newBlob) }
    }

    @Test
    fun `GIVEN matching name WHEN getContact THEN returns it`() = runTest {
        // Arrange
        val alice = createContact(id = "c1", name = "Alice")
        val bob = createContact(id = "c2", name = "Bob")
        val blob = createBlob()
        coEvery { blobStore.getBlobSync(UserWalletId(WALLET_A)) } returns blob
        every { cipher.decrypt(blob, userWallet) } returns
            AddressBook(UserWalletId(WALLET_A), listOf(alice, bob)).right()

        // Act
        val result = repository.getContact(UserWalletId(WALLET_A), name = "Bob")

        // Assert
        assertThat(result).isEqualTo(bob)
    }

    private fun createContact(id: String, name: String, iconColor: String = "KekColor"): Contact = Contact(
        id = ContactId(id),
        walletId = UserWalletId(WALLET_A),
        name = ContactName(name).getOrNull()!!,
        icon = "",
        iconColor = iconColor,
        createdAt = TIMESTAMP,
        updatedAt = TIMESTAMP,
        addressEntries = emptyList(),
    )

    private fun createBlob(): AddressBookBlob = AddressBookBlob(
        walletId = WALLET_A,
        updatedAt = TIMESTAMP,
        nonce = "00112233445566778899aabb",
        ciphertext = "deadbeef",
        authTag = "cafebabecafebabecafebabecafebabe",
    )

    private companion object {
        const val WALLET_A = "0a0a0a"
        const val TIMESTAMP = "2026-05-22T09:00:00.000Z"
    }
}