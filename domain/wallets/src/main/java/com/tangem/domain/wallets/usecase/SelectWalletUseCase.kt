package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SelectWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.redux.ReduxStateHolder

/**
 * Use case for selecting wallet
 *
 * @property userWalletsListRepository repository for getting list of user wallets
 * @property reduxStateHolder       redux state holder
 *
[REDACTED_AUTHOR]
 */
class SelectWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val reduxStateHolder: ReduxStateHolder,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<SelectWalletError, UserWallet> {
        return userWalletsListRepository.select(userWalletId).map {
            reduxStateHolder.onUserWalletSelected(it)
            it
        }
    }
}