package com.tangem.domain.addressbook.repository

import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/** Persistence port for the address book. The implementation is provided by the data layer. */
interface AddressBookRepository {

    fun getContacts(userWalletId: UserWalletId): Flow<List<Contact>>

    /** Contacts across several wallets, flattened. Each [Contact] keeps its own [Contact.walletId]. */
    fun getContacts(userWalletIds: Set<UserWalletId>): Flow<List<Contact>>

    suspend fun getContact(userWalletId: UserWalletId, name: String): Contact?

    /** Inserts or updates a [contact]. */
    suspend fun saveContact(contact: Contact)

    suspend fun deleteContact(id: ContactId)
}