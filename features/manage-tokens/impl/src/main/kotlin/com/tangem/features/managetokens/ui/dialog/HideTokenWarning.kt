package com.tangem.features.managetokens.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.features.managetokens.impl.R

@Composable
internal fun HideTokenWarning(currency: ManagedCryptoCurrency, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    BasicDialog(
        title = stringResource(
            R.string.token_details_hide_alert_title,
            currency.name,
        ),
        message = stringResource(R.string.token_details_hide_alert_message),
        confirmButton = DialogButtonUM(
            title = stringResource(R.string.token_details_hide_alert_hide),
            warning = true,
            onClick = onConfirm,
        ),
        dismissButton = DialogButtonUM(
            title = stringResource(R.string.common_cancel),
            onClick = onDismiss,
        ),
        onDismissDialog = onDismiss,
    )
}