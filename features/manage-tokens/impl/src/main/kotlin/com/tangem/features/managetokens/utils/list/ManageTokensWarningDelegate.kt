package com.tangem.features.managetokens.utils.list

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.managetokens.component.ManageTokensMode
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.managetokens.impl.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class ManageTokensWarningDelegate @AssistedInject constructor(
    private val messageSender: UiMessageSender,
    @Assisted private val mode: ManageTokensMode,
    @Assisted private val source: ManageTokensSource,
    @Assisted private val uiActions: ManageTokensUiActions,
) {

    suspend fun showRemoveNetworkWarning(
        currency: ManagedCryptoCurrency,
        network: Network,
        isCoin: Boolean,
        onConfirm: () -> Unit,
    ) {
        val isNonePortfolio = when (mode) {
            ManageTokensMode.None -> true
            is ManageTokensMode.Account,
            is ManageTokensMode.Wallet,
            -> false
        }
        val hasLinkedTokens = if (isNonePortfolio || !isCoin) {
            false
        } else {
            uiActions.checkHasLinkedTokens(network)
        }
        val canHideWithoutConfirming = source == ManageTokensSource.ONBOARDING

        if (hasLinkedTokens) {
            showLinkedTokensWarning(currency, network)
        } else if (canHideWithoutConfirming) {
            onConfirm()
        } else {
            showHideTokenWarning(currency, onConfirm)
        }
    }

    private fun showLinkedTokensWarning(currency: ManagedCryptoCurrency, network: Network) {
        val message = DialogMessage(
            title = resourceReference(
                id = R.string.token_details_unable_hide_alert_title,
                formatArgs = wrappedList(currency.name),
            ),
            message = resourceReference(
                id = R.string.token_details_unable_hide_alert_message,
                formatArgs = wrappedList(
                    currency.name,
                    currency.symbol,
                    network.name,
                ),
            ),
        )
        messageSender.send(message)
    }

    private fun showHideTokenWarning(currency: ManagedCryptoCurrency, onConfirm: () -> Unit) {
        val message = DialogMessage(
            title = resourceReference(
                id = R.string.token_details_hide_alert_title,
                formatArgs = wrappedList(currency.name),
            ),
            message = resourceReference(R.string.token_details_hide_alert_message),
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.token_details_hide_alert_hide),
                    warning = true,
                    onClick = onConfirm,
                )
            },
            secondActionBuilder = { cancelAction() },
        )

        messageSender.send(message)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            mode: ManageTokensMode,
            source: ManageTokensSource,
            uiActions: ManageTokensUiActions,
        ): ManageTokensWarningDelegate
    }
}