package com.tangem.domain.addressbook.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import java.util.UUID

/**
 * Creates a new [Contact] with client-generated UUID v4 ids. The name must be valid and unique
 * (case-insensitive) within the wallet.
 */
class CreateContactUseCase(
    private val repository: AddressBookRepository,
    private val validateContactName: ValidateContactNameUseCase,
) {

    @Suppress("LongParameterList")
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        name: String,
        network: Network,
        addressEntries: List<AddressEntry>,
    ): Either<SaveContactError, Contact> = either {
        val validName = validateContactName(userWalletId, name)
            .mapLeft(SaveContactError::Name)
            .bind()

        val contact = Contact(
            id = ContactId(UUID.randomUUID().toString()),
            walletId = userWalletId,
            name = validName,
            addressEntries = addressEntries,
        )
        repository.saveContact(contact)
        contact
    }
}