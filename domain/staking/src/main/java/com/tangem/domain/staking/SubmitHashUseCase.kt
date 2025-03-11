package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.SubmitHashData
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

    suspend operator fun invoke(submitHashData: SubmitHashData): Either<StakingError, Unit> {
        return Either
            .catch {
                stakingTransactionHashRepository.submitHash(
                    transactionId = submitHashData.transactionId,
                    transactionHash = submitHashData.transactionHash,
                )
            }.mapLeft {
                stakingErrorResolver.resolve(it)
            }
    }
}