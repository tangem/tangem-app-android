package com.tangem.domain.addressbook.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.first

/**
 * Validates a contact name: format rules via [ContactName] plus case-insensitive uniqueness within
 * the wallet.
 */
class ValidateContactNameUseCase(
    private val repository: AddressBookRepository,
) {

    suspend operator fun invoke(
        walletId: UserWalletId,
        name: String,
    ): Either<ContactNameValidationError, ContactName> = either {
        val validName = ContactName(name)
            .mapLeft(ContactNameValidationError::Format)
            .bind()

        val contacts = repository.getContacts(walletId).first()
        val isDuplicate = contacts.any { contact ->
            contact.name.value.equals(validName.value, ignoreCase = true)
        }
        ensure(!isDuplicate) { ContactNameValidationError.Duplicate }

        validName
    }
}