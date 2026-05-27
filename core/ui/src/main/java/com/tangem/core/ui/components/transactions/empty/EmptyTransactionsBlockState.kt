package com.tangem.core.ui.components.transactions.empty

import androidx.annotation.DrawableRes
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
        @DrawableRes val reloadIconResId: Int,
        @DrawableRes val exploreIconResId: Int,
    ) : EmptyTransactionsBlockState(
        buttonsState = ButtonsState.PairButtons(
            firstButtonConfig = ActionButtonConfig(
                text = TextReference.Res(R.string.common_reload),
                iconResId = reloadIconResId,
                onClick = onReload,
                isEnabled = true,
            ),
            secondButtonConfig = ActionButtonConfig(
                text = TextReference.Res(R.string.common_explore),
                iconResId = exploreIconResId,
                onClick = onExplore,
                isEnabled = true,
            ),
        ),
        iconRes = R.drawable.ic_alert_history_64,
        text = TextReference.Res(R.string.transaction_history_error_failed_to_load),
    )

    data class Empty(
        val onExplore: () -> Unit,
        @DrawableRes val exploreIconResId: Int,
    ) : EmptyTransactionsBlockState(
        buttonsState = ButtonsState.SingleButton(
            actionButtonConfig = ActionButtonConfig(
                text = TextReference.Res(R.string.common_explore),
                iconResId = exploreIconResId,
                onClick = onExplore,
                isEnabled = true,
            ),
        ),
        iconRes = R.drawable.ic_empty_token_64,
        text = TextReference.Res(R.string.transaction_history_empty_transactions),
    )

    data class NotImplemented(
        val onExplore: () -> Unit,
        @DrawableRes val exploreIconResId: Int,
    ) : EmptyTransactionsBlockState(
        buttonsState = ButtonsState.SingleButton(
            actionButtonConfig = ActionButtonConfig(
                text = TextReference.Res(R.string.common_explore_transaction_history),
                iconResId = exploreIconResId,
                onClick = onExplore,
                isEnabled = true,
            ),
        ),
        iconRes = R.drawable.ic_compass_64,
        text = TextReference.Res(R.string.transaction_history_not_supported_description),
    )
}