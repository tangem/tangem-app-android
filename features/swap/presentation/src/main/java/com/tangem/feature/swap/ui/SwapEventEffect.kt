package com.tangem.feature.swap.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.tangem.common.ui.alerts.models.AlertUM
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.shareText
import com.tangem.feature.swap.models.states.events.SwapEvent
import com.tangem.feature.swap.presentation.R

@Composable
internal fun SwapEventEffect(event: StateEvent<SwapEvent>) {
    val context = LocalContext.current
    var alertConfig by remember { mutableStateOf<AlertUM?>(value = null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(key1 = alertConfig) {
        keyboardController?.hide()
    }

    alertConfig?.let {
        SwapAlert(state = it, onDismiss = { alertConfig = null })
    }

    EventEffect(
        event = event,
        onTrigger = { value ->
            when (value) {
                is SwapEvent.ShowAlert -> {
                    alertConfig = value.alert
                }
                is SwapEvent.ShowShareDialog -> {
                    context.shareText(value.txUrl)
                }
            }
        },
    )
}

@Composable
internal fun SwapAlert(state: AlertUM, onDismiss: () -> Unit) {
    val confirmButton = DialogButtonUM(
        title = state.confirmButtonText.resolveReference(),
        onClick = {
            state.onConfirmClick()
            onDismiss()
        },
    )
    val dismissButton = DialogButtonUM(
        title = stringResource(id = R.string.common_cancel),
        onClick = onDismiss,
    )

    BasicDialog(
        message = state.message.resolveReference(),
        confirmButton = confirmButton,
        onDismissDialog = onDismiss,
        title = state.title?.resolveReference(),
        dismissButton = dismissButton,
    )
}