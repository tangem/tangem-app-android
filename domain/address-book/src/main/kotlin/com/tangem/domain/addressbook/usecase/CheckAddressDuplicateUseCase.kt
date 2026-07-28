package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.verification.ContactSignatureVerifier
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Enforces the `network + address` uniqueness rule within a wallet's address book: checks whether the
 * ([networkId], [address]) pair is already saved and, if so, returns the name of the contact that holds it
 * so the UI can tell the user under which name it is stored. Returns `null` when the pair is free.
 *
 * The same address in a different network is allowed. [excludeContactId] lets an in-place edit skip the
 * contact currently being edited. Address comparison is exact (matching the in-editor dedup in
 * `AddValidatedAddressTransformer`), so case-sensitive chains are not falsely flagged.
 *
 * Only verified addresses count as duplicates: contacts are run through [ContactSignatureVerifier] first, so
 * an unverified/invalid entry never blocks the pair and the user is free to overwrite it.
 */
class CheckAddressDuplicateUseCase(
    private val repository: AddressBookRepository,
    private val contactSignatureVerifier: ContactSignatureVerifier,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        networkId: String,
        address: String,
        excludeContactId: ContactId? = null,
    ): String? {
        val contacts = repository.getContactsSync(userWalletId)
        return contactSignatureVerifier.verifyContacts(contacts)
            .firstOrNull { contact ->
                contact.id != excludeContactId && contact.addresses.any { entry ->
                    entry.networkId.value == networkId && entry.address == address
                }
            }
            ?.name?.value
    }
}