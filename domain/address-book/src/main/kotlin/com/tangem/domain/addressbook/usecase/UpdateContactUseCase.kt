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
import com.tangem.domain.models.wallet.UserWallet

class UpdateContactUseCase(
    private val repository: AddressBookRepository,
    private val signAddressEntries: SignAddressEntriesUseCase,
    private val timestampProvider: IsoTimestampProvider,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
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
        val signed = signAddressEntries(userWallet, updated)
            .mapLeft(SaveContactError::Signing)
            .bind()
        repository.saveContact(signed)
        signed
    }
}