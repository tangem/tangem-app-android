package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.firstOrNull

class GetUserWalletUseCase(private val userWalletsListManager: UserWalletsListManager) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<GetUserWalletError, UserWallet> = either {
        val userWallets = userWalletsListManager.userWallets.firstOrNull().orEmpty()

        ensureNotNull(userWallets.firstOrNull { it.walletId == userWalletId }) {
            raise(GetUserWalletError.UserWalletNotFound)
        }
    }
}
