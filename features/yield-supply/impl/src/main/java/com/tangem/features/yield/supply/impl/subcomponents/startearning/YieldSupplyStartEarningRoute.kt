package com.tangem.features.yield.supply.impl.subcomponents.startearning

import com.tangem.core.decompose.navigation.Route

internal sealed class YieldSupplyStartEarningRoute : Route {
    data object Action : YieldSupplyStartEarningRoute()
    data object FeePolicy : YieldSupplyStartEarningRoute()
}