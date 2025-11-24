package com.tangem.domain.staking.usecase

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.repositories.StakeKitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Emits a map of Validators values per currency for staking.
 *
 * Return map:
 * - key: currency staking key (network.backendId + "_" + symbol)
 * - value: validators
 */
class StakingApyFlowUseCase(private val stakeKitRepository: StakeKitRepository) {

    operator fun invoke(): Flow<Map<String, List<Yield.Validator>>> {
        return stakeKitRepository.getEnabledYields()
            .map { yields ->
                yields.associate { yield ->
                    val key = "${yield.token.coinGeckoId}_${yield.token.symbol}"
                    val apy = yield.validators
                    key to apy
                }
            }
    }
}