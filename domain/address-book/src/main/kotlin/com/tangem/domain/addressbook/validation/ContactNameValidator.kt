package com.tangem.domain.addressbook.validation

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.verification.ContactSignatureVerifier
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Validates a contact name: format rules via [ContactName] plus case-insensitive uniqueness within the
 * wallet.
 *
 * Uniqueness is enforced only against **verified** contacts (see [ContactSignatureVerifier.isNameVerified]):
 * a spoofed or tampered contact synced from another device must not be able to reserve a name. Reads the
 * local snapshot via [AddressBookRepository.getContactsSync] (validation runs on live keystrokes) and
 * filters to same-name contacts before verifying, so signature checks fire only on an actual collision.
 */
class ContactNameValidator(
    private val repository: AddressBookRepository,
    private val contactSignatureVerifier: ContactSignatureVerifier,
) {

    suspend fun validate(walletId: UserWalletId, name: String): Either<ContactNameValidationError, ContactName> =
        either {
            val validName = ContactName(name)
                .mapLeft(ContactNameValidationError::Format)
                .bind()

            val sameName = repository.getContactsSync(walletId)
                .filter { it.name.value.equals(validName.value, ignoreCase = true) }
            val isDuplicate = sameName.any { contactSignatureVerifier.isNameVerified(it) }
            ensure(!isDuplicate) { ContactNameValidationError.Duplicate }

            validName
        }
}