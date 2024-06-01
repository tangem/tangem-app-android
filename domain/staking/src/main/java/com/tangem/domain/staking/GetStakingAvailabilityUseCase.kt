package com.tangem.domain.staking

import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.repositories.StakingRepository

/**
 * Use case for getting info about staking capability in tangem app.
 */
class GetStakingAvailabilityUseCase(
    private val stakingRepository: StakingRepository,
) {

    suspend operator fun invoke(currencyId: String?, symbol: String): StakingAvailability {
        return if (currencyId == null) {
            StakingAvailability.Unavailable
        } else {
            stakingRepository.getStakingAvailabilityForActions(currencyId, symbol)
        }
    }
}
