package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for rename user wallet
 *
 * @property userWalletsListManager user wallets list manager
 */
class RenameWalletUseCase(private val userWalletsListManager: UserWalletsListManager) {

    suspend operator fun invoke(userWalletId: UserWalletId, name: String): Either<UpdateWalletError, UserWallet> {
        val existingNames = userWalletsListManager.userWalletsSync

        if (existingNames.any { it.name == name && it.walletId != userWalletId }) {
            return UpdateWalletError.NameAlreadyExists.left()
        }

        return either {
            userWalletsListManager.update(userWalletId) { it.copy(name = name) }
                .doOnSuccess { return it.right() }
                .doOnFailure { return UpdateWalletError.DataError.left() }

            return UpdateWalletError.DataError.left()
        }
    }
}