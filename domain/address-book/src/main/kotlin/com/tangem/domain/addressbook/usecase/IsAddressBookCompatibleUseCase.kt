package com.tangem.domain.addressbook.usecase

import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Emits whether the stored address book(s) can be used by this build — `false` when the backend contract
 * version is newer than the one this app supports, so consumers can degrade (hide the book, block editing).
 *
 * @param userWalletId a specific wallet, or `null` to check every wallet (compatible only when all stored
 * books are compatible). See [AddressBookRepository.isAddressBookCompatible].
 */
class IsAddressBookCompatibleUseCase(
    private val repository: AddressBookRepository,
) {

    operator fun invoke(userWalletId: UserWalletId? = null): Flow<Boolean> =
        repository.isAddressBookCompatible(userWalletId)
}