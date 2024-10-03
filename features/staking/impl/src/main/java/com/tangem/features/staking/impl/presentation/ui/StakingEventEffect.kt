package com.tangem.features.staking.impl.presentation.ui

import androidx.compose.material3.SnackbarHostState
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
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.events.StakingEvent

@Composable
internal fun StakingEventEffect(event: StateEvent<StakingEvent>, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val resources = LocalContext.current.resources
    var alertConfig by remember { mutableStateOf<AlertUM?>(value = null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(key1 = alertConfig) {
        keyboardController?.hide()
    }

    alertConfig?.let {
        StakingAlert(state = it, onDismiss = { alertConfig = null })
    }

    EventEffect(
        event = event,
        onTrigger = { value ->
            when (value) {
                is StakingEvent.ShowSnackBar -> {
                    snackbarHostState.showSnackbar(message = value.text.resolveReference(resources))
                }
                is StakingEvent.ShowAlert -> {
                    alertConfig = value.alert
                }
                is StakingEvent.ShowShareDialog -> {
                    context.shareText(value.txUrl)
                }
            }
        },
    )
}

@Composable
internal fun StakingAlert(state: AlertUM, onDismiss: () -> Unit) {
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
