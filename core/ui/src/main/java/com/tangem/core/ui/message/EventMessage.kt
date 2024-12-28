package com.tangem.core.ui.message

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage.Duration

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
 * @param duration The duration of the snackbar. Optional, [Duration.Short] by default.
 * @param onDismissRequest The action to perform when the snackbar is dismissed. Optional, empty lambda by default.
 * @param actionLabel The label of the action button. Optional, `null` by default.
 * @param action The action to perform when the action button is clicked. Optional, `null` by default.
 * */
data class SnackbarMessage(
    val message: TextReference,
    val duration: Duration = Duration.Short,
    val onDismissRequest: () -> Unit = {},
    val actionLabel: TextReference? = null,
    val action: (() -> Unit)? = null,
) : EventMessage {

    /**
     * The duration of the snackbar.
     * */
    enum class Duration {

        /**
         * Shows the snackbar for a short period of time.
         * */
        Short,

        /**
         * Shows the snackbar for a long period of time.
         * */
        Long,

        /**
         * Shows the snackbar indefinitely, until dismissed or action performed.
         * */
        Indefinite,
    }
}

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

/**
 * Shows an alert dialog.
 *
 * @param message The message to show.
 * @param title The title of the dialog. Optional, `null` by default.
 * @param firstAction The first dialog action to perform.
 * @param secondAction The second dialog action to perform. Optional, `null` by default.
 * Performing the second action always dismisses the dialog.
 * @param isDismissable If `false` then dialog can not be dismissed by back button click or by outside click.
 * @param dismissOnFirstAction If `true` then dialog is dismissed when first action is performed,
 * ignoring lambda passed to [EventMessageAction.onClick].
 * `true` by default.
 * @param onDismissRequest The action to perform when the dialog is dismissed.
 * */
data class DialogMessage(
    val message: TextReference,
    val title: TextReference? = null,
    val firstAction: EventMessageAction,
    val secondAction: EventMessageAction? = null,
    val isDismissable: Boolean = true,
    val dismissOnFirstAction: Boolean = true,
    val onDismissRequest: () -> Unit = {},
) : EventMessage {

    companion object {

        /**
         * Builder for [DialogMessage].
         *
         * @param message The message to show.
         * @param title The title of the dialog. Optional, `null` by default.
         * @param isDismissable If `false` then dialog can not be dismissed by back button click or by outside click.
         * `true` by default.
         * @param dismissOnFirstAction If `true` then dialog is dismissed when first action is performed,
         * ignoring lambda passed to [EventMessageAction.onClick].
         * `true` by default.
         * @param onDismissRequest The action to perform when the dialog is dismissed.
         * @param firstActionBuilder The builder for the first action.
         * By default, it builds an action with title "OK".
         * @param secondActionBuilder The builder for the second action. Optional, `null` by default.
         * Performing the second action always dismisses the dialog.
         *
         * @return [DialogMessage] with the specified parameters.
         * */
        operator fun invoke(
            message: TextReference,
            title: TextReference? = null,
            isDismissable: Boolean = true,
            dismissOnFirstAction: Boolean = true,
            onDismissRequest: () -> Unit = {},
            firstActionBuilder: EventMessageAction.BuilderScope.() -> EventMessageAction = { okAction() },
            secondActionBuilder: (EventMessageAction.BuilderScope.() -> EventMessageAction)? = null,
        ): DialogMessage {
            val buttonsScope = EventMessageAction.BuilderScope(onDismissRequest)

            return DialogMessage(
                message = message,
                title = title,
                isDismissable = isDismissable,
                dismissOnFirstAction = dismissOnFirstAction,
                onDismissRequest = onDismissRequest,
                firstAction = firstActionBuilder(buttonsScope),
                secondAction = secondActionBuilder?.invoke(buttonsScope),
            )
        }
    }
}

/**
 * Shows a bottom sheet.
 *
 * @param iconResId The icon to show in the bottom sheet.
 * Optional, `null` by default.
 * @param title The title of the bottom sheet. Optional, `null` by default.
 * @param message The message to show in the bottom sheet.
 * @param firstAction The first action to perform. Optional, `null` by default.
 * @param secondAction The second action to perform. Optional, `null` by default.
 * @param onDismissRequest The action to perform when the bottom sheet is dismissed.
 * */
data class BottomSheetMessage(
    @DrawableRes val iconResId: Int? = null,
    val title: TextReference? = null,
    val message: TextReference,
    val firstAction: EventMessageAction? = null,
    val secondAction: EventMessageAction? = null,
    val onDismissRequest: () -> Unit = {},
) : EventMessage {

    companion object {

        /**
         * Builder for [BottomSheetMessage].
         *
         * @param iconResId The icon to show in the bottom sheet. Optional, `null` by default.
         * @param title The title of the bottom sheet. Optional, `null` by default.
         * @param message The message to show in the bottom sheet.
         * @param onDismissRequest The action to perform when the bottom sheet is dismissed.
         * @param firstActionBuilder The builder for the first action. Optional, `null` by default.
         * @param secondActionBuilder The builder for the second action. Optional, `null` by default.
         * */
        operator fun invoke(
            @DrawableRes iconResId: Int?,
            title: TextReference?,
            message: TextReference,
            onDismissRequest: () -> Unit = {},
            firstActionBuilder: (EventMessageAction.BuilderScope.() -> EventMessageAction)? = null,
            secondActionBuilder: (EventMessageAction.BuilderScope.() -> EventMessageAction)? = null,
        ): BottomSheetMessage {
            val buttonsScope = EventMessageAction.BuilderScope(onDismissRequest)

            return BottomSheetMessage(
                iconResId = iconResId,
                title = title,
                message = message,
                onDismissRequest = onDismissRequest,
                firstAction = firstActionBuilder?.invoke(buttonsScope),
                secondAction = secondActionBuilder?.invoke(buttonsScope),
            )
        }
    }
}

/**
 * Represents an action button in the dialog.
 *
 * @param title The title of the action.
 * @param warning If `true` then the action is highlighted as a warning.
 * @param enabled If `false` then the action is disabled.
 * @param onClick The action to perform when the button is clicked.
 * */
data class EventMessageAction(
    val title: TextReference,
    val warning: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
) {

    /**
     * Scope for building buttons for the message.
     *
     * @param onDismissRequest The action to perform when the dialog is dismissed.
     * */
    class BuilderScope internal constructor(val onDismissRequest: () -> Unit) {

        /**
         * Builds an action with title "OK".
         *
         * @param onClick The action to perform when the button is clicked. By default, it dismisses the message.
         * */
        fun okAction(onClick: () -> Unit = onDismissRequest) = EventMessageAction(
            title = resourceReference(id = R.string.common_ok),
            onClick = onClick,
        )

        /**
         * Builds an action with title "Cancel".
         *
         * @param onClick The action to perform when the button is clicked. By default, it dismisses the message.
         * */
        fun cancelAction(onClick: () -> Unit = onDismissRequest) = EventMessageAction(
            title = resourceReference(id = R.string.common_cancel),
            onClick = onClick,
        )
    }
}