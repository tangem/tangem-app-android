package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingTransactionHashRepository

/**
 * Use case for submitting transaction hash to stakekit
 */
class SubmitHashUseCase(
    private val stakingTransactionHashRepository: StakingTransactionHashRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend fun submitHash(transactionId: String, transactionHash: String): Either<StakingError, Unit> {
        return Either
            .catch {
                stakingTransactionHashRepository.submitHash(
                    transactionId = transactionId,
                    transactionHash = transactionHash,
                )
            }.mapLeft {
                stakingErrorResolver.resolve(it)
            }
    }
}