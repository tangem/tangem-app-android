package com.tangem.feature.swap.models.states

import com.tangem.common.ui.R
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.utils.getExpressErrorMessage
import com.tangem.feature.swap.utils.getExpressErrorTitle

internal object SwapNotificationUM {

    sealed class Error(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.ic_alert_24,
        buttonState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : NotificationUM.Error(
        title = title,
        subtitle = subtitle,
        iconResId = iconResId,
        buttonState = buttonState,
        onCloseClick = onCloseClick,
    ) {
        data class GenericError(
            val title: TextReference = resourceReference(id = R.string.common_warning),
            val subtitle: TextReference?,
            val onConfirmClick: () -> Unit,
        ) : Error(
            title = title,
            subtitle = subtitle ?: resourceReference(id = R.string.common_unknown_error),
            buttonState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(R.string.common_retry),
                onClick = onConfirmClick,
            ),
        )

        data object ApprovalInProgressWarning : Error(
            title = resourceReference(R.string.warning_express_approval_in_progress_title),
            subtitle = resourceReference(R.string.warning_express_approval_in_progress_message),
        )

        data class TransactionInProgressWarning(
            val currencySymbol: String,
        ) : Error(
            title = resourceReference(R.string.warning_express_active_transaction_title),
            subtitle = resourceReference(
                id = R.string.warning_express_active_transaction_message,
                formatArgs = wrappedList(currencySymbol),
            ),
        )

        data class UnableToCoverFeeWarning(
            val fromToken: CryptoCurrency,
            val currencyName: String,
            val currencySymbol: String,
            val feeCurrency: CryptoCurrency?,
            val onConfirmClick: (CryptoCurrency) -> Unit,
        ) : Error(
            title = resourceReference(
                R.string.warning_express_not_enough_fee_for_token_tx_title,
                wrappedList(fromToken.network.name),
            ),
            subtitle = resourceReference(
                R.string.warning_express_not_enough_fee_for_token_tx_description,
                wrappedList(currencyName, currencySymbol),
            ),
            iconResId = fromToken.networkIconResId,
            buttonState = feeCurrency?.let {
                NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = resourceReference(R.string.common_buy_currency, wrappedList(currencySymbol)),
                    onClick = { onConfirmClick(it) },
                )
            },
        )

        data class MinimalAmountError(
            val amount: String,
        ) : Error(
            title = resourceReference(
                id = R.string.warning_express_too_minimal_amount_title,
                formatArgs = wrappedList(amount),
            ),
            subtitle = resourceReference(R.string.warning_express_wrong_amount_description),
        )

        data class MaximumAmountError(
            val amount: String,
        ) : Error(
            title = resourceReference(
                id = R.string.warning_express_too_maximum_amount_title,
                formatArgs = wrappedList(amount),
            ),
            subtitle = resourceReference(R.string.warning_express_wrong_amount_description),
            iconResId = R.drawable.ic_alert_circle_24,
        )
    }

    sealed class Warning(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.img_attention_20,
        buttonsState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : NotificationUM.Warning(
        title = title,
        subtitle = subtitle,
        iconResId = iconResId,
        buttonsState = buttonsState,
        onCloseClick = onCloseClick,
    ) {
        data class NoAvailableTokensToSwap(
            val tokenName: String,
        ) : Warning(
            title = resourceReference(
                com.tangem.feature.swap.presentation.R.string.warning_express_no_exchangeable_coins_title,
            ),
            subtitle = resourceReference(
                id = com.tangem.feature.swap.presentation.R.string.warning_express_no_exchangeable_coins_description,
                formatArgs = wrappedList(tokenName),
            ),
        )

        data class NeedReserveToCreateAccount(
            val receiveAmount: String,
            val receiveToken: String,
        ) : Warning(
            title = resourceReference(
                id = R.string.warning_express_notification_invalid_reserve_amount_title,
                formatArgs = wrappedList("$receiveAmount $receiveToken"),
            ),
            subtitle = resourceReference(R.string.send_notification_invalid_reserve_amount_text),
        )

        data class ReduceAmount(
            val currencyName: String,
            val amount: String,
            val onConfirmClick: () -> Unit,
        ) : Warning(
            title = resourceReference(R.string.send_notification_high_fee_title),
            subtitle = resourceReference(
                R.string.send_notification_high_fee_text,
                wrappedList(currencyName, amount),
            ),
            buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(
                    R.string.xtz_withdrawal_message_reduce,
                    wrappedList(amount),
                ),
                onClick = onConfirmClick,
            ),
        )

        data class ExpressError(
            val expressDataError: ExpressDataError,
            val onConfirmClick: () -> Unit,
        ) : Warning(
            title = getExpressErrorTitle(expressDataError),
            subtitle = getExpressErrorMessage(expressDataError),
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.warning_button_refresh),
                onClick = onConfirmClick,
            ),
        )

        data class ExpressGeneralError(
            val code: Int,
            val onConfirmClick: () -> Unit,
        ) : Warning(
            title = TextReference.Res(R.string.warning_express_refresh_required_title),
            subtitle = TextReference.Res(R.string.express_error_code, wrappedList(code)),
            buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = TextReference.Res(R.string.warning_button_refresh),
                onClick = onConfirmClick,
            ),
        )
    }

    sealed class Info(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.ic_alert_circle_24,
    ) : NotificationUM.Info(
        title = title,
        subtitle = subtitle,
        iconResId = iconResId,
    ) {
        data class PermissionNeeded(
            val providerName: String,
            val fromTokenSymbol: String,
        ) : Info(
            title = resourceReference(R.string.express_provider_permission_needed),
            subtitle = resourceReference(
                id = R.string.give_permission_swap_subtitle,
                formatArgs = wrappedList(providerName, fromTokenSymbol),
            ),
            iconResId = R.drawable.ic_locked_24,
        )
    }
}