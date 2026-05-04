package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SelectWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case for selecting wallet.
 *
 * Side effects tied to selection (analytics tracking context, Tangem SDK display config, access
 * code request policy) are fired from the repository itself when the selected [UserWalletId]
 * changes — see the implementation of [UserWalletsListRepository.select].
 *
 * @property userWalletsListRepository repository for getting list of user wallets
 *
[REDACTED_AUTHOR]
 */
class SelectWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<SelectWalletError, UserWallet> {
        return userWalletsListRepository.select(userWalletId)
    }
}