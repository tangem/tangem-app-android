package com.tangem.domain.staking.usecase

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.repositories.StakingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Emits a map of Validators values per currency for staking.
 *
 * Return map:
 * - key: currency staking key (network.backendId + "_" + symbol)
 * - value: validators
 */
class StakingApyFlowUseCase(private val stakingRepository: StakingRepository) {

    operator fun invoke(): Flow<Map<String, List<Yield.Validator>>> {
        return stakingRepository.getEnabledYields()
            .map { yields ->
                yields.associate { yield ->
                    val key = composeKey(yield)
                    val apy = yield.validators
                    key to apy
                }
            }
    }

    // so that cause for api.stakek.it coinGeckoId on BNB Smart chain different how we receive our backendId
    // coinGeckoId - binance
    // our backendId BNB Smart Chain - binance-smart-chain
    // our backendId BNB Beacon Chain - binance
    private fun composeKey(yield: Yield): String {
        return if (yield.token.symbol == StakingIntegrationID.Coin.BSC.blockchain.currency) {
            "${Blockchain.BSC.toNetworkId()}_${yield.token.symbol}"
        } else {
            "${yield.token.coinGeckoId}_${yield.token.symbol}"
        }
    }
}