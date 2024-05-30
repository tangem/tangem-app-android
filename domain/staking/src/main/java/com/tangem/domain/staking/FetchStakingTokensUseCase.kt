package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.StakingTokenWithYield
import com.tangem.domain.staking.repositories.StakingRepository
import kotlinx.coroutines.coroutineScope

/**
 * Use case for getting enabled tokens
 */
class FetchStakingTokensUseCase(
    private val stakingRepository: StakingRepository,
) {
    suspend operator fun invoke() {
        Either.catch { stakingRepository.fetchEnabledTokens() }
    }
}
