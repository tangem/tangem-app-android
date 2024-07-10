package com.tangem.domain.staking

import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency

/**
 * Use case for getting info about staking capability in tangem app.
 */
class GetStakingAvailabilityUseCase(
    private val stakingRepository: StakingRepository,
) {

    suspend operator fun invoke(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): StakingAvailability {
        return stakingRepository.getStakingAvailabilityForActions(cryptoCurrencyId, symbol)
    }
}