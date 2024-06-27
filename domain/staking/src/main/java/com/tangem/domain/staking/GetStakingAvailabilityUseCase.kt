package com.tangem.domain.staking

import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.repositories.StakingRepository

/**
 * Use case for getting info about staking availability for certain blockchain.
 */
class GetStakingAvailabilityUseCase(
    private val stakingRepository: StakingRepository,
) {

    operator fun invoke(blockchainNetworkId: String): StakingAvailability {
        return stakingRepository.getStakingAvailability(blockchainNetworkId)
    }
}