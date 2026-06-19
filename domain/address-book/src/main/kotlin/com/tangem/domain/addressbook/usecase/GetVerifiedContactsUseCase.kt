package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.model.VerifiedContact
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetVerifiedContactsUseCase(
    private val getContacts: GetContactsUseCase,
    private val verifyAddressEntries: VerifyAddressEntriesUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    operator fun invoke(query: String, userWalletId: UserWalletId? = null): Flow<List<VerifiedContact>> {
        return getContacts(query, userWalletId).map { contacts ->
            val walletsById = userWalletsListRepository.userWalletsSync().associateBy { it.walletId }
            contacts.mapNotNull { contact ->
                val userWallet = walletsById[contact.walletId] ?: return@mapNotNull null
                val verification = verifyAddressEntries(userWallet, contact).getOrNull() ?: return@mapNotNull null
                VerifiedContact(
                    contact = contact.copy(addressEntries = verification.valid),
                    invalidEntries = verification.invalid,
                )
            }
        }
    }
}