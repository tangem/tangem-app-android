package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.wallet.impl.R

internal object WalletAlertUM {

    fun unableHideToken(cryptoCurrency: CryptoCurrency): DialogMessage {
        return DialogMessage(
            title = resourceReference(
                id = R.string.token_details_unable_hide_alert_title,
                formatArgs = WrappedList(listOf(cryptoCurrency.name)),
            ),
            message = resourceReference(
                id = R.string.token_details_unable_hide_alert_message,
                formatArgs = WrappedList(
                    listOf(
                        cryptoCurrency.name,
                        cryptoCurrency.symbol,
                        cryptoCurrency.network.name,
                    ),
                ),
            ),
        )
    }

    fun hideTokenConfirm(cryptoCurrency: CryptoCurrency, onClick: () -> Unit): DialogMessage {
        return DialogMessage(
            title = resourceReference(
                id = R.string.token_details_hide_alert_title,
                formatArgs = WrappedList(listOf(cryptoCurrency.name)),
            ),
            message = resourceReference(R.string.token_details_hide_alert_message),
            firstActionBuilder = {
                okAction(onClick)
            },
        )
    }

    fun providersStillLoading(): DialogMessage {
        return DialogMessage(
            title = resourceReference(R.string.action_buttons_service_loading_alert_title),
            message = resourceReference(R.string.action_buttons_service_loading_alert_message),
        )
    }

    fun unavailableOperation(): DialogMessage {
        return DialogMessage(
            title = resourceReference(R.string.action_buttons_something_wrong_alert_title),
            message = resourceReference(R.string.action_buttons_something_wrong_alert_message),
        )
    }

    fun insufficientTokensCountForSwapping(): DialogMessage {
        return DialogMessage(
            title = resourceReference(R.string.action_buttons_swap_no_tokens_added_alert_title),
            message = resourceReference(R.string.action_buttons_swap_no_tokens_added_alert_message),
        )
    }

    fun qrCodeUnrecognized(): DialogMessage {
        return DialogMessage(
            title = resourceReference(R.string.qr_scanner_error_unrecognized_title),
            message = resourceReference(R.string.qr_scanner_error_unrecognized_message),
        )
    }

    fun qrCodeUnsupportedNetwork(): DialogMessage {
        return DialogMessage(
            title = resourceReference(R.string.qr_scanner_error_token_not_added_title),
            message = resourceReference(R.string.qr_scanner_error_token_not_added_message),
        )
    }

    fun qrCodeUnsupportedParams(unsupportedParams: Map<String, String>, onContinue: () -> Unit): DialogMessage {
        val paramsList = unsupportedParams.entries.joinToString { "${it.key} = ${it.value}" }
        return DialogMessage(
            title = resourceReference(R.string.qr_scanner_warning_unknown_parameters_title),
            message = resourceReference(
                id = R.string.qr_scanner_warning_unknown_parameters_message,
                formatArgs = WrappedList(listOf(paramsList)),
            ),
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.common_continue),
                    onClick = onContinue,
                )
            },
            secondActionBuilder = { cancelAction() },
        )
    }

    fun qrCodeAddressSameAsWallet(): DialogMessage {
        return DialogMessage(
            message = resourceReference(R.string.send_error_address_same_as_wallet),
        )
    }

    fun confirmExpressStatusHide(onConfirmClick: () -> Unit): DialogMessage {
        return DialogMessage(
            title = resourceReference(R.string.express_status_hide_dialog_title),
            message = resourceReference(R.string.express_status_hide_dialog_text),
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.common_hide),
                    onClick = onConfirmClick,
                )
            },
            secondActionBuilder = { cancelAction() },
        )
    }
}