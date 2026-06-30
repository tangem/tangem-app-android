package com.tangem.data.addressbook

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.addressbook.store.AddressBookBlobStore
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.datasource.api.addressbook.AddressBookApi
import com.tangem.datasource.api.addressbook.models.SyncAddressBooksRequest
import com.tangem.datasource.api.addressbook.models.SyncAddressBooksResponse
import com.tangem.datasource.api.addressbook.models.UpdateAddressBookRequest
import com.tangem.datasource.api.addressbook.models.UpdateAddressBookResponse
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.addressbook.crypto.AddressBookCipher
import com.tangem.domain.addressbook.error.AddressBookCryptoError
import com.tangem.domain.addressbook.error.AddressBookSyncError
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
import io.mockk.coVerifyOrder
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
    private val addressBookApi: AddressBookApi = mockk()
    private val eTagsStore: ETagsStore = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val timestampProvider: IsoTimestampProvider = mockk()

    private val userWallet: UserWallet = mockk {
        every { walletId } returns UserWalletId(WALLET_A)
    }

    private val repository = DefaultAddressBookRepository(
        blobStore = blobStore,
        cipher = cipher,
        addressBookApi = addressBookApi,
        eTagsStore = eTagsStore,
        userWalletsListRepository = userWalletsListRepository,
        timestampProvider = timestampProvider,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun setup() {
        clearMocks(blobStore, cipher, addressBookApi, eTagsStore, userWalletsListRepository, timestampProvider)
        every { timestampProvider.now() } returns TIMESTAMP
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWallet)
        coEvery { addressBookApi.syncAddressBooks(any()) } returns
            ApiResponse.Success(SyncAddressBooksResponse(items = emptyList()))
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
    fun `GIVEN blob WHEN getContacts THEN syncs before reading contacts`() = runTest {
        // Arrange
        val contact = createContact(id = "c1", name = "Alice")
        val blob = createBlob()
        every { blobStore.getBlob(UserWalletId(WALLET_A)) } returns flowOf(blob)
        every { cipher.decrypt(blob, userWallet) } returns AddressBook(UserWalletId(WALLET_A), listOf(contact)).right()

        // Act
        repository.getContacts(UserWalletId(WALLET_A)).first()

        // Assert
        coVerifyOrder {
            addressBookApi.syncAddressBooks(any())
            cipher.decrypt(blob, userWallet)
        }
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
    fun `GIVEN blob WHEN getAllContacts THEN syncs before reading contacts`() = runTest {
        // Arrange
        val contact = createContact(id = "c1", name = "Alice")
        val blob = createBlob()
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every { blobStore.getBlobs(setOf(UserWalletId(WALLET_A))) } returns flowOf(listOf(blob))
        every { cipher.decrypt(blob, userWallet) } returns AddressBook(UserWalletId(WALLET_A), listOf(contact)).right()

        // Act
        repository.getAllContacts().first()

        // Assert
        coVerifyOrder {
            addressBookApi.syncAddressBooks(any())
            cipher.decrypt(blob, userWallet)
        }
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
    fun `GIVEN backend accepts WHEN saveContact THEN pushes merged book and stores blob and etag`() = runTest {
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
        coEvery { addressBookApi.updateAddressBook(WALLET_A, any(), any()) } returns successPutResponse()

        // Act
        val result = repository.saveContact(added)

        // Assert
        assertThat(result).isEqualTo(Unit.right())
        assertThat(bookSlot.captured.contacts).containsExactly(existing, added)
        coVerify(exactly = 1) { blobStore.storeBlob(newBlob) }
        coVerify(exactly = 1) { eTagsStore.store(UserWalletId(WALLET_A), ETagsStore.Key.AddressBook, ETAG_NEW) }
    }

    @Test
    fun `GIVEN no stored etag WHEN saveContact THEN PUT is sent without If-Match`() = runTest {
        // Arrange
        coEvery { blobStore.getBlobSync(UserWalletId(WALLET_A)) } returns null
        val newBlob = createBlob()
        every { cipher.encrypt(any(), userWallet, any()) } returns newBlob.right()
        coEvery { blobStore.storeBlob(any()) } returns Unit
        coEvery { eTagsStore.getSyncOrNull(UserWalletId(WALLET_A), ETagsStore.Key.AddressBook) } returns null
        coEvery { addressBookApi.updateAddressBook(WALLET_A, null, any()) } returns successPutResponse()

        // Act
        val result = repository.saveContact(createContact(id = "c1", name = "Alice"))

        // Assert
        assertThat(result).isEqualTo(Unit.right())
        coVerify(exactly = 1) { addressBookApi.updateAddressBook(WALLET_A, null, any()) }
    }

    @Test
    fun `GIVEN stored etag WHEN saveContact THEN PUT carries it in If-Match`() = runTest {
        // Arrange
        coEvery { blobStore.getBlobSync(UserWalletId(WALLET_A)) } returns null
        every { cipher.encrypt(any(), userWallet, any()) } returns createBlob().right()
        coEvery { blobStore.storeBlob(any()) } returns Unit
        coEvery { eTagsStore.getSyncOrNull(UserWalletId(WALLET_A), ETagsStore.Key.AddressBook) } returns ETAG_OLD
        coEvery { addressBookApi.updateAddressBook(WALLET_A, ETAG_OLD, any()) } returns successPutResponse()

        // Act
        repository.saveContact(createContact(id = "c1", name = "Alice"))

        // Assert
        coVerify(exactly = 1) { addressBookApi.updateAddressBook(WALLET_A, ETAG_OLD, any()) }
    }

    @Test
    fun `GIVEN etag conflict WHEN saveContact THEN returns Conflict and does not store locally`() = runTest {
        // Arrange
        coEvery { blobStore.getBlobSync(UserWalletId(WALLET_A)) } returns null
        every { cipher.encrypt(any(), userWallet, any()) } returns createBlob().right()
        coEvery { addressBookApi.updateAddressBook(WALLET_A, any(), any()) } returns
            errorResponse(ApiResponseError.HttpException.Code.PRECONDITION_FAILED)

        // Act
        val result = repository.saveContact(createContact(id = "c1", name = "Alice"))

        // Assert
        assertThat(result).isEqualTo(AddressBookSyncError.Conflict.left())
        coVerify(exactly = 0) { blobStore.storeBlob(any()) }
        coVerify(exactly = 0) { eTagsStore.store(any(), any(), any()) }
    }

    @Test
    fun `GIVEN no network WHEN saveContact THEN returns Network and does not store locally`() = runTest {
        // Arrange
        coEvery { blobStore.getBlobSync(UserWalletId(WALLET_A)) } returns null
        every { cipher.encrypt(any(), userWallet, any()) } returns createBlob().right()
        coEvery { addressBookApi.updateAddressBook(WALLET_A, any(), any()) } returns networkErrorResponse()

        // Act
        val result = repository.saveContact(createContact(id = "c1", name = "Alice"))

        // Assert
        assertThat(result).isEqualTo(AddressBookSyncError.Network.left())
        coVerify(exactly = 0) { blobStore.storeBlob(any()) }
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
        coEvery { addressBookApi.updateAddressBook(WALLET_A, any(), any()) } returns successPutResponse()

        // Act
        repository.saveContact(updated)

        // Assert
        assertThat(bookSlot.captured.contacts).containsExactly(updated)
    }

    @Test
    fun `GIVEN contact in wallet WHEN deleteContact THEN pushes and re-stores book without it`() = runTest {
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
        coEvery { addressBookApi.updateAddressBook(WALLET_A, any(), any()) } returns successPutResponse()

        // Act
        val result = repository.deleteContact(ContactId("c2"))

        // Assert
        assertThat(result).isEqualTo(Unit.right())
        assertThat(bookSlot.captured.contacts).containsExactly(kept)
        coVerify(exactly = 1) { blobStore.storeBlob(newBlob) }
    }

    @Test
    fun `GIVEN backend returns changed item WHEN syncAddressBooks THEN stores blob and etag for it`() = runTest {
        // Arrange
        val walletB: UserWallet = mockk { every { walletId } returns UserWalletId(WALLET_B) }
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWallet, walletB)
        val requestSlot = slot<SyncAddressBooksRequest>()
        // Only wallet A changed; wallet B is omitted (matching etag) → must keep its local copy.
        coEvery { addressBookApi.syncAddressBooks(capture(requestSlot)) } returns
            ApiResponse.Success(SyncAddressBooksResponse(items = listOf(syncItem(WALLET_A))))
        val blobSlot = slot<AddressBookBlob>()
        coEvery { blobStore.storeBlob(capture(blobSlot)) } returns Unit

        // Act
        val result = repository.syncAddressBooks()

        // Assert
        assertThat(result).isEqualTo(Unit.right())
        assertThat(requestSlot.captured.wallets.map { it.walletId }).containsExactly(WALLET_A, WALLET_B)
        assertThat(blobSlot.captured.walletId).isEqualTo(WALLET_A)
        coVerify(exactly = 1) { blobStore.storeBlob(any()) }
        coVerify(exactly = 1) { eTagsStore.store(UserWalletId(WALLET_A), ETagsStore.Key.AddressBook, ETAG_NEW) }
        coVerify(exactly = 0) { blobStore.storeBlob(match { it.walletId == WALLET_B }) }
    }

    @Test
    fun `GIVEN unauthorized WHEN syncAddressBooks THEN returns Unauthorized and stores nothing`() = runTest {
        // Arrange
        coEvery { addressBookApi.syncAddressBooks(any()) } returns
            errorResponse(ApiResponseError.HttpException.Code.UNAUTHORIZED)

        // Act
        val result = repository.syncAddressBooks()

        // Assert
        assertThat(result).isEqualTo(AddressBookSyncError.Unauthorized.left())
        coVerify(exactly = 0) { blobStore.storeBlob(any()) }
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

    private fun successPutResponse(etag: String = ETAG_NEW): ApiResponse<UpdateAddressBookResponse> =
        ApiResponse.Success(
            data = UpdateAddressBookResponse(walletId = WALLET_A, etag = etag, updatedAt = TIMESTAMP),
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> errorResponse(code: ApiResponseError.HttpException.Code): ApiResponse<T> =
        ApiResponse.Error(
            cause = ApiResponseError.HttpException(code = code, message = null, errorBody = null),
        ) as ApiResponse<T>

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> networkErrorResponse(): ApiResponse<T> =
        ApiResponse.Error(cause = ApiResponseError.NetworkException()) as ApiResponse<T>

    private fun syncItem(walletId: String): SyncAddressBooksResponse.Item = SyncAddressBooksResponse.Item(
        walletId = walletId,
        etag = ETAG_NEW,
        version = AddressBookBlob.CURRENT_VERSION,
        updatedAt = TIMESTAMP,
        nonce = "00112233445566778899aabb",
        ciphertext = "deadbeef",
        authTag = "cafebabecafebabecafebabecafebabe",
    )

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
        const val WALLET_B = "0b0b0b"
        const val TIMESTAMP = "2026-05-22T09:00:00.000Z"
        const val ETAG_OLD = "etag-old"
        const val ETAG_NEW = "etag-new"
    }
}