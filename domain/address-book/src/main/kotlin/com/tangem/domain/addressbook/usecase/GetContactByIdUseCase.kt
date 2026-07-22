package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.verification.ContactSignatureVerifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Emits the contact with the given [id], carrying only its verified addresses. Entries whose signatures
 * don't verify against the wallet are stripped, and a contact left with no verified addresses is treated
 * as absent (`null`) — the UI must never surface unverified addresses.
 */
class GetContactByIdUseCase(
    private val repository: AddressBookRepository,
    private val contactSignatureVerifier: ContactSignatureVerifier,
) {

    operator fun invoke(id: ContactId): Flow<Contact?> {
        return repository.getAllContacts().map { contacts ->
            val contact = contacts.find { it.id == id } ?: return@map null
            contactSignatureVerifier.verifyContacts(listOf(contact)).firstOrNull()
        }
    }
}