package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.repository.AddressBookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetContactByIdUseCase(
    private val repository: AddressBookRepository,
) {

    operator fun invoke(id: ContactId): Flow<Contact?> =
        repository.getAllContacts().map { contacts -> contacts.find { it.id == id } }
}