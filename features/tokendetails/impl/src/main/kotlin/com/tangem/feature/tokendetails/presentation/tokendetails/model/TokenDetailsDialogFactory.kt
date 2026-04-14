package com.tangem.feature.tokendetails.presentation.tokendetails.model

import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.tokendetails.impl.R
import javax.inject.Inject

@ModelScoped
internal class TokenDetailsDialogFactory @Inject constructor(
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
) {

    fun showConfirmHideToken(currency: CryptoCurrency, onConfirm: () -> Unit) {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(
                    id = R.string.token_details_hide_alert_title,
                    formatArgs = wrappedList(currency.name),
                ),
                message = resourceReference(R.string.token_details_hide_alert_message),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.token_details_hide_alert_hide),
                        isWarning = true,
                        onClick = onConfirm,
                    )
                },
                secondActionBuilder = { cancelAction() },
            ),
        )
    }

    fun showLinkedTokens(currency: CryptoCurrency) {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(
                    id = R.string.token_details_unable_hide_alert_title,
                    formatArgs = wrappedList(currency.symbol),
                ),
                message = resourceReference(
                    id = R.string.token_details_unable_hide_alert_message,
                    formatArgs = wrappedList(currency.name, currency.symbol, currency.network.name),
                ),
            ),
        )
    }

    fun showDismissIncompleteTransactionConfirm(onConfirm: () -> Unit) {
        uiMessageSender.send(
            DialogMessage(
                message = resourceReference(R.string.warning_kaspa_unfinished_token_transaction_discard_message),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_yes),
                        onClick = onConfirm,
                    )
                },
                secondActionBuilder = { cancelAction() },
            ),
        )
    }

    fun showError(text: TextReference) {
        uiMessageSender.send(DialogMessage(message = text))
    }
}