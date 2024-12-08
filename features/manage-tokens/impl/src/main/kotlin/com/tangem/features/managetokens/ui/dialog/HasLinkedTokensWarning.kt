package com.tangem.features.managetokens.ui.dialog

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.features.managetokens.impl.R

@Composable
internal fun HasLinkedTokensWarning(currency: ManagedCryptoCurrency, network: Network, onDismiss: () -> Unit) {
    BasicDialog(
        title = stringResourceSafe(
            R.string.token_details_unable_hide_alert_title,
            currency.name,
        ),
        message = stringResourceSafe(
            R.string.token_details_unable_hide_alert_message,
            currency.name,
            currency.symbol,
            network.name,
        ),
        confirmButton = DialogButtonUM(
            title = stringResourceSafe(R.string.common_ok),
            onClick = onDismiss,
        ),
        onDismissDialog = onDismiss,
    )
}