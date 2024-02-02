package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.send.impl.R

sealed class SendFeeNotification(val config: NotificationConfig) {

    sealed class Warning(
        val title: TextReference,
        val subtitle: TextReference,
        val buttonsState: NotificationConfig.ButtonsState? = null,
    ) : SendFeeNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = R.drawable.img_attention_20,
            buttonsState = buttonsState,
        ),
    ) {
        object TooLow : Warning(
            title = resourceReference(id = R.string.send_notification_transaction_delay_title),
            subtitle = resourceReference(id = R.string.send_notification_transaction_delay_text),
        )

        data class TooHigh(
            val value: String,
        ) : Warning(
            title = resourceReference(id = R.string.send_notification_fee_too_high_title),
            subtitle = resourceReference(id = R.string.send_notification_fee_too_high_text, wrappedList(value)),
        )

        object NetworkCoverage : Warning(
            title = resourceReference(id = R.string.send_network_fee_warning_title),
            subtitle = resourceReference(id = R.string.send_network_fee_warning_content),
        )

        data class NetworkFeeUnreachable(val onRefresh: () -> Unit) : Warning(
            title = resourceReference(R.string.send_fee_unreachable_error_title),
            subtitle = resourceReference(R.string.send_fee_unreachable_error_text),
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.warning_button_refresh),
                onClick = onRefresh,
            ),
        )
    }

    sealed class Error(
        val title: TextReference,
        val subtitle: TextReference,
        val iconResId: Int,
        val buttonsState: NotificationConfig.ButtonsState? = null,
    ) : SendFeeNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = iconResId,
            buttonsState = buttonsState,
        ),
    ) {
        data class ExceedsBalance(
            val networkIconId: Int,
            val currencyName: String,
            val feeName: String,
            val feeSymbol: String,
            val networkName: String,
            val onClick: (() -> Unit)? = null,
        ) : Error(
            title = resourceReference(
                id = R.string.warning_send_blocked_funds_for_fee_title,
                wrappedList(feeName),
            ),
            subtitle = resourceReference(
                id = R.string.warning_send_blocked_funds_for_fee_message,
                formatArgs = wrappedList(currencyName, networkName, currencyName, feeName, feeSymbol),
            ),
            iconResId = networkIconId,
            buttonsState = onClick?.let {
                NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = resourceReference(R.string.common_buy_currency, wrappedList(feeName)),
                    onClick = onClick,
                )
            },
        )
    }
}