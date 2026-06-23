package com.tangem.domain.addressbook.interactor

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.addressbook.model.AddressEntriesVerification
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.VerifiedContact
import com.tangem.domain.addressbook.usecase.GetContactsUseCase
import com.tangem.domain.addressbook.usecase.buildAddressEntryPayload
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.VerifyMessagesError
import com.tangem.domain.transaction.usecase.VerifySecp256k1MessagesUseCase
import com.tangem.utils.extensions.hexToBytesOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetVerifiedContactsInteractor(
    private val getContacts: GetContactsUseCase,
    private val verifyMessages: VerifySecp256k1MessagesUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    operator fun invoke(query: String, userWalletId: UserWalletId? = null): Flow<List<VerifiedContact>> {
        return getContacts(query, userWalletId).map { contacts ->
            val walletsById = userWalletsListRepository.userWalletsSync().associateBy { it.walletId }
            contacts.mapNotNull { contact ->
                val userWallet = walletsById[contact.walletId] ?: return@mapNotNull null
                val verification = verify(userWallet, contact).getOrNull() ?: return@mapNotNull null
                VerifiedContact(
                    contact = contact.copy(addressEntries = verification.valid),
                    invalidEntries = verification.invalid,
                )
            }
        }
    }

    private fun verify(
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

        return verifyMessages(userWallet = userWallet, messages = messages, signatures = signatures)
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