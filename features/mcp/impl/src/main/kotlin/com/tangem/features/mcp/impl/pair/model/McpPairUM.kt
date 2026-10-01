package com.tangem.features.mcp.impl.pair.model

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface McpPairUM {

    val onDecline: () -> Unit

    data class Loading(override val onDecline: () -> Unit) : McpPairUM

    data class Content(
        val agent: McpAgentUM,
        val portfolioSelectRow: PortfolioSelectUM,
        val pushNotificationsWarning: McpPushNotificationsWarningUM?,
        val connectButton: McpConnectButtonUM,
        override val onDecline: () -> Unit,
    ) : McpPairUM
}

@Immutable
internal data class McpPushNotificationsWarningUM(val onOpenSettingsClick: () -> Unit)

@Immutable
internal data class McpAgentUM(
    val name: TextReference,
    val subtitle: TextReference,
    val iconUrl: String?,
    /** `getMcpDeviceVerification` response dumped as text, until there is a contract to lay out. */
    val verificationText: String,
)

@Immutable
internal data class McpConnectButtonUM(
    val isEnabled: Boolean,
    val isLoading: Boolean,
    val onClick: () -> Unit,
)