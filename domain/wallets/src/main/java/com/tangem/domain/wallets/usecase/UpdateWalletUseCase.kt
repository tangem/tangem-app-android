package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.wallets.models.UpdateWalletError.DataError

/**
 * Use case for updating user wallet
 *
 * @property userWalletsListRepository repository for getting list of user wallets
 *
[REDACTED_AUTHOR]
 */
class UpdateWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): Either<UpdateWalletError, UserWallet> {
        val userWallet = userWalletsListRepository.userWallets.value?.find { it.walletId == userWalletId }
            ?: return Either.Left(
                DataError(IllegalStateException("User wallet with id $userWalletId not found")),
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
}