package com.tangem.domain.staking.toggles

import com.tangem.domain.staking.model.StakingIntegrationID

interface StakingFeatureToggles {

    fun isIntegrationEnabled(integrationId: StakingIntegrationID): Boolean
}