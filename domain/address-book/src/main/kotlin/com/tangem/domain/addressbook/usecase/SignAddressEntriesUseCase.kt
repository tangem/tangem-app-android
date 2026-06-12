package com.tangem.domain.addressbook.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SignHashesError
import com.tangem.domain.transaction.usecase.SignHashesUseCase
import com.tangem.utils.extensions.toHexString
import java.security.MessageDigest

/**
 * Signs every [AddressEntry] of a [Contact] with the wallet's primary key in a single signing
 * session (one card tap). Each entry is hashed as `SHA-256(address + networkId + memo + contactId +
 * name)` and the produced signature is stored back into [AddressEntry.signature].
 */
class SignAddressEntriesUseCase(
    private val signHashesUseCase: SignHashesUseCase,
) {

    suspend operator fun invoke(userWallet: UserWallet, contact: Contact): Either<SignHashesError, Contact> = either {
        val entries = contact.addressEntries
        if (entries.isEmpty()) return@either contact

        val hashes = entries.map { entry -> hashEntry(contact, entry) }
        val signatures = signHashesUseCase(userWallet = userWallet, hashes = hashes).bind()

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