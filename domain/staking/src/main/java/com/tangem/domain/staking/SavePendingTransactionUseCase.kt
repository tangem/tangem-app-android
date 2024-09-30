package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingPendingTransactionRepository
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for saving hash that failed to submit during staking confirmation
 */
class SavePendingTransactionUseCase(
    private val stakingPendingTransactionRepository: StakingPendingTransactionRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(userWalletId: UserWalletId, transaction: PendingTransaction): Either<StakingError, Unit> {
        return Either.catch {
            stakingPendingTransactionRepository.saveTransaction(
                userWalletId = userWalletId,
                transaction = transaction,
            )
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }
}
