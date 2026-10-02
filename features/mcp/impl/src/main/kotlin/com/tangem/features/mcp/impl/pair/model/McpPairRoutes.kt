package com.tangem.features.mcp.impl.pair.model

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
@Immutable
internal sealed interface McpPairRoutes : Route {

    @Serializable
    data object AgentInfo : McpPairRoutes

    @Serializable
    data object PortfolioSelector : McpPairRoutes
}