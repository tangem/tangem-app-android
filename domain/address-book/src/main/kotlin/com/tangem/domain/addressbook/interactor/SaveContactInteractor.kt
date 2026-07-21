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
import com.tangem.domain.addressbook.usecase.buildAddressEntryPayload
import com.tangem.domain.addressbook.validation.ContactNameValidator
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SignHashesError
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.transaction.usecase.primarySecp256k1PublicKey
import com.tangem.utils.extensions.toHexString
import java.security.MessageDigest
import java.util.UUID

class SaveContactInteractor(
    private val repository: AddressBookRepository,
    private val validateContactName: ContactNameValidator,
    private val signUseCase: SignUseCase,
    private val timestampProvider: IsoTimestampProvider,
) {

    suspend fun createContact(
        userWallet: UserWallet,
        name: String,
        iconColor: String,
        addresses: List<AddressEntry>,
    ): Either<SaveContactError, Contact> = either {
        val userWalletId = userWallet.walletId
        val validName = validateContactName.validate(userWalletId, name)
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
            addresses = addresses,
        )
        val signed = signAddresses(userWallet, contact)
            .mapLeft(SaveContactError::Signing)
            .bind()
        repository.saveContact(signed)
            .mapLeft(SaveContactError::Backend)
            .bind()
        signed
    }

    /**
     * Moves an existing [contact] to [targetWallet]: creates a fresh contact there (new id, name validated for
     * uniqueness in the target wallet, addresses re-signed with the target wallet's key) and, once that succeeds,
     * deletes the original from its source wallet.
     *
     * Create-then-delete is deliberate: if the target write fails the original is untouched (no data loss); if only
     * the delete fails the contact ends up duplicated rather than lost.
     */
    suspend fun moveContact(
        targetWallet: UserWallet,
        contact: Contact,
        name: String,
        iconColor: String,
        addresses: List<AddressEntry>,
    ): Either<SaveContactError, Contact> = either {
        val created = createContact(
            userWallet = targetWallet,
            name = name,
            iconColor = iconColor,
            addresses = addresses,
        ).bind()
        repository.deleteContact(contact.id)
            .mapLeft(SaveContactError::Backend)
            .bind()
        created
    }

    suspend fun updateContact(
        userWallet: UserWallet,
        contact: Contact,
        name: String,
        iconColor: String,
        addresses: List<AddressEntry>,
    ): Either<SaveContactError, Contact> = either {
        val validName = ContactName(name)
            .mapLeft { SaveContactError.Name(ContactNameValidationError.Format(it)) }
            .bind()

        val updated = contact.copy(
            name = validName,
            iconColor = iconColor,
            addresses = addresses,
            updatedAt = timestampProvider.now(),
        )
        val signed = signAddresses(userWallet, updated)
            .mapLeft(SaveContactError::Signing)
            .bind()
        repository.saveContact(signed)
            .mapLeft(SaveContactError::Backend)
            .bind()
        signed
    }

    private suspend fun signAddresses(userWallet: UserWallet, contact: Contact): Either<SignHashesError, Contact> =
        either {
            val entries = contact.addresses
            if (entries.isEmpty()) return@either contact

            val publicKey = userWallet.primarySecp256k1PublicKey() ?: raise(SignHashesError.NoSigningKey)
            val hashes = entries.map { entry -> hashEntry(contact, entry) }
            val signatures = signUseCase(hashes = hashes, publicKey = publicKey, userWallet = userWallet).bind()

            val signedEntries = entries.mapIndexed { index, entry ->
                entry.copy(signature = signatures[index].toHexString())
            }
            contact.copy(addresses = signedEntries)
        }

    private fun hashEntry(contact: Contact, entry: AddressEntry): ByteArray {
        val payload = buildAddressEntryPayload(contact, entry)
        return MessageDigest.getInstance("SHA-256").digest(payload)
    }
}