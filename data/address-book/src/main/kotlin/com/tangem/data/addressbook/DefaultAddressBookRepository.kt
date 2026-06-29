package com.tangem.data.addressbook

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.tangem.data.addressbook.store.AddressBookBlobStore
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.datasource.api.addressbook.AddressBookApi
import com.tangem.datasource.api.addressbook.models.SyncAddressBooksRequest
import com.tangem.datasource.api.addressbook.models.SyncAddressBooksResponse
import com.tangem.datasource.api.addressbook.models.UpdateAddressBookRequest
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.domain.addressbook.crypto.AddressBookCipher
import com.tangem.domain.addressbook.error.AddressBookSyncError
import com.tangem.domain.addressbook.model.AddressBook
import com.tangem.domain.addressbook.model.AddressBookBlob
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.time.IsoTimestampProvider
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.joda.time.DateTime

@Suppress("LongParameterList")
internal class DefaultAddressBookRepository(
    private val blobStore: AddressBookBlobStore,
    private val cipher: AddressBookCipher,
    private val addressBookApi: AddressBookApi,
    private val eTagsStore: ETagsStore,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val timestampProvider: IsoTimestampProvider,
    private val dispatchers: CoroutineDispatcherProvider,
) : AddressBookRepository {

    private val writeMutex = Mutex()

    override fun getContacts(userWalletId: UserWalletId): Flow<List<Contact>> {
        return getContactsForWallet(userWalletId)
            .onStart { syncAddressBooks() }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllContacts(): Flow<List<Contact>> {
        return userWalletsListRepository.userWallets
            .filterNotNull()
            .flatMapLatest { wallets ->
                val walletsById = wallets.associateBy { it.walletId.stringValue }
                val ids = wallets.mapTo(mutableSetOf()) { it.walletId }
                blobStore.getBlobs(ids).map { blobs ->
                    blobs.flatMap { blob ->
                        walletsById[blob.walletId]?.let { userWallet ->
                            decryptContacts(blob, userWallet)
                        }.orEmpty()
                    }
                }
            }
            .onStart { syncAddressBooks() }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    private fun getContactsForWallet(userWalletId: UserWalletId): Flow<List<Contact>> {
        return blobStore.getBlob(userWalletId).map { blob ->
            val userWallet = blob?.let { findUserWallet(it.walletId) } ?: return@map emptyList()
            decryptContacts(blob, userWallet)
        }
    }

    override suspend fun getContact(userWalletId: UserWalletId, name: String): Contact? =
        withContext(dispatchers.default) {
            val blob = blobStore.getBlobSync(userWalletId) ?: return@withContext null
            val userWallet = findUserWallet(blob.walletId) ?: return@withContext null
            decryptContacts(blob, userWallet).find { it.name.value == name }
        }

    override suspend fun saveContact(contact: Contact): Either<AddressBookSyncError, Unit> =
        withContext(dispatchers.default) {
            writeMutex.withLock {
                val userWallet = findUserWallet(contact.walletId.stringValue)
                    ?: return@withLock AddressBookSyncError.Unknown.left()
                val current = currentContacts(contact.walletId, userWallet)
                val merged = current.filterNot { it.id == contact.id } + contact
                persist(userWallet, AddressBook(walletId = contact.walletId, contacts = merged))
            }
        }

    override suspend fun deleteContact(id: ContactId): Either<AddressBookSyncError, Unit> =
        withContext(dispatchers.default) {
            writeMutex.withLock {
                userWalletsListRepository.userWalletsSync().forEach { userWallet ->
                    val blob = blobStore.getBlobSync(userWallet.walletId) ?: return@forEach
                    val addressBook = cipher.decrypt(blob, userWallet).getOrNull() ?: return@forEach
                    if (addressBook.contacts.none { it.id == id }) return@forEach

                    val remaining = addressBook.contacts.filterNot { it.id == id }
                    return@withLock persist(userWallet, addressBook.copy(contacts = remaining))
                }
                // No wallet held the contact — nothing to push, treat as success.
                Unit.right()
            }
        }

    override suspend fun syncAddressBooks(): Either<AddressBookSyncError, Unit> = withContext(dispatchers.default) {
        val wallets = userWalletsListRepository.userWalletsSync()
        // The backend rejects more than MAX_SYNC_WALLETS per request, so sync in chunks and stop on the
        // first failed chunk.
        wallets.chunked(MAX_SYNC_WALLETS)
            .fold(initial = Unit.right() as Either<AddressBookSyncError, Unit>) { acc, chunk ->
                acc.flatMap { syncWalletsChunk(chunk) }
            }
    }

    private suspend fun syncWalletsChunk(wallets: List<UserWallet>): Either<AddressBookSyncError, Unit> {
        val request = SyncAddressBooksRequest(
            wallets = wallets.map { wallet ->
                SyncAddressBooksRequest.Wallet(
                    walletId = wallet.walletId.stringValue,
                    etag = eTagsStore.getSyncOrNull(wallet.walletId, ETagsStore.Key.AddressBook),
                )
            },
        )
        return safeApiCall(
            call = {
                val response = withContext(dispatchers.io) { addressBookApi.syncAddressBooks(request).bind() }
                // Only wallets whose etag changed are returned; the rest keep their local copy.
                response.items.forEach { item ->
                    val userWalletId = UserWalletId(stringValue = item.walletId)
                    blobStore.storeBlob(item.toBlob())
                    eTagsStore.store(userWalletId, ETagsStore.Key.AddressBook, item.etag)
                }
                Unit.right()
            },
            onError = { error ->
                TangemLogger.e(messageString = "Failed to sync address books: $error")
                error.toSyncError().left()
            },
        )
    }

    private fun decryptContacts(blob: AddressBookBlob, userWallet: UserWallet): List<Contact> {
        return cipher.decrypt(blob, userWallet).getOrNull()?.contacts.orEmpty()
    }

    private suspend fun currentContacts(userWalletId: UserWalletId, userWallet: UserWallet): List<Contact> {
        val blob = blobStore.getBlobSync(userWalletId) ?: return emptyList()
        return decryptContacts(blob, userWallet)
    }

    /**
     * Encrypts [addressBook], pushes it to the backend, and persists it locally **only** on success.
     * On any failure (encryption, network, etag conflict, …) nothing is written locally.
     */
    private suspend fun persist(userWallet: UserWallet, addressBook: AddressBook): Either<AddressBookSyncError, Unit> {
        val updatedAt = DateTime.parse(timestampProvider.now())
        return cipher.encrypt(addressBook, userWallet, updatedAt)
            .mapLeft { error ->
                TangemLogger.e(
                    messageString = "Failed to encrypt address book for wallet ${userWallet.walletId}: $error",
                )
                AddressBookSyncError.Unknown
            }
            .flatMap { blob -> pushBlob(addressBook.walletId, blob) }
    }

    private suspend fun pushBlob(
        userWalletId: UserWalletId,
        blob: AddressBookBlob,
    ): Either<AddressBookSyncError, Unit> {
        // Absent etag means the book has not been created on the backend yet → omit If-Match to create it.
        val eTag = eTagsStore.getSyncOrNull(userWalletId, ETagsStore.Key.AddressBook)
        return safeApiCall(
            call = {
                val response = withContext(dispatchers.io) {
                    addressBookApi.updateAddressBook(
                        walletId = blob.walletId,
                        eTag = eTag,
                        body = UpdateAddressBookRequest(
                            version = blob.version,
                            nonce = blob.nonce,
                            ciphertext = blob.ciphertext,
                            authTag = blob.authTag,
                        ),
                    ).bind()
                }
                blobStore.storeBlob(blob)
                eTagsStore.store(userWalletId, ETagsStore.Key.AddressBook, response.etag)
                Unit.right()
            },
            onError = { error ->
                TangemLogger.e(messageString = "Failed to push address book for wallet $userWalletId: $error")
                error.toSyncError().left()
            },
        )
    }

    private fun SyncAddressBooksResponse.Item.toBlob(): AddressBookBlob = AddressBookBlob(
        version = version,
        walletId = walletId,
        updatedAt = updatedAt,
        nonce = nonce,
        ciphertext = ciphertext,
        authTag = authTag,
    )

    private fun ApiResponseError.toSyncError(): AddressBookSyncError = when (this) {
        is ApiResponseError.HttpException -> when (code) {
            Code.PRECONDITION_FAILED -> AddressBookSyncError.Conflict
            Code.NOT_FOUND -> AddressBookSyncError.NotFound
            Code.UNAUTHORIZED -> AddressBookSyncError.Unauthorized
            Code.BAD_REQUEST -> AddressBookSyncError.BadRequest
            else -> AddressBookSyncError.Unknown
        }
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> AddressBookSyncError.Network
        is ApiResponseError.UnknownException -> AddressBookSyncError.Unknown
    }

    private suspend fun findUserWallet(walletId: String): UserWallet? =
        userWalletsListRepository.userWalletsSync().find { it.walletId.stringValue == walletId }

    private companion object {
        const val MAX_SYNC_WALLETS = 20
    }
}