package com.tangem.core.ui.components.transactions.empty

import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.extensions.TextReference

sealed class EmptyTransactionsBlockState(
    val buttonsState: ButtonsState,
    val iconRes: Int,
    val text: TextReference,
) {

    sealed class ButtonsState {
        data class SingleButton(val actionButtonConfig: ActionButtonConfig) : ButtonsState()
        data class PairButtons(
            val firstButtonConfig: ActionButtonConfig,
            val secondButtonConfig: ActionButtonConfig,
        ) : ButtonsState()
    }

    data class FailedToLoad(
        val onReload: () -> Unit,
        val onExplore: () -> Unit,
    ) : EmptyTransactionsBlockState(
        buttonsState = ButtonsState.PairButtons(
            firstButtonConfig = ActionButtonConfig(
                text = TextReference.Res(R.string.common_reload),
                iconResId = R.drawable.ic_refresh_24,
                onClick = onReload,
                enabled = true,
            ),
            secondButtonConfig = ActionButtonConfig(
                text = TextReference.Res(R.string.common_explore),
                iconResId = R.drawable.ic_arrow_top_right_24,
                onClick = onExplore,
                enabled = true,
            ),
        ),
        iconRes = R.drawable.ic_alert_history_64,
        text = TextReference.Res(R.string.transaction_history_error_failed_to_load),
    )

    data class Empty(val onExplore: (() -> Unit)) : EmptyTransactionsBlockState(
        buttonsState = ButtonsState.SingleButton(
            actionButtonConfig = ActionButtonConfig(
                text = TextReference.Res(R.string.common_explore),
                iconResId = R.drawable.ic_arrow_top_right_24,
                onClick = onExplore,
                enabled = true,
            ),
        ),
        iconRes = R.drawable.ic_empty_token_64,
        text = TextReference.Res(R.string.transaction_history_empty_transactions),
    )

    data class NotImplemented(val onExplore: () -> Unit) : EmptyTransactionsBlockState(
        buttonsState = ButtonsState.SingleButton(
            actionButtonConfig = ActionButtonConfig(
                text = TextReference.Res(R.string.common_explore_transaction_history),
                iconResId = R.drawable.ic_arrow_top_right_24,
                onClick = onExplore,
                enabled = true,
            ),
        ),
        iconRes = R.drawable.ic_compass_64,
        text = TextReference.Res(R.string.transaction_history_not_supported_description),
    )
}