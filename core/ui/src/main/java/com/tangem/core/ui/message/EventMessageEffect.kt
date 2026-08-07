package com.tangem.core.ui.message

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheet
import com.tangem.core.ui.components.snackbar.TangemTopSnackbarHostState
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.*

@Composable
fun EventMessageEffect(
    messageHandler: EventMessageHandler = LocalEventMessageHandler.current,
    topSnackbarHostState: TangemTopSnackbarHostState = LocalTopSnackbarHostState.current,
    onShowTopSnackbar: suspend (SnackbarMessage) -> Unit = { message ->
        topSnackbarHostState.showSnackbar(message)
    },
    onShowToast: (ToastMessage, Context) -> Unit = { message, context -> showToast(message, context) },
) {
    val messageEvent by messageHandler.collectAsState()
    val context = LocalContext.current

    var dialogMessage: DialogMessage? by remember { mutableStateOf(value = null) }
    var bottomSheetMessage: BottomSheetMessage? by remember { mutableStateOf(value = null) }
    var loadingMessage: GlobalLoadingMessage? by remember { mutableStateOf(value = null) }

    EventEffect(event = messageEvent) { message ->
        when (message) {
            is SnackbarMessage -> {
                onShowTopSnackbar(message)
            }
            is DialogMessage -> {
                dialogMessage = message
            }
            is BottomSheetMessage -> {
                bottomSheetMessage = message
            }
            is ToastMessage -> {
                onShowToast(message, context)
            }
            is GlobalLoadingMessage -> {
                loadingMessage = if (message.isShow) {
                    message
                } else {
                    null
                }
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
            state = message.messageBottomSheetUM,
            onDismissRequest = { bottomSheetMessage = null },
        )
    }

    if (loadingMessage != null) {
        LoadingDialog()
    }
}

@Composable
private fun LoadingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SpacerHMax()
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = TangemTheme.colors.icon.primary1,
            )
            SpacerHMax()
        }
    }
}

@Composable
private fun MessageDialog(message: DialogMessage, onDismissRequest: () -> Unit) {
    BasicDialog(
        message = message.message.resolveReference(),
        title = message.title?.resolveReference(),
        confirmButton = message.firstAction.let { action ->
            DialogButtonUM(
                title = action.title.resolveReference(),
                isWarning = action.isWarning,
                isEnabled = action.isEnabled,
                onClick = {
                    action.onClick()

                    if (message.shouldDismissOnFirstAction) {
                        onDismissRequest()
                    }
                },
            )
        },
        dismissButton = message.secondAction?.let { action ->
            DialogButtonUM(
                title = action.title.resolveReference(),
                isWarning = action.isWarning,
                isEnabled = action.isEnabled,
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

private fun showToast(message: ToastMessage, context: Context) {
    Toast.makeText(
        /* context = */ context,
        /* text = */ message.message.resolveReference(context.resources),
        /* duration = */
        when (message.duration) {
            ToastMessage.Duration.Short -> Toast.LENGTH_SHORT
            ToastMessage.Duration.Long -> Toast.LENGTH_LONG
        },
    ).show()
}