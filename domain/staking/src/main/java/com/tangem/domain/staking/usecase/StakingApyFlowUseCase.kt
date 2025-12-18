package com.tangem.domain.staking.usecase

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.toStakingTarget
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Emits a map of StakingTarget values per currency for staking.
 *
 * Return map:
 * - key: currency staking key (coinGeckoId + "_" + symbol)
 * - value: list of staking targets (validators or vaults)
 */
class StakingApyFlowUseCase(
    private val stakeKitRepository: StakeKitRepository,
    private val p2pEthPoolRepository: P2PEthPoolRepository,
    private val stakingFeatureToggles: StakingFeatureToggles,
) {

    operator fun invoke(): Flow<Map<String, List<StakingTarget>>> {
        return combine(
            stakeKitRepository.getEnabledYields(),
            p2pEthPoolRepository.getVaultsFlow(),
        ) { yields, p2pVaults ->
            val stakeKitMap = yields.filterNot { yield ->
                val isCardanoYield = yield.token.coinGeckoId == Blockchain.Cardano.toCoinId()
                isCardanoYield && !stakingFeatureToggles.isCardanoStakingEnabled
            }.associate { yield ->
                val key = "${yield.token.coinGeckoId}_${yield.token.symbol}"
                val targets = yield.validators.map { it.toStakingTarget() }
                key to targets
            }

            val p2pMap = if (p2pVaults.isNotEmpty()) {
                val ethKey = "${Blockchain.Ethereum.toCoinId()}_${Blockchain.Ethereum.currency}"
                val targets = p2pVaults.map { it.toStakingTarget() }
                mapOf(ethKey to targets)
            } else {
                emptyMap()
            }

            stakeKitMap + p2pMap
        }
    }
}