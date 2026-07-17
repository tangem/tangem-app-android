package com.tangem.domain.addressbook.repository

import arrow.core.Either
import com.tangem.domain.addressbook.error.AddressBookSyncError
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/** Persistence port for the address book. The implementation is provided by the data layer. */
interface AddressBookRepository {

    /** Contacts for a single wallet. Each [Contact] keeps its own [Contact.walletId]. */
    fun getContacts(userWalletId: UserWalletId): Flow<List<Contact>>

    /** Contacts across all wallets (flattened). Each [Contact] keeps its own [Contact.walletId]. */
    fun getAllContacts(): Flow<List<Contact>>

    suspend fun getContactsSync(userWalletId: UserWalletId): List<Contact>

    suspend fun getContact(userWalletId: UserWalletId, name: String): Contact?

    /**
     * Whether the stored address book(s) can be used by this build — i.e. their contract version is not newer
     * than the one this app supports (see [com.tangem.domain.addressbook.model.AddressBookBlob.isVersionCompatible]).
     * @param userWalletId a specific wallet, or `null` to check every wallet — `null` is compatible only when
     * **all** currently stored books are compatible.
     */
    fun isAddressBookCompatible(userWalletId: UserWalletId? = null): Flow<Boolean>

    suspend fun saveContact(contact: Contact): Either<AddressBookSyncError, Unit>

    suspend fun deleteContact(id: ContactId): Either<AddressBookSyncError, Unit>

    suspend fun syncAddressBooks(): Either<AddressBookSyncError, Unit>
}