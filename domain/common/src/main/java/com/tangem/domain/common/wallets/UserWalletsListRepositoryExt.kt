package com.tangem.domain.common.wallets

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Update user wallet by [userWalletId] and return updated wallet.
 *
 * @param userWalletId The ID of the user wallet to update.
 * @param transform A function that takes the current user wallet and returns the updated version.
 * @return Either containing the updated user wallet on success or an error if the update fails.
 */
suspend fun UserWalletsListRepository.update(
    userWalletId: UserWalletId,
    transform: suspend (UserWallet) -> UserWallet,
): Either<SaveWalletError, UserWallet> = either {
    val userWallet = userWallets.value?.find { it.walletId == userWalletId }
    requireNotNull(userWallet) { "Unable to find user wallet with provided ID: $userWalletId" }

    val updatedUserWallet = transform(userWallet)

    saveWithoutLock(
        userWallet = updatedUserWallet,
        canOverride = true,
    )
        .bind()

    updatedUserWallet
}