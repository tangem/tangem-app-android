package com.tangem.features.txhistory.joint.requests.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import com.tangem.core.ui.R
import com.tangem.core.ui.components.transactions.TransactionItem
import com.tangem.core.ui.components.transactions.TxHistoryDateHeader
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.txhistory.entity.JointCosignRequestsUM

internal fun LazyListScope.jointCosignRequestItems(state: JointCosignRequestsUM) {
    if (state.items.isEmpty()) return

    item(key = "joint_cosign_requests_header", contentType = "joint_cosign_requests_header") {
        val title = if (state.isSending) {
            R.string.joint_operation_sending_to_network
        } else {
            R.string.transaction_history_pending
        }
        TxHistoryDateHeader(title = stringResourceSafe(title))
    }
    items(
        items = state.items,
        key = { item -> "joint_cosign_request:${item.txHash}" },
        contentType = { TransactionItemUM::class.java },
    ) { item ->
        TransactionItem(state = item, isBalanceHidden = state.isBalanceHidden)
    }
}