package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository

/**
 * Use case for commiting hashes that failed to submit during staking confirmation
 */
class SendUnsubmittedHashesUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(): Either<StakingError, Unit> {
        return Either
            .catch { stakingRepository.sendUnsubmittedHashes() }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }
}
