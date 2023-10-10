package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.firstOrNull

class GetUserWalletUseCase(private val walletsStateHolder: WalletsStateHolder) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Any, UserWallet> = either {
        val userWalletsListManager = ensureNotNull(
            value = walletsStateHolder.userWalletsListManager,
            raise = {
                val error = IllegalStateException("User wallets list manager not initialized")

                GetUserWalletError.DataError(error)
            },
        )

        val userWallets = userWalletsListManager.userWallets.firstOrNull().orEmpty()

        ensureNotNull(userWallets.firstOrNull { it.walletId == userWalletId }) {
            raise(GetUserWalletError.UserWalletNotFound)
        }
    }
}