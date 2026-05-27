package com.tangem.feature.swap.router

import com.tangem.core.decompose.navigation.Route

internal sealed interface SwapRoute : Route {
    data object Main : SwapRoute
    data object Success : SwapRoute
    data class SelectToken(val isFromDirection: Boolean) : SwapRoute
}