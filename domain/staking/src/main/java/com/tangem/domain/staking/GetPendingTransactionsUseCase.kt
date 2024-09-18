package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingPendingTransactionRepository

/**
 * Use case for getting saved staking pending transactions.
 */
class GetPendingTransactionsUseCase(
    private val stakingPendingTransactionRepository: StakingPendingTransactionRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(): Either<StakingError, List<PendingTransaction>> {
        return Either.catch {
            stakingPendingTransactionRepository.getTransactions()
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }
}
