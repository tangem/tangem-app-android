package com.tangem.core.ui.message

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheet
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetV2
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalEventMessageHandler
import com.tangem.core.ui.res.LocalSnackbarHostState

@Composable
fun EventMessageEffect(
    messageHandler: EventMessageHandler = LocalEventMessageHandler.current,
    snackbarHostState: SnackbarHostState = LocalSnackbarHostState.current,
    onShowSnackbar: suspend (SnackbarMessage, Context) -> Unit = { message, context ->
        showSnackbar(snackbarHostState, message, context)
    },
) {
    val messageEvent by messageHandler.collectAsState()
    val context = LocalContext.current

    var dialogMessage: DialogMessage? by remember { mutableStateOf(value = null) }
    var bottomSheetMessage: BottomSheetMessage? by remember { mutableStateOf(value = null) }
    var bottomSheetMessageV2: BottomSheetMessageV2? by remember { mutableStateOf(value = null) }

    EventEffect(event = messageEvent) { message ->
        when (message) {
            is SnackbarMessage -> {
                onShowSnackbar(message, context)
            }
            is DialogMessage -> {
                dialogMessage = message
            }
            is BottomSheetMessage -> {
                bottomSheetMessage = message
            }
            is BottomSheetMessageV2 -> {
                bottomSheetMessageV2 = message
            }
        }
    }

    dialogMessage?.let { message ->
        MessageDialog(
            message = message,
            onDismissRequest = {
                dialogMessage = null
                message.onDismissRequest()
            },
        )
    }

    bottomSheetMessage?.let { message ->
        MessageBottomSheet(
            message = message,
            onDismissRequest = {
                bottomSheetMessage = null
                message.onDismissRequest()
            },
        )
    }

    bottomSheetMessageV2?.let { message ->
        MessageBottomSheetV2(
            state = message.messageBottomSheetUMV2,
            onDismissRequest = { bottomSheetMessageV2 = null },
        )
    }
}

@Composable
private fun MessageBottomSheet(message: BottomSheetMessage, onDismissRequest: () -> Unit) {
    val config = TangemBottomSheetConfig(
        isShown = true,
        content = MessageBottomSheetUM(
            iconResId = message.iconResId,
            title = message.title,
            message = message.message,
            primaryAction = message.firstAction?.let { action ->
                MessageBottomSheetUM.ActionUM(
                    text = action.title,
                    enabled = action.enabled,
                    onClick = {
                        action.onClick()
                        onDismissRequest()
                    },
                )
            },
            secondaryAction = message.secondAction?.let { action ->
                MessageBottomSheetUM.ActionUM(
                    text = action.title,
                    enabled = action.enabled,
                    onClick = {
                        action.onClick()
                        onDismissRequest()
                    },
                )
            },
        ),
        onDismissRequest = onDismissRequest,
    )

    MessageBottomSheet(config)
}

@Composable
private fun MessageDialog(message: DialogMessage, onDismissRequest: () -> Unit) {
    BasicDialog(
        message = message.message.resolveReference(),
        title = message.title?.resolveReference(),
        confirmButton = message.firstAction.let { action ->
            DialogButtonUM(
                title = action.title.resolveReference(),
                warning = action.warning,
                enabled = action.enabled,
                onClick = {
                    action.onClick()

                    if (message.dismissOnFirstAction) {
                        onDismissRequest()
                    }
                },
            )
        },
        dismissButton = message.secondAction?.let { action ->
            DialogButtonUM(
                title = action.title.resolveReference(),
                warning = action.warning,
                enabled = action.enabled,
                onClick = {
                    action.onClick()
                    onDismissRequest()
                },
            )
        },
        onDismissDialog = onDismissRequest,
        isDismissable = message.isDismissable,
    )
}

private suspend fun showSnackbar(snackbarHostState: SnackbarHostState, message: SnackbarMessage, context: Context) {
    val result = snackbarHostState.showSnackbar(
        message = message.message.resolveReference(context.resources),
        actionLabel = message.actionLabel?.resolveReference(context.resources),
        duration = when (message.duration) {
            SnackbarMessage.Duration.Short -> SnackbarDuration.Short
            SnackbarMessage.Duration.Long -> SnackbarDuration.Long
            SnackbarMessage.Duration.Indefinite -> SnackbarDuration.Indefinite
        },
    )

    when (result) {
        SnackbarResult.Dismissed -> message.onDismissRequest()
        SnackbarResult.ActionPerformed -> message.action?.invoke()
    }
}