package com.tangem.features.yield.supply.impl.subcomponents.active.model

import com.tangem.core.decompose.navigation.Route

internal sealed class YieldSupplyActiveRoute : Route {
    data object Info : YieldSupplyActiveRoute()
    data object Action : YieldSupplyActiveRoute()
}