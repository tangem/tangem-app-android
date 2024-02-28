package com.tangem.features.send.impl.presentation.state

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.send.impl.R

internal sealed class SendNotification(val config: NotificationConfig) {

    sealed class Error(
        title: TextReference,
        subtitle: TextReference,
        buttonState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : SendNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = R.drawable.ic_alert_24,
            buttonsState = buttonState,
            onCloseClick = onCloseClick,
        ),
    ) {

        object TotalExceedsBalance : Error(
            title = resourceReference(R.string.send_notification_exceed_balance_title),
            subtitle = resourceReference(R.string.send_notification_exceed_balance_text),
        )

        object InvalidAmount : Error(
            title = resourceReference(R.string.send_notification_invalid_amount_title),
            subtitle = resourceReference(R.string.send_notification_invalid_amount_text),
        )

        data class MinimumAmountError(val amount: String) : Error(
            title = resourceReference(R.string.send_notification_invalid_amount_title),
            subtitle = resourceReference(R.string.send_notification_invalid_minimum_amount_text, wrappedList(amount)),
        )

        data class ReserveAmountError(val amount: String) : Error(
            title = resourceReference(R.string.send_notification_invalid_reserve_amount_title, wrappedList(amount)),
            subtitle = resourceReference(R.string.send_notification_invalid_reserve_amount_text),
        )

        data class TransactionLimitError(
            val cryptoCurrency: String,
            val utxoLimit: String,
            val amountLimit: String,
            val onConfirmClick: () -> Unit,
        ) : Error(
            title = resourceReference(R.string.send_notifiaction_transaction_limit_title),
            subtitle = resourceReference(
                R.string.send_notification_transaction_limit_text,
                wrappedList(cryptoCurrency, utxoLimit, amountLimit),
            ),
            buttonState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(R.string.send_notification_reduce_to, wrappedList(amountLimit)),
                onClick = onConfirmClick,
            ),
        )
    }

    sealed class Warning(
        title: TextReference,
        subtitle: TextReference,
        buttonsState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : SendNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = R.drawable.img_attention_20,
            buttonsState = buttonsState,
            onCloseClick = onCloseClick,
        ),
    ) {
        data class HighFeeError(
            val amount: String,
            val onConfirmClick: () -> Unit,
            val onCloseClick: () -> Unit,
        ) : Warning(
            title = resourceReference(R.string.send_notification_high_fee_title),
            subtitle = resourceReference(R.string.send_notification_high_fee_text, wrappedList(amount)),
            buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(R.string.send_notification_reduce_by, wrappedList(amount)),
                onClick = onConfirmClick,
            ),
            onCloseClick = onCloseClick,
        )

        data class ExistentialDeposit(val deposit: String) : Warning(
            title = resourceReference(R.string.send_notification_existential_deposit_title),
            subtitle = resourceReference(R.string.send_notification_existential_deposit_text, wrappedList(deposit)),
        )

        data class NetworkCoverage(
            val amountReducedBy: String,
            val amountReduced: String,
        ) : Warning(
            title = resourceReference(id = R.string.send_network_fee_warning_title),
            subtitle = resourceReference(
                id = R.string.send_network_fee_warning_content,
                formatArgs = wrappedList(amountReducedBy, amountReduced),
            ),
        )

        data object FeeTooLow : Warning(
            title = resourceReference(id = R.string.send_notification_transaction_delay_title),
            subtitle = resourceReference(id = R.string.send_notification_transaction_delay_text),
        )
    }
}
