package com.tangem.managetokens.presentation.managetokens.state

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.managetokens.impl.R

data class DerivationNotificationState(
    val totalNeeded: Int,
    val totalWallets: Int,
    val walletsToDerive: Int,
    val onGenerateClick: () -> Unit,
) {
    val config = NotificationConfig(
        title = resourceReference(id = R.string.warning_missing_derivation_title),
        subtitle = pluralReference(
            id = R.plurals.warning_missing_derivation_message,
            count = totalNeeded,
            formatArgs = wrappedList(totalNeeded),
        ),
        iconResId = R.drawable.ic_alert_circle_24,
        buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
            text = resourceReference(id = R.string.common_generate_addresses),
            iconResId = R.drawable.ic_tangem_24,
            onClick = onGenerateClick,
            additionalText = pluralReference(
                id = R.plurals.manage_tokens_number_of_wallets_android,
                count = totalWallets,
                formatArgs = wrappedList(walletsToDerive, totalWallets),
            ),
        ),
    )
}