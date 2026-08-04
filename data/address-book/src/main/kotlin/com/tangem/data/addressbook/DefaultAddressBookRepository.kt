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
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
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
import kotlinx.coroutines.flow.*
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

    private val logger = TangemLogger.withTag(LOG_TAG)

    override fun getContacts(userWalletId: UserWalletId): Flow<List<Contact>> {
        return getContactsForWallet(userWalletId)
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
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    private fun getContactsForWallet(userWalletId: UserWalletId): Flow<List<Contact>> {
        return blobStore.getBlob(userWalletId).map { blob ->
            val userWallet = blob?.let { findUserWallet(it.walletId) } ?: return@map emptyList()
            decryptContacts(blob, userWallet)
        }
    }

    override suspend fun getContactsSync(userWalletId: UserWalletId): List<Contact> {
        return withContext(dispatchers.io) {
            val blob = blobStore.getBlobSync(userWalletId) ?: return@withContext emptyList()
            val userWallet = findUserWallet(blob.walletId) ?: return@withContext emptyList()
            decryptContacts(blob, userWallet)
        }
    }

    override suspend fun getContact(userWalletId: UserWalletId, name: String): Contact? =
        withContext(dispatchers.default) {
            val blob = blobStore.getBlobSync(userWalletId) ?: return@withContext null
            val userWallet = findUserWallet(blob.walletId) ?: return@withContext null
            decryptContacts(blob, userWallet).find { it.name.value == name }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun isAddressBookCompatible(userWalletId: UserWalletId?): Flow<Boolean> {
        val source = if (userWalletId != null) {
            blobStore.getBlob(userWalletId).map { blob -> blob?.isVersionCompatible != false }
        } else {
            userWalletsListRepository.userWallets
                .filterNotNull()
                .flatMapLatest { wallets ->
                    val ids = wallets.mapTo(mutableSetOf()) { it.walletId }
                    // getBlobs returns only wallets that already have a stored book; wallets without one are
                    // absent here and count as compatible (a fresh book uses the supported version).
                    blobStore.getBlobs(ids).map { blobs -> blobs.all { it.isVersionCompatible } }
                }
        }
        return source.distinctUntilChanged().flowOn(dispatchers.default)
    }

    override suspend fun saveContact(contact: Contact): Either<AddressBookSyncError, Unit> =
        withContext(dispatchers.default) {
            writeMutex.withLock {
                val userWallet = findUserWallet(contact.walletId.stringValue)
                    ?: return@withLock AddressBookSyncError.Unknown.left()
                currentContacts(contact.walletId, userWallet).flatMap { current ->
                    val merged = current.filterNot { it.id == contact.id } + contact
                    persist(userWallet, AddressBook(contacts = merged))
                }
            }
        }

    override suspend fun deleteContact(id: ContactId): Either<AddressBookSyncError, Unit> =
        withContext(dispatchers.default) {
            writeMutex.withLock {
                userWalletsListRepository.userWalletsSync().forEach { userWallet ->
                    val blob = blobStore.getBlobSync(userWallet.walletId) ?: return@forEach
                    if (!blob.isVersionCompatible) return@forEach
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
        // Metadata only (wallet count) — persisted to the prod log so sync activity is traceable.
        logger.i("Syncing address books for ${wallets.size} wallet(s)")
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
                logger.i("Sync response: ${response.items.size} updated book(s) out of ${wallets.size} requested")
                response.items.forEach { item ->
                    val userWalletId = UserWalletId(stringValue = item.walletId)
                    // Metadata only (no keys/ciphertext/plaintext) — helps QA verify what the backend delivered
                    // (esp. for cross-platform books).
                    logger.i(
                        "Storing synced address book for wallet ${item.walletId}: " +
                            "updatedAt=${item.updatedAt}, nonceLen=${item.nonce.length}, " +
                            "ciphertextLen=${item.ciphertext.length}, authTagLen=${item.authTag.length}",
                    )
                    blobStore.storeBlob(item.toBlob())
                    eTagsStore.store(userWalletId, ETagsStore.Key.AddressBook, item.etag)
                }
                Unit.right()
            },
            onError = { error ->
                logger.e("Failed to sync address books: $error")
                error.toSyncError().left()
            },
        )
    }

    private fun decryptContacts(blob: AddressBookBlob, userWallet: UserWallet): List<Contact> {
        if (!blob.isVersionCompatible) {
            logger.e(
                "Skipping address book for wallet ${blob.walletId}: version ${blob.version} is newer than " +
                    "supported ${AddressBookBlob.CURRENT_VERSION}",
            )
            return emptyList()
        }
        return cipher.decrypt(blob, userWallet).fold(
            ifLeft = { error ->
                // The cipher already logged the low-level cause; this ties the failure to the read path so QA
                // can see that a stored/synced book (e.g. one created on iOS) could not be shown to the user.
                logger.e("Skipping address book for wallet ${blob.walletId}: decrypt failed with $error")
                emptyList()
            },
            ifRight = { it.contacts },
        )
    }

    /**
     * The contacts currently stored for [userWalletId], as the base a write is merged onto.
     *
     * A missing blob means no book exists yet → an empty base, so the first contact legitimately creates it.
     * But a blob that fails to decrypt must surface as [AddressBookSyncError.DecryptionFailed] rather than an empty
     * list: treating a broken book as empty would let a merged write overwrite the (non-empty) backend copy with a
     * book built from a single new contact, wiping every existing one.
     */
    private suspend fun currentContacts(
        userWalletId: UserWalletId,
        userWallet: UserWallet,
    ): Either<AddressBookSyncError, List<Contact>> {
        val blob = blobStore.getBlobSync(userWalletId) ?: return emptyList<Contact>().right()
        if (!blob.isVersionCompatible) {
            logger.e(
                "Refusing to overwrite address book for wallet $userWalletId: stored version ${blob.version} is " +
                    "newer than supported ${AddressBookBlob.CURRENT_VERSION} — a write would downgrade it",
            )
            return AddressBookSyncError.VersionMismatch.left()
        }
        return cipher.decrypt(blob, userWallet)
            .map { it.contacts }
            .mapLeft { error ->
                logger.e(
                    "Refusing to overwrite address book for wallet $userWalletId: it is stored but decrypt " +
                        "failed with $error",
                )
                AddressBookSyncError.DecryptionFailed
            }
    }

    /**
     * Encrypts [addressBook], pushes it to the backend, and persists it locally **only** on success.
     * On any failure (encryption, network, etag conflict, …) nothing is written locally.
     */
    private suspend fun persist(userWallet: UserWallet, addressBook: AddressBook): Either<AddressBookSyncError, Unit> {
        val updatedAt = DateTime.parse(timestampProvider.now())
        return cipher.encrypt(addressBook, userWallet, updatedAt)
            .mapLeft { error ->
                logger.e("Failed to encrypt address book for wallet ${userWallet.walletId}: $error")
                AddressBookSyncError.Unknown
            }
            .flatMap { blob -> pushBlob(userWallet.walletId, blob) }
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
                logger.e("Failed to push address book for wallet $userWalletId: $error")
                val syncError = error.toSyncError()
                // A 412 means the local etag is stale relative to the backend. Refresh the local blob + etag so the
                // next save attempt (user re-taps Save) starts from the current backend state. We do NOT re-push here
                // on purpose — the write stays single-shot; the conflict is still surfaced so the UI can prompt.
                if (syncError is AddressBookSyncError.Conflict) {
                    syncAddressBooks()
                }
                syncError.left()
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
        const val LOG_TAG = "AddressBook"
        const val MAX_SYNC_WALLETS = 20
    }
}