package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.wallet.impl.R

internal object WalletAlertUM {

    fun seedPhraseConfirm(onClick: () -> Unit): DialogMessage {
        return DialogMessage(
            message = resourceReference(R.string.warning_seedphrase_issue_answer_yes),
            firstActionBuilder = {
                okAction(onClick = onClick)
            },
        )
    }

    fun seedPhraseDismiss(onClick: () -> Unit): DialogMessage {
        return DialogMessage(
            message = resourceReference(R.string.warning_seedphrase_issue_answer_no),
            firstActionBuilder = {
                okAction(onClick = onClick)
            },
        )
    }

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