package com.tangem.tap.features.main

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.tap.features.main.model.ActionConfig
import com.tangem.tap.features.main.model.ModalNotification
import com.tangem.tap.features.main.model.Toast
import com.tangem.wallet.R

internal class NotificationsFactory(
    private val intents: MainIntents,
) {

    fun createBalancesAreHiddenToast(): Toast {
        return Toast(
            message = resourceReference(R.string.toast_balances_hidden),
            action = ActionConfig(
                text = resourceReference(R.string.toast_undo),
                onClick = intents::onHiddenBalanceToastAction,
            ),
        )
    }

    fun createBalancesAreShownToast(): Toast {
        return Toast(
            message = resourceReference(R.string.toast_balances_shown),
            action = ActionConfig(
                text = resourceReference(R.string.toast_undo),
                onClick = intents::onShownBalanceToastAction,
            ),
        )
    }

    fun createBalancesAreHiddenModalNotification(): ModalNotification {
        return ModalNotification(
            iconResId = R.drawable.ic_eye_off_outline_24,
            title = resourceReference(R.string.balance_hidden_title),
            message = resourceReference(R.string.balance_hidden_description),
            primaryAction = ActionConfig(
                text = resourceReference(R.string.balance_hidden_got_it_button),
                onClick = { intents.onHiddenBalanceNotificationAction(isPermanent = false) },
            ),
            secondaryAction = ActionConfig(
                text = resourceReference(R.string.balance_hidden_do_not_show_button),
                onClick = { intents.onHiddenBalanceNotificationAction(isPermanent = true) },
            ),
        )
    }
}