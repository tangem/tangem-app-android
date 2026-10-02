package com.tangem.features.walletconnect.connections.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

/** MVP details of one MCP session: the session model pretty-printed plus the revoke action. */
internal data class McpSessionInfoUM(
    val debugText: String,
    val isRevoking: Boolean,
    val onRevokeClick: () -> Unit,
    val onDismiss: () -> Unit,
) : TangemBottomSheetConfigContent