package com.tangem.core.ui.components.snackbar

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider

internal data class TangemSnackbarModel(val snackbarData: SnackbarData, val actionOnNewLine: Boolean)

internal class TangemSnackbarModelProvider : CollectionPreviewParameterProvider<TangemSnackbarModel>(
    listOf(
        createTangemSnackbarModel(
            message = "Single-line description.",
            actionLabel = "Button",
            actionOnNewLine = false,
        ),
        createTangemSnackbarModel(
            message = "Very loooooooooooong single-line description.",
            actionLabel = "Button",
            actionOnNewLine = false,
        ),
        createTangemSnackbarModel(
            message = "Single-line description.",
            actionLabel = "Very looooooong button name",
            actionOnNewLine = false,
        ),
        createTangemSnackbarModel(
            message = "Single-line description.",
            actionLabel = "Button",
            actionOnNewLine = true,
        ),
        createTangemSnackbarModel(
            message = "Very loooooooooooong single-line description.",
            actionLabel = "Button",
            actionOnNewLine = true,
        ),
        createTangemSnackbarModel(
            message = "Single-line description.",
            actionLabel = "Very looooooong button name",
            actionOnNewLine = true,
        ),
    ),
) {

    companion object {

        fun createTangemSnackbarModel(
            message: String,
            actionLabel: String,
            actionOnNewLine: Boolean,
        ): TangemSnackbarModel {
            return TangemSnackbarModel(
                snackbarData = createSnackbarData(message, actionLabel),
                actionOnNewLine = actionOnNewLine,
            )
        }

        private fun createSnackbarData(message: String, actionLabel: String): SnackbarData {
            return object : SnackbarData {

                override val visuals: SnackbarVisuals
                    get() = object : SnackbarVisuals {
                        override val message: String = message
                        override val actionLabel: String = actionLabel
                        override val duration: SnackbarDuration = SnackbarDuration.Short // Never-mind
                        override val withDismissAction: Boolean = false // Never-mind
                    }

                override fun dismiss() = Unit
                override fun performAction() = Unit
            }
        }
    }
}