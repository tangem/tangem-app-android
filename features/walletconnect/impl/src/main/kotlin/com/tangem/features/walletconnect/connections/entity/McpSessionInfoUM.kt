package com.tangem.features.walletconnect.connections.entity

import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

/** Details of one connected MCP agent: who it is, where it is connected, and the revoke action. */
internal data class McpSessionInfoUM(
    val agentName: String,
    val agentOrigin: String,
    val agentIconUrl: String?,
    val connectedAt: String,
    val wallet: PortfolioSelectUM,
    val isRevoking: Boolean,
    val onRevokeClick: () -> Unit,
    val onDismiss: () -> Unit,
) : TangemBottomSheetConfigContent