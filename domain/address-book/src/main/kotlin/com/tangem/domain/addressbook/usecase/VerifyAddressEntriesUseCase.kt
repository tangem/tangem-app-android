package com.tangem.domain.addressbook.usecase

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.addressbook.model.AddressEntriesVerification
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.VerifyMessagesError
import com.tangem.domain.transaction.usecase.VerifySecp256k1MessagesUseCase
import com.tangem.utils.extensions.hexToBytesOrNull

/**
 * Verifies each [AddressEntry] of a [Contact] against [userWallet] and partitions them into the ones
 * whose signature was produced by that wallet ([AddressEntriesVerification.valid]) and the ones that
 * were not ([AddressEntriesVerification.invalid]). The counterpart of [SignAddressEntriesUseCase].
 *
 * An entry is **invalid** when its signature fails verification or is missing/malformed (non-hex);
 * such entries should be hidden from the user. Both partitions preserve the contact's original entry
 * order. An empty contact yields two empty lists. The wallet's signing key being unavailable surfaces
 * as a [VerifyMessagesError.NoSigningKey] failure (the entries cannot be verified at all).
 *
 * Each entry is verified against the exact bytes that were signed (see [buildAddressEntryPayload]).
 */
class VerifyAddressEntriesUseCase(
    private val verifyMessagesUseCase: VerifySecp256k1MessagesUseCase,
) {

    operator fun invoke(
        userWallet: UserWallet,
        contact: Contact,
    ): Either<VerifyMessagesError, AddressEntriesVerification> {
        val entries = contact.addressEntries
        if (entries.isEmpty()) return AddressEntriesVerification(valid = emptyList(), invalid = emptyList()).right()

        // Entries with a malformed (non-hex) signature can't be verified — they are invalid by format.
        val wellFormed = entries.mapNotNull { entry ->
            entry.signature.hexToBytesOrNull()?.let { signature -> entry to signature }
        }
        val messages = wellFormed.map { (entry, _) -> buildAddressEntryPayload(contact, entry) }
        val signatures = wellFormed.map { (_, signature) -> signature }

        return verifyMessagesUseCase(userWallet = userWallet, messages = messages, signatures = signatures)
            .map { flags ->
                val validIds = wellFormed
                    .filterIndexed { index, _ -> flags[index] }
                    .mapTo(HashSet()) { (entry, _) -> entry.id }

                AddressEntriesVerification(
                    valid = entries.filter { it.id in validIds },
                    invalid = entries.filterNot { it.id in validIds },
                )
            }
    }
}