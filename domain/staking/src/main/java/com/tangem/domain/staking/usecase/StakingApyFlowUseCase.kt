package com.tangem.domain.staking.usecase

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.repositories.StakingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/**
 * Emits a map of APY values per currency for staking.
 *
 * Return map:
 * - key: currency staking key (network.backendId + "_" + symbol)
 * - value: APY as string
 */
class StakingApyFlowUseCase(private val stakingRepository: StakingRepository) {

    operator fun invoke(): Flow<Map<String, BigDecimal>> {
        return stakingRepository.getEnabledYields()
            .map { yields ->
                yields.associate { yield ->
                    val key = "${yield.token.coinGeckoId}_${yield.token.symbol}"
                    val apy = calculateApy(yield)
                    key to apy
                }
            }
    }

    private fun calculateApy(yield: Yield): BigDecimal {
        val rates = yield.validators.mapNotNull { it.rewardInfo?.rate }
        return if (rates.isNotEmpty()) {
            rates.maxOf { it }
        } else {
            yield.apy
        }
    }
}