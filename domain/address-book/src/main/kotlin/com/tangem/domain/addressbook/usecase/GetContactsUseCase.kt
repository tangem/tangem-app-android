package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetContactsUseCase(
    private val repository: AddressBookRepository,
) {

    operator fun invoke(query: String, userWalletId: UserWalletId? = null): Flow<List<Contact>> {
        val source = if (userWalletId == null) {
            repository.getAllContacts()
        } else {
            repository.getContacts(userWalletId)
        }
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return source
        return source.map { contacts -> contacts.filter { it.matches(normalizedQuery) } }
    }

    private fun Contact.matches(query: String): Boolean {
        val isNameContaining = name.value.contains(other = query, ignoreCase = true)
        val isAddressContaining = addressEntries.any { addressEntry ->
            addressEntry.address.contains(other = query, ignoreCase = true)
        }
        return isNameContaining || isAddressContaining
    }
}