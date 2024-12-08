package com.tangem.features.send.impl.presentation.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.tangem.common.ui.alerts.models.AlertUM
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendEvent

@Composable
internal fun SendEventEffect(event: StateEvent<SendEvent>, snackbarHostState: SnackbarHostState) {
    val resources = LocalContext.current.resources
    var alertConfig by remember { mutableStateOf<AlertUM?>(value = null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(key1 = alertConfig) {
        keyboardController?.hide()
    }

    alertConfig?.let {
        SendAlert(state = it, onDismiss = { alertConfig = null })
    }

    EventEffect(
        event = event,
        onTrigger = { value ->
            when (value) {
                is SendEvent.ShowSnackBar -> {
                    snackbarHostState.showSnackbar(message = value.text.resolveReference(resources))
                }
                is SendEvent.ShowAlert -> {
                    alertConfig = value.alert
                }
            }
        },
    )
}

@Composable
internal fun SendAlert(state: AlertUM, onDismiss: () -> Unit) {
    val confirmButton: DialogButtonUM
    val dismissButton: DialogButtonUM?

    val onActionClick = state.onConfirmClick
    if (onActionClick != null) {
        confirmButton = DialogButtonUM(
            title = state.confirmButtonText.resolveReference(),
            onClick = {
                onActionClick()
                onDismiss()
            },
        )
        dismissButton = DialogButtonUM(
            title = stringResourceSafe(id = R.string.common_cancel),
            onClick = onDismiss,
        )
    } else {
        confirmButton = DialogButtonUM(
            title = state.confirmButtonText.resolveReference(),
            onClick = onDismiss,
        )
        dismissButton = null
    }

    BasicDialog(
        message = state.message.resolveReference(),
        confirmButton = confirmButton,
        onDismissDialog = onDismiss,
        title = state.title?.resolveReference(),
        dismissButton = dismissButton,
    )
}