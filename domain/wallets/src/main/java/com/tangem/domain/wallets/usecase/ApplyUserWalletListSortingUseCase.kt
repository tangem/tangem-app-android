package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.common.wallets.UserWalletTransformAction
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case to apply a specific sorting order to a list of user wallets.
 *
 * @property userWalletsListRepository Repository for managing user wallets list.
 */
class ApplyUserWalletListSortingUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    /**
     * Applies the sorting order of the provided list of user wallet IDs.
     *
     * @param userWalletIds List of [UserWalletId] representing the desired order.
     * @return Either an [Error] or Unit on successful completion.
     */
    suspend operator fun invoke(userWalletIds: List<UserWalletId>): Either<Error, Unit> = either {
        ensure(userWalletIds.size > 1) { Error.UnableToSortSingleWallet }
        catch(
            block = {
                userWalletsListRepository.transform(UserWalletTransformAction.Reorder { userWalletIds })
            },
            catch = { raise(Error.ReorderError) },
        )
    }

    /**
     * Sealed interface representing possible errors that can occur during the application
     * of user wallet list sorting.
     */
    sealed interface Error {

        data object UnableToSortSingleWallet : Error

        data object ReorderError : Error
    }
}