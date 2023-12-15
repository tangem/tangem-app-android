package com.tangem.managetokens.presentation.common.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.common.state.AlertState

@Composable
internal fun Alert(state: AlertState, onDismiss: () -> Unit) {
    when (state) {
        is AlertState.DefaultAlert,
        is AlertState.NonNative,
        AlertState.TokensUnsupportedCurve,
        is AlertState.TokensUnsupported,
        is AlertState.TokensUnsupportedBlockchainByCard,
        -> DefaultAlert(state, onDismiss)
        is AlertState.TokenUnavailable -> TokenUnavailableAlert(state, onDismiss)
    }
}

@Composable
private fun DefaultAlert(state: AlertState, onDismiss: () -> Unit) {
    BasicDialog(
        message = state.message.resolveReference(),
        confirmButton = DialogButton(
            title = stringResource(id = R.string.common_ok),
            onClick = onDismiss,
        ),
        onDismissDialog = onDismiss,
    )
}

@Composable
private fun TokenUnavailableAlert(state: AlertState.TokenUnavailable, onDismiss: () -> Unit) {
    BasicDialog(
        message = state.message.resolveReference(),
        confirmButton = DialogButton(
            title = state.confirmButtonText.resolveReference(),
            onClick = onDismiss,
        ),
        dismissButton = DialogButton(
            title = state.dismissButtonText.resolveReference(),
            onClick = { state.onUpvoteClick() },
        ),
        onDismissDialog = onDismiss,
    )
}