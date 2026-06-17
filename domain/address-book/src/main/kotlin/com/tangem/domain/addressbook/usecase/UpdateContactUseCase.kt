package com.tangem.domain.addressbook.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.time.IsoTimestampProvider

/**

 * format-checked — uniqueness is not re-validated on update. Address entries must be prepared and
 * validated before calling this use case. [Contact.updatedAt] is restamped with the current time.
 */
class UpdateContactUseCase(
    private val repository: AddressBookRepository,
    private val timestampProvider: IsoTimestampProvider,
) {

    suspend operator fun invoke(
        contact: Contact,
        name: String,
        addressEntries: List<AddressEntry>,
    ): Either<SaveContactError, Contact> = either {
        val validName = ContactName(name)
            .mapLeft { SaveContactError.Name(ContactNameValidationError.Format(it)) }
            .bind()

        val updated = contact.copy(
            name = validName,
            addressEntries = addressEntries,
            updatedAt = timestampProvider.now(),
        )
        repository.saveContact(updated)
        updated
    }
}