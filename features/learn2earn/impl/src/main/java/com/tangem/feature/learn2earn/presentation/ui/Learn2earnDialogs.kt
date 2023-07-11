package com.tangem.feature.learn2earn.presentation.ui

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.TextButton
import com.tangem.feature.learn2earn.impl.R
import com.tangem.feature.learn2earn.presentation.ui.state.MainScreenState

/**
* [REDACTED_AUTHOR]
 */
@Composable
internal fun Learn2earnDialogs(dialog: MainScreenState.Dialog?) {
    when (dialog) {
        is MainScreenState.Dialog.Claimed -> ClaimedDialog(dialog)
        is MainScreenState.Dialog.PromoCodeNotRegistered -> PromoCodeNotRegisteredDialog(dialog)
        is MainScreenState.Dialog.Error -> ErrorDialog(dialog)
        else -> Unit
    }
}

@Composable
private fun ClaimedDialog(dialog: MainScreenState.Dialog.Claimed) {
    AlertDialog(
        title = {
            Text(text = stringResource(id = R.string.common_success))
        },
        text = {
            Text(text = stringResource(id = R.string.main_promotion_credited, dialog.networkFullName))
        },
        confirmButton = {
            TextButton(
                text = stringResource(id = R.string.common_ok),
                onClick = dialog.onOk,
            )
        },
        onDismissRequest = dialog.onDismissRequest,
    )
}

@Composable
private fun PromoCodeNotRegisteredDialog(dialog: MainScreenState.Dialog.PromoCodeNotRegistered) {
    AlertDialog(
        title = {
            Text(text = stringResource(id = R.string.common_error))
        },
        text = {
            Text(text = stringResource(id = R.string.main_promotion_no_purchase))
        },
        dismissButton = {
            TextButton(
                text = stringResource(id = R.string.common_cancel),
                onClick = dialog.onCancel,
            )
        },
        confirmButton = {
            TextButton(
                text = stringResource(id = R.string.common_buy),
                onClick = dialog.onOk,
            )
        },
        onDismissRequest = dialog.onDismissRequest,
    )
}

@Composable
private fun ErrorDialog(dialog: MainScreenState.Dialog.Error) {
    AlertDialog(
        title = {
            Text(text = stringResource(id = R.string.common_error))
        },
        text = {
            Text(text = dialog.error.description)
        },
        confirmButton = {
            TextButton(
                text = stringResource(id = R.string.common_ok),
                onClick = dialog.onOk,
            )
        },
        onDismissRequest = dialog.onDismissRequest,
    )
}
