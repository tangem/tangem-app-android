package com.tangem.core.ui.message

import android.content.Context
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.extensions.resolveReference

@Composable
fun EventMessageEffect(messageHandler: EventMessageHandler, snackbarHostState: SnackbarHostState) {
    val messageEvent by messageHandler.collectAsState()
    val context = LocalContext.current
    var contentMessage: ContentMessage? by remember { mutableStateOf(value = null) }

    EventEffect(event = messageEvent) { message ->
        when (message) {
            is ContentMessage -> {
                contentMessage = message
            }
            is SnackbarMessage -> {
                showSnackbar(snackbarHostState, message, context)
            }
        }
    }

    contentMessage?.content?.invoke(
        onDismiss = { contentMessage = null },
    )
}

private suspend fun showSnackbar(snackbarHostState: SnackbarHostState, message: SnackbarMessage, context: Context) {
    val result = snackbarHostState.showSnackbar(
        message = message.message.resolveReference(context.resources),
        actionLabel = message.actionLabel?.resolveReference(context.resources),
        duration = message.duration,
    )

    if (result == SnackbarResult.ActionPerformed) {
        message.action?.invoke()
    }
}