package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.Contact

/**
 * Builds the canonical bytes that are signed for a single [AddressEntry]:
 * `address + networkId + memo + contactId + name`.
 *
 * Shared by `SaveContactInteractor` (which hashes and signs it) and `GetVerifiedContactsInteractor`
 * (which verifies the signature against it), so the signed and verified payloads can never diverge.
 */
internal fun buildAddressEntryPayload(contact: Contact, entry: AddressEntry): ByteArray {
    val payload = buildString {
        append(entry.address)
        append(entry.networkId.value)
        append(entry.memo.orEmpty())
        append(contact.id.value)
        append(contact.name.value)
    }
    return payload.toByteArray(Charsets.UTF_8)
}