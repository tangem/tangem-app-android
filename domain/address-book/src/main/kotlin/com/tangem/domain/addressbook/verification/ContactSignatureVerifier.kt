package com.tangem.domain.addressbook.verification

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.usecase.buildAddressEntryPayload
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.VerifyMessagesError
import com.tangem.domain.transaction.usecase.VerifySecp256k1MessagesUseCase
import com.tangem.utils.extensions.hexToBytesOrNull

class ContactSignatureVerifier(
    private val verifyMessages: VerifySecp256k1MessagesUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    suspend fun verifyContacts(contacts: List<Contact>): List<Contact> {
        val walletsById = userWalletsListRepository.userWalletsSync().associateBy { it.walletId }
        return contacts.mapNotNull { contact ->
            val userWallet = walletsById[contact.walletId] ?: return@mapNotNull null
            val validEntries = verify(userWallet, contact).getOrNull() ?: return@mapNotNull null
            contact.copy(addresses = validEntries).takeIf { validEntries.isNotEmpty() }
        }
    }

    suspend fun isNameVerified(contact: Contact): Boolean {
        val userWallet = userWalletsListRepository.userWalletsSync()
            .firstOrNull { it.walletId == contact.walletId } ?: return false
        return verify(userWallet, contact).getOrNull()?.isNotEmpty() == true
    }

    private fun verify(userWallet: UserWallet, contact: Contact): Either<VerifyMessagesError, List<AddressEntry>> {
        val entries = contact.addresses
        if (entries.isEmpty()) return emptyList<AddressEntry>().right()

        // Entries with a malformed (non-hex) signature can't be verified — they are invalid by format.
        val wellFormed = entries.mapNotNull { entry ->
            entry.signature.hexToBytesOrNull()?.let { signature -> entry to signature }
        }
        val messages = wellFormed.map { (entry, _) -> buildAddressEntryPayload(contact, entry) }
        val signatures = wellFormed.map { (_, signature) -> signature }

        return verifyMessages(userWallet = userWallet, messages = messages, signatures = signatures)
            .map { flags ->
                val validIds = wellFormed
                    .filterIndexed { index, _ -> flags[index] }
                    .mapTo(HashSet()) { (entry, _) -> entry.id }

                entries.filter { it.id in validIds }
            }
    }
}