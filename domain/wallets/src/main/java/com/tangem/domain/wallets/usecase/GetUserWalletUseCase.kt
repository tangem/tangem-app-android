package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.requireUserWalletsSync
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.GetUserWalletError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

class GetUserWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Either<GetUserWalletError, UserWallet> = either {
        val userWallets = userWalletsListRepository.requireUserWalletsSync()

        ensureNotNull(userWallets.firstOrNull { it.walletId == userWalletId }) {
            raise(GetUserWalletError.UserWalletNotFound)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun invokeFlow(userWalletId: UserWalletId): EitherFlow<GetUserWalletError, UserWallet> {
        val flow = userWalletsListRepository.userWallets.map { requireNotNull(it) }

        return flow.transformLatest { userWallets ->
            userWallets.firstOrNull { it.walletId == userWalletId }
                ?.let { emit(it.right()) }
                ?: emit(GetUserWalletError.UserWalletNotFound.left())
        }
    }
}