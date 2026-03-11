package com.tangem.domain.wallets.delegate

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.copy
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.wallets.models.UserWalletRemoteInfo

class DefaultUserWalletsSyncDelegate(
    private val userWalletsListRepository: UserWalletsListRepository,
) : UserWalletsSyncDelegate {

    override suspend fun syncWallet(userWalletId: UserWalletId, name: String): Either<UpdateWalletError, UserWallet> {
        return renameUserWallet(userWalletId, name)
    }

    override suspend fun syncWallets(list: List<UserWalletRemoteInfo>): Either<UpdateWalletError, Unit> = either {
        list.forEach { userWallet ->
            renameUserWallet(userWallet.walletId, userWallet.name).bind()
        }
    }

    private suspend fun renameUserWallet(
        userWalletId: UserWalletId,
        name: String,
    ): Either<UpdateWalletError, UserWallet> = either {
        val userWallets = userWalletsListRepository.userWalletsSync()
        val userWallet = userWallets.find { it.walletId == userWalletId }
            ?: raise(UpdateWalletError.DataError(IllegalStateException("User wallet with id $userWalletId not found")))

        ensure(userWallets.none { it.name == name && it.walletId != userWalletId }) {
            UpdateWalletError.NameAlreadyExists
        }

        ensure(name != userWallet.name) {
            UpdateWalletError.NameAlreadyExists
        }

        val updatedWallet = userWallet.copy(name = name)

        userWalletsListRepository.saveWithoutLock(updatedWallet, canOverride = true)
            .map { updatedWallet }
            .mapLeft { error -> UpdateWalletError.DataError(IllegalStateException("$error")) }
            .bind()
    }
}