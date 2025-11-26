package com.tangem.domain.staking.usecase

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Emits a map of Validators values per currency for staking.
 *
 * Return map:
 * - key: currency staking key (network.backendId + "_" + symbol)
 * - value: validators
 */
class StakingApyFlowUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingFeatureToggles: StakingFeatureToggles,
) {

    operator fun invoke(): Flow<Map<String, List<Yield.Validator>>> {
        return stakingRepository.getEnabledYields()
            .map { yields ->
                yields.filterNot { yield ->
                    val isCardanoYield = yield.token.coinGeckoId == Blockchain.Cardano.toCoinId()
                    isCardanoYield && !stakingFeatureToggles.isCardanoStakingEnabled
                }.associate { yield ->
                    val key = "${yield.token.coinGeckoId}_${yield.token.symbol}"
                    val apy = yield.validators
                    key to apy
                }
            }
    }
}