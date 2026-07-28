package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.lib.crypto.BlockchainUtils
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
        return source.map { contacts ->
            val filtered = if (normalizedQuery.isEmpty()) {
                contacts
            } else {
                contacts.filter { it.matches(normalizedQuery) }
            }
            filtered.sortedByDescending { it.createdAt }
        }
    }

    private fun Contact.matches(query: String): Boolean {
        val isNameContaining = name.value.contains(other = query, ignoreCase = true)
        val isAddressContaining = addresses.any { addressEntry ->
            val isCaseInsensitiveContractAddress = BlockchainUtils.isCaseInsensitiveContractAddress(
                networkId = addressEntry.networkId.value,
            )
            addressEntry.address.contains(other = query, ignoreCase = isCaseInsensitiveContractAddress)
        }
        val isNetworkContaining = addresses.any { addressEntry ->
            addressEntry.networkId.value.contains(other = query, ignoreCase = true)
        }
        return isNameContaining || isAddressContaining || isNetworkContaining
    }
}