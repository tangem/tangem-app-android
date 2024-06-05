package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.repositories.StakingRepository

/**
 * Use case for getting enabled tokens
 */
class FetchStakingTokensUseCase(
    private val stakingRepository: StakingRepository,
) {
    suspend operator fun invoke(): Either<Throwable, Unit> {
        return Either.catch { stakingRepository.fetchEnabledTokens() }
    }
}