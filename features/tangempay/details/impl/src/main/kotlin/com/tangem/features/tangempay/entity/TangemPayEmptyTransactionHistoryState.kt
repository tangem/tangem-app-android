package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R

internal sealed class TangemPayEmptyTransactionHistoryState {

    abstract val iconRes: Int
    abstract val text: TextReference

    data class FailedToLoad(
        private val onReload: () -> Unit,
    ) : TangemPayEmptyTransactionHistoryState() {
        override val iconRes: Int = R.drawable.ic_alert_history_64
        override val text: TextReference = resourceReference(R.string.transaction_history_error_failed_to_load)
        val actionButtonConfig = ActionButtonConfig(
            text = resourceReference(R.string.common_reload),
            iconResId = R.drawable.ic_refresh_24,
            onClick = onReload,
        )
    }

    data object Empty : TangemPayEmptyTransactionHistoryState() {
        override val iconRes: Int = R.drawable.ic_empty_token_64
        override val text: TextReference = resourceReference(R.string.transaction_history_empty_transactions)
    }
}