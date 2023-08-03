package com.tangem.core.ui.components.transactions.empty

import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.extensions.TextReference

sealed class EmptyTransactionsBlockState(
    val iconRes: Int,
    val text: TextReference,
    val actionButtonConfig: ActionButtonConfig,
) {

    class FailedToLoad(onClick: () -> Unit) : EmptyTransactionsBlockState(
        actionButtonConfig = ActionButtonConfig(
            text = TextReference.Res(R.string.common_reload),
            iconResId = R.drawable.ic_refresh_24,
            onClick = onClick,
            enabled = true,
        ),
        iconRes = R.drawable.ic_alert_history_64,
        text = TextReference.Res(R.string.transaction_history_error_failed_to_load),
    )

    class Empty(onClick: (() -> Unit)?) : EmptyTransactionsBlockState(
        actionButtonConfig = ActionButtonConfig(
            text = TextReference.Res(R.string.common_buy),
            iconResId = R.drawable.ic_plus_24,
            onClick = onClick ?: {},
            enabled = onClick != null,
        ),
        iconRes = R.drawable.img_coin_64,
        text = TextReference.Res(R.string.transaction_history_empty_transactions),
    )

    class NotImplemented(onClick: () -> Unit) : EmptyTransactionsBlockState(
        actionButtonConfig = ActionButtonConfig(
            text = TextReference.Res(R.string.common_explore_transaction_history),
            iconResId = R.drawable.ic_arrow_top_right_24,
            onClick = onClick,
            enabled = true,
        ),
        iconRes = R.drawable.ic_compass_64,
        text = TextReference.Res(R.string.transaction_history_not_supported_description),
    )
}