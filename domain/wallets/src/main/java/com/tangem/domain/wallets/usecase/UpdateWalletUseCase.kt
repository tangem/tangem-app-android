package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.common.CompletionResult
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.core.wallets.error.SaveWalletError
import com.tangem.domain.wallets.models.UpdateWalletError.*
import com.tangem.domain.core.wallets.UserWalletsListRepository

/**
 * Use case for updating user wallet
 *
 * @property userWalletsListManager user wallets list manager
 *
[REDACTED_AUTHOR]
 */
class UpdateWalletUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): Either<UpdateWalletError, UserWallet> {
        if (useNewRepository) {
            val userWallet = userWalletsListRepository.userWallets.value?.find { it.walletId == userWalletId }
                ?: return Either.Left(
                    UpdateWalletError.DataError(IllegalStateException("User wallet with id $userWalletId not found")),
                )
            val updatedWallet = update(userWallet)
            return userWalletsListRepository.saveWithoutLock(updatedWallet, canOverride = true)
                .mapLeft {
                    when (it) {
                        is SaveWalletError.DataError -> DataError(
                            IllegalStateException("Failed to update wallet: ${it.messageId}"),
                        )
                        is SaveWalletError.WalletAlreadySaved -> UpdateWalletError.NameAlreadyExists
                    }
                }
        }

        return either {
            when (val result = userWalletsListManager.update(userWalletId, update)) {
                is CompletionResult.Failure -> raise(UpdateWalletError.DataError(result.error))
                is CompletionResult.Success -> result.data
            }
        }
    }
}