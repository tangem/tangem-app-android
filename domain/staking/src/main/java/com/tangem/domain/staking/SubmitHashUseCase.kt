package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.SubmitHashData
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakeKitTransactionHashRepository

/**
 * Use case for submitting transaction hash to stakekit
 */
class SubmitHashUseCase(
    private val stakeKitTransactionHashRepository: StakeKitTransactionHashRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(submitHashData: SubmitHashData): Either<StakingError, Unit> {
        return Either
            .catch {
                stakeKitTransactionHashRepository.submitHash(
                    transactionId = submitHashData.transactionId,
                    transactionHash = submitHashData.transactionHash,
                )
            }.mapLeft {
                stakingErrorResolver.resolve(it)
            }
    }
}