package com.tangem.data.addressbook

import com.tangem.data.addressbook.store.AddressBookBlobStore
import com.tangem.domain.addressbook.crypto.AddressBookCipher
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.joda.time.DateTime

internal class DefaultAddressBookRepository(
    private val blobStore: AddressBookBlobStore,
    private val cipher: AddressBookCipher,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val timestampProvider: IsoTimestampProvider,
    private val dispatchers: CoroutineDispatcherProvider,
) : AddressBookRepository {

    private val writeMutex = Mutex()

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

    override suspend fun getContact(userWalletId: UserWalletId, name: String): Contact? =
        withContext(dispatchers.default) {
            val blob = blobStore.getBlobSync(userWalletId) ?: return@withContext null
            val userWallet = findUserWallet(blob.walletId) ?: return@withContext null
            decryptContacts(blob, userWallet).find { it.name.value == name }
        }

    override suspend fun saveContact(contact: Contact) = withContext(dispatchers.default) {
        writeMutex.withLock {
            val userWallet = findUserWallet(contact.walletId.stringValue) ?: return@withLock
            val current = currentContacts(contact.walletId, userWallet)
            val merged = current.filterNot { it.id == contact.id } + contact
            persist(userWallet, AddressBook(walletId = contact.walletId, contacts = merged))
        }
    }

    override suspend fun deleteContact(id: ContactId) = withContext(dispatchers.default) {
        writeMutex.withLock {
            userWalletsListRepository.userWalletsSync().forEach { userWallet ->
                val blob = blobStore.getBlobSync(userWallet.walletId) ?: return@forEach
                val addressBook = cipher.decrypt(blob, userWallet).getOrNull() ?: return@forEach
                if (addressBook.contacts.none { it.id == id }) return@forEach

                val remaining = addressBook.contacts.filterNot { it.id == id }
                persist(userWallet, addressBook.copy(contacts = remaining))
                return@withLock
            }
        }
    }

    private fun decryptContacts(blob: AddressBookBlob, userWallet: UserWallet): List<Contact> {
        return cipher.decrypt(blob, userWallet).getOrNull()?.contacts.orEmpty()
    }

    private suspend fun currentContacts(userWalletId: UserWalletId, userWallet: UserWallet): List<Contact> {
        val blob = blobStore.getBlobSync(userWalletId) ?: return emptyList()
        return decryptContacts(blob, userWallet)
    }

    private suspend fun persist(userWallet: UserWallet, addressBook: AddressBook) {
        val updatedAt = DateTime.parse(timestampProvider.now())
        cipher.encrypt(addressBook, userWallet, updatedAt)
            .onRight { blobStore.storeBlob(it) }
    }

    private suspend fun findUserWallet(walletId: String): UserWallet? =
        userWalletsListRepository.userWalletsSync().find { it.walletId.stringValue == walletId }
}