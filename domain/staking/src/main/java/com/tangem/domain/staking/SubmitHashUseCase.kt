package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.repositories.StakingRepository

/**
 * Use case for submitting transaction hash to stakekit
 */
class SubmitHashUseCase(private val stakingRepository: StakingRepository) {

    suspend fun submitHash(transactionId: String, transactionHash: String): Either<Throwable, Unit> {
        return Either.catch {
            stakingRepository.submitHash(
                transactionId = transactionId,
                transactionHash = transactionHash,
            )
        }
    }
}