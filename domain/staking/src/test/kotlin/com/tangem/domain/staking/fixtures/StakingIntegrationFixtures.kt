package com.tangem.domain.staking.fixtures

import com.tangem.domain.staking.model.P2PEthPoolIntegration
import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.test.mock.staking.MockStaking

fun stakeKitIntegration(
    id: StakingIntegrationID.StakeKit = StakingIntegrationID.StakeKit.Coin.Solana,
    yield: Yield = MockStaking.yieldFor(id.value),
): StakeKitIntegration = StakeKitIntegration(integrationId = id, yield = yield)

fun p2pIntegration(
    vaults: List<P2PEthPoolVault> = listOf(MockStaking.vault()),
    vaultLimits: Map<String, VaultLimitInfo> = mapOf(MockStaking.VAULT_ADDRESS to MockStaking.vaultLimit()),
): P2PEthPoolIntegration = P2PEthPoolIntegration(
    integrationId = StakingIntegrationID.P2PEthPool,
    vaults = vaults,
    vaultLimits = vaultLimits,
)

val allStakeKitIds: List<StakingIntegrationID.StakeKit> =
    StakingIntegrationID.StakeKit.Coin.entries + StakingIntegrationID.StakeKit.EthereumToken.entries