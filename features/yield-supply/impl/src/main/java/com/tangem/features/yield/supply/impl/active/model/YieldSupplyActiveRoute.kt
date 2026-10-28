package com.tangem.features.yield.supply.impl.active.model

import com.tangem.core.decompose.navigation.Route
import java.math.BigDecimal

internal sealed class YieldSupplyActiveRoute : Route {
    data object Exit : YieldSupplyActiveRoute()
    data object Approve : YieldSupplyActiveRoute()
    data class AboutApy(val baseApy: BigDecimal, val rewardsApy: BigDecimal) : YieldSupplyActiveRoute()
    data class Claim(val chainId: Int, val userAddress: String) : YieldSupplyActiveRoute()
}