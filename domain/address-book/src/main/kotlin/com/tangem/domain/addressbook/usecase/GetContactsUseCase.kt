package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

class GetContactsUseCase(
    private val repository: AddressBookRepository,
) {

    operator fun invoke(userWalletIds: Set<UserWalletId>): Flow<List<Contact>> = repository.getContacts(userWalletIds)
}