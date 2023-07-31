package com.tangem.core.ui.components.transactions.empty

import androidx.annotation.DrawableRes
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.extensions.TextReference

sealed class EmptyTransactionsBlockState {

    @get:DrawableRes
    abstract val iconRes: Int

    abstract val text: TextReference

    abstract val actionButtonConfig: ActionButtonConfig

    data class FailedToLoad(
        override val actionButtonConfig: ActionButtonConfig,
    ) : EmptyTransactionsBlockState() {
        override val iconRes: Int = R.drawable.ic_alert_history_64
        override val text: TextReference = TextReference.Res(R.string.transaction_history_error_failed_to_load)
    }

    data class Empty(
        override val actionButtonConfig: ActionButtonConfig,
    ) : EmptyTransactionsBlockState() {
        override val iconRes: Int = R.drawable.img_coin_64
        override val text: TextReference = TextReference.Res(R.string.transaction_history_empty_transactions)
    }

    data class NotImplemented(
        override val actionButtonConfig: ActionButtonConfig,
    ) : EmptyTransactionsBlockState() {
        override val iconRes: Int = R.drawable.ic_compass_64
        override val text: TextReference = TextReference.Res(R.string.transaction_history_not_supported_description)
    }
}