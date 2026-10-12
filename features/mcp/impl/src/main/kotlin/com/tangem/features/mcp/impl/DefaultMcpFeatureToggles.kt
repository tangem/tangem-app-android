package com.tangem.features.mcp.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.mcp.api.McpFeatureToggles

internal class DefaultMcpFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : McpFeatureToggles {

    override val isMcpEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1721_MCP_ENABLED)
}