package com.tangem.domain.addressbook.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.time.IsoTimestampProvider
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import java.util.UUID

/**
 * Creates a new [Contact] with client-generated UUID v4 ids. The name must be valid and unique

 * the current time. Every address entry is signed with [userWallet]'s key before the contact is
 * persisted, so only signed contacts are ever stored.
 */
class CreateContactUseCase(
    private val repository: AddressBookRepository,
    private val validateContactName: ValidateContactNameUseCase,
    private val signAddressEntries: SignAddressEntriesUseCase,
    private val timestampProvider: IsoTimestampProvider,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        name: String,
        iconColor: String,
        network: Network,
        addressEntries: List<AddressEntry>,
    ): Either<SaveContactError, Contact> = either {
        val userWalletId = userWallet.walletId
        val validName = validateContactName(userWalletId, name)
            .mapLeft(SaveContactError::Name)
            .bind()

        val now = timestampProvider.now()
        val contact = Contact(
            id = ContactId(UUID.randomUUID().toString()),
            walletId = userWalletId,
            name = validName,
            icon = "",
            iconColor = iconColor,
            createdAt = now,
            updatedAt = now,
            addressEntries = addressEntries,
        )
        val signed = signAddressEntries(userWallet, contact)
            .mapLeft(SaveContactError::Signing)
            .bind()
        repository.saveContact(signed)
        signed
    }
}