package com.tangem.domain.addressbook.interactor

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.time.IsoTimestampProvider
import com.tangem.domain.addressbook.usecase.ValidateContactNameUseCase
import com.tangem.domain.addressbook.usecase.buildAddressEntryPayload
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SignHashesError
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.transaction.usecase.primarySecp256k1PublicKey
import com.tangem.utils.extensions.toHexString
import java.security.MessageDigest
import java.util.UUID

class SaveContactInteractor(
    private val repository: AddressBookRepository,
    private val validateContactName: ValidateContactNameUseCase,
    private val signUseCase: SignUseCase,
    private val timestampProvider: IsoTimestampProvider,
) {

    suspend fun createContact(
        userWallet: UserWallet,
        name: String,
        iconColor: String,
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

    suspend fun updateContact(
        userWallet: UserWallet,
        contact: Contact,
        name: String,
        iconColor: String,
        addressEntries: List<AddressEntry>,
    ): Either<SaveContactError, Contact> = either {
        val validName = ContactName(name)
            .mapLeft { SaveContactError.Name(ContactNameValidationError.Format(it)) }
            .bind()

        val updated = contact.copy(
            name = validName,
            iconColor = iconColor,
            addressEntries = addressEntries,
            updatedAt = timestampProvider.now(),
        )
        val signed = signAddressEntries(userWallet, updated)
            .mapLeft(SaveContactError::Signing)
            .bind()
        repository.saveContact(signed)
        signed
    }

    private suspend fun signAddressEntries(
        userWallet: UserWallet,
        contact: Contact,
    ): Either<SignHashesError, Contact> = either {
        val entries = contact.addressEntries
        if (entries.isEmpty()) return@either contact

        val publicKey = userWallet.primarySecp256k1PublicKey() ?: raise(SignHashesError.NoSigningKey)
        val hashes = entries.map { entry -> hashEntry(contact, entry) }
        val signatures = signUseCase(hashes = hashes, publicKey = publicKey, userWallet = userWallet).bind()

        val signedEntries = entries.mapIndexed { index, entry ->
            entry.copy(signature = signatures[index].toHexString())
        }
        contact.copy(addressEntries = signedEntries)
    }

    private fun hashEntry(contact: Contact, entry: AddressEntry): ByteArray {
        val payload = buildAddressEntryPayload(contact, entry)
        return MessageDigest.getInstance("SHA-256").digest(payload)
    }
}