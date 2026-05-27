package com.tangem.data.staking.toggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.toggles.StakingFeatureToggles

internal class DefaultStakingFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : StakingFeatureToggles {

    override fun isIntegrationEnabled(integrationId: StakingIntegrationID): Boolean {
        val toggle = integrationId.getFeatureToggle() ?: return true
        return featureTogglesManager.isFeatureEnabled(toggle)
    }

    private fun StakingIntegrationID.getFeatureToggle(): FeatureToggles? = when (this) {
        is StakingIntegrationID.P2PEthPool -> FeatureToggles.STAKING_ETH_ENABLED
        is StakingIntegrationID.StakeKit -> this.getStakeKitFeatureToggle()
    }

    private fun StakingIntegrationID.StakeKit.getStakeKitFeatureToggle(): FeatureToggles? = when (this) {
        is StakingIntegrationID.StakeKit.Coin -> when (this) {
            StakingIntegrationID.StakeKit.Coin.Ton,
            StakingIntegrationID.StakeKit.Coin.Solana,
            StakingIntegrationID.StakeKit.Coin.Cosmos,
            StakingIntegrationID.StakeKit.Coin.Tron,
            StakingIntegrationID.StakeKit.Coin.BSC,
            StakingIntegrationID.StakeKit.Coin.Cardano,
            -> null
        }
        is StakingIntegrationID.StakeKit.EthereumToken -> when (this) {
            StakingIntegrationID.StakeKit.EthereumToken.Polygon -> null
        }
    }
}