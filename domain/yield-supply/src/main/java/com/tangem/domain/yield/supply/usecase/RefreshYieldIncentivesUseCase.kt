package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository

/** Forgets the cached rewards of an address, so the next read reflects a claim that has just settled. */
class RefreshYieldIncentivesUseCase(private val yieldIncentivesRepository: YieldIncentivesRepository) {

    suspend operator fun invoke(chainId: Int, userAddress: String) {
        yieldIncentivesRepository.invalidateUserCache(chainId = chainId, userAddress = userAddress)
    }
}