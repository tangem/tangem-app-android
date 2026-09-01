package com.tangem.domain.staking

import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.toStakingTarget
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.repositories.StakeKitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Combines persisted StakeKit validators and P2PEthPool vaults from both repositories into a single lookup map,
 * keyed by address
 */
class GetStakingTargetsByAddressUseCase(
    private val stakeKitRepository: StakeKitRepository,
    private val p2pEthPoolRepository: P2PEthPoolRepository,
) {

    operator fun invoke(): Flow<Map<String, StakingTarget>> {
        return combine(
            stakeKitRepository.getPersistedValidatorsFlow(),
            p2pEthPoolRepository.getPersistedVaultsFlow(),
        ) { validators, vaults ->
            buildMap {
                validators.forEach { put(it.address, it.toStakingTarget()) }
                vaults.forEach { put(it.vaultAddress, it.toStakingTarget()) }
            }
        }
    }
}