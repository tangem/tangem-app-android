package com.tangem.features.mcp.impl.pair.model

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface McpPairUM {

    data object Loading : McpPairUM

    data class AgentInfo(
        val agent: McpAgentUM,
        val wallet: PortfolioSelectUM,
        val notificationsWarning: McpNotificationsWarningUM?,
        val connectButton: McpConnectButtonUM,
        val onDecline: () -> Unit,
    ) : McpPairUM

    data class Success(val agentName: TextReference, val onDone: () -> Unit) : McpPairUM
}

@Immutable
internal data class McpAgentUM(val name: TextReference, val origin: TextReference?)

@Immutable
internal data class McpNotificationsWarningUM(val onOpenSettingsClick: () -> Unit)

@Immutable
internal data class McpConnectButtonUM(val isEnabled: Boolean, val isLoading: Boolean, val onClick: () -> Unit)