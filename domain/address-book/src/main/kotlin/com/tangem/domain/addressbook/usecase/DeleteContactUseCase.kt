package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.repository.AddressBookRepository

class DeleteContactUseCase(
    private val repository: AddressBookRepository,
) {

    suspend operator fun invoke(id: ContactId) = repository.deleteContact(id)
}