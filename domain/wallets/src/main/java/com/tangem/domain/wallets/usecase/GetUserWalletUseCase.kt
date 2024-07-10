package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.transformLatest

class GetUserWalletUseCase(private val userWalletsListManager: UserWalletsListManager) {

    operator fun invoke(userWalletId: UserWalletId): Either<GetUserWalletError, UserWallet> = either {
        val userWallets = userWalletsListManager.userWalletsSync

        ensureNotNull(userWallets.firstOrNull { it.walletId == userWalletId }) {
            raise(GetUserWalletError.UserWalletNotFound)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun invokeFlow(userWalletId: UserWalletId): EitherFlow<GetUserWalletError, UserWallet> {
        return userWalletsListManager.userWallets.transformLatest { userWallets ->
            userWallets.firstOrNull { it.walletId == userWalletId }
                ?.let { emit(it.right()) }
                ?: emit(GetUserWalletError.UserWalletNotFound.left())
        }
    }
}
