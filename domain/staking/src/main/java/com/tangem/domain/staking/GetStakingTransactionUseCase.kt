package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import kotlinx.coroutines.delay

/**
 * Use case for creating enter action
 */
class GetStakingTransactionUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(params: ActionParams): Either<StakingError, StakingTransaction> {
        return Either.catch {
            val createAction = stakingRepository.createAction(params)

            // workaround, sometimes transaction is not created immediately after actions/enter
            delay(PATCH_TRANSACTION_REQUEST_DELAY)

            val createdTransaction = createAction.transactions?.get(0) ?: error("No available transaction to patch")
            val patchedTransaction = stakingRepository.constructTransaction(createdTransaction.id)

            patchedTransaction
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }

    companion object {
        private const val PATCH_TRANSACTION_REQUEST_DELAY = 1000L
    }
}
