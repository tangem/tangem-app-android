package com.tangem.features.mcp.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.mcp.api.McpFeatureToggles

internal class DefaultMcpFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : McpFeatureToggles {

    override val isIntentsHistoryEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_19454_MCP_INTENTS_HISTORY_ENABLED)
}