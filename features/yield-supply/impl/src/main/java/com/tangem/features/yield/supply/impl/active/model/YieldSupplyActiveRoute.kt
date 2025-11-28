package com.tangem.features.yield.supply.impl.active.model

import com.tangem.core.decompose.navigation.Route

internal sealed class YieldSupplyActiveRoute : Route {
    data object Exit : YieldSupplyActiveRoute()
    data object Approve : YieldSupplyActiveRoute()
}