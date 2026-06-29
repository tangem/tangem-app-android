package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.repository.AddressBookRepository

class SyncAddressBooksUseCase(
    private val repository: AddressBookRepository,
) {

    suspend operator fun invoke() = repository.syncAddressBooks()
}