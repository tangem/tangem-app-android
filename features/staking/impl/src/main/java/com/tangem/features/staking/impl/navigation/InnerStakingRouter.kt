package com.tangem.features.staking.impl.navigation

import com.tangem.features.staking.api.navigation.StakingRouter

interface InnerStakingRouter : StakingRouter {

    fun openUrl(url: String)
}