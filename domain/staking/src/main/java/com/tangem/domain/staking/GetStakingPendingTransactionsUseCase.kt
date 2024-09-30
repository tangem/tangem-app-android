package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingPendingTransactionRepository
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for getting saved staking pending transactions.
 */
class GetStakingPendingTransactionsUseCase(
    private val stakingPendingTransactionRepository: StakingPendingTransactionRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(userWalletId: UserWalletId): Either<StakingError, List<BalanceItem>> {
        return Either.catch {
            stakingPendingTransactionRepository.getTransactionsWithBalanceItems(userWalletId).map { it.second }
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }
}