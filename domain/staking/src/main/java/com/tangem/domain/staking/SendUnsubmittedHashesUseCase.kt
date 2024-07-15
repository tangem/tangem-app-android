package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.repositories.StakingRepository

/**
 * Use case for commiting hashes that failed to submit during staking confirmation
 */
class SendUnsubmittedHashesUseCase(private val stakingRepository: StakingRepository) {

    suspend operator fun invoke(): Either<Throwable, Unit> {
        return Either.catch {
            stakingRepository.sendUnsubmittedHashes()
        }
    }
}