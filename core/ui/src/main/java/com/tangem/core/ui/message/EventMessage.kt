package com.tangem.core.ui.message

import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.ui.extensions.TextReference

/**
 * Event that is used to show a message in the UI.
 *
 * @see EventMessageHandler
 * */
@Immutable
sealed interface EventMessage : UiMessage

/**
 * Shows a snackbar.
 *
 * @param message The message to show.
 * @param duration The duration of the snackbar.
 * @param actionLabel The label of the action button.
 * @param action The action to perform when the action button is clicked.
 * */
data class SnackbarMessage(
    val message: TextReference,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabel: TextReference? = null,
    val action: (() -> Unit)? = null,
) : EventMessage

/**
 * Shows a [content] in the UI.
 *
 * @param content The content to show.
 * */
data class ContentMessage(val content: Content) : EventMessage {

    @Stable
    fun interface Content {

        @Suppress("ComposableFunctionName", "TopLevelComposableFunctions")
        @Composable
        operator fun invoke(onDismiss: () -> Unit)
    }
}