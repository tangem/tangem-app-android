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
    ) : SendNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = R.drawable.ic_alert_24,
            buttonsState = buttonState,
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
        ) : Error(
            title = resourceReference(R.string.send_notifiaction_transaction_limit_title),
            subtitle = resourceReference(
                R.string.send_notifiaction_transaction_limit_text,
                wrappedList(cryptoCurrency, utxoLimit, amountLimit),
            ),
        )
    }

    sealed class Warning(
        title: TextReference,
        subtitle: TextReference,
    ) : SendNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = R.drawable.img_attention_20,
        ),
    ) {
        data class HighFeeError(val amount: String) : Warning(
            title = resourceReference(R.string.send_notification_high_fee_title),
            subtitle = resourceReference(R.string.send_notification_high_fee_text, wrappedList(amount)),
        )

        data class ExistentialDeposit(val deposit: String) : Warning(
            title = resourceReference(R.string.send_notification_existential_deposit_title),
            subtitle = resourceReference(R.string.send_notification_existential_deposit_text, wrappedList(deposit)),
        )
    }
}