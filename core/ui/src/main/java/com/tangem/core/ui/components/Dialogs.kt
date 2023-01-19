package com.tangem.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

/**
 * Simple alert dialog with a message and 'OK' button
 *
 * @param message message to show
 * @param title title to show (no title if null)
 * @param confirmButton title and action for confirm button
 * @param dismissButton title and action for dismiss button (no dismiss button if null)
 * @param onDismissDialog action to perform when dialog is closed
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=48%3A30&t=izDPAJnDbJTTC0Fp-1"
 * >Figma component</a>
 */
@Composable
fun BasicDialog(
    message: String,
    confirmButton: DialogButton,
    onDismissDialog: () -> Unit,
    title: String? = null,
    dismissButton: DialogButton? = null,
) {
    AlertDialog(
        text = {
            Text(
                text = message,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        },
        title = if (title != null) {
            {
                Text(
                    text = title,
                    style = TangemTheme.typography.h2,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        } else {
            null
        },
        onDismissRequest = onDismissDialog,
        confirmButton = {
            TextButton(
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                text = confirmButton.title ?: stringResource(id = R.string.common_ok),
                onClick = confirmButton.onClick,
            )
        },
        dismissButton = {
            if (dismissButton != null) {
                TextButton(
                    modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                    text = dismissButton.title ?: stringResource(id = R.string.common_cancel),
                    onClick = dismissButton.onClick,
                )
            }
        },
        shape = TangemTheme.shapes.roundedCornersLarge,
        modifier = Modifier.padding(TangemTheme.dimens.spacing24),
    )
}

data class DialogButton(
    val title: String? = null,
    val onClick: () -> Unit,
)

@Composable
fun SimpleOkDialog(message: String, onDismissDialog: () -> Unit) {
    BasicDialog(
        message = message,
        confirmButton = DialogButton(onClick = onDismissDialog),
        onDismissDialog = onDismissDialog,
    )
}

// region Preview

@Composable
private fun SimpleOkDialogPreview() {
    SimpleOkDialog(
        message = "All protected passwords will be deleted from the " +
            "secure storage, you must enter the wallet password to work with the app.",
    ) {}
}

@Composable
private fun BasicDialogPreview() {
    BasicDialog(
        message = "All protected passwords will be deleted from the secure storage, you must enter the wallet " +
            "password to work with the app",
        title = "Attention",
        confirmButton = DialogButton {},
        dismissButton = DialogButton {},
        onDismissDialog = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview_SimpleOkDialog_InLightTheme() {
    TangemTheme(isDark = false) {
        SimpleOkDialogPreview()
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_BasicDialog_InLightTheme() {
    TangemTheme(isDark = false) {
        BasicDialogPreview()
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_SimpleOkDialog_InDarkTheme() {
    TangemTheme(isDark = true) {
        SimpleOkDialogPreview()
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_BasicDialog_InDarkTheme() {
    TangemTheme(isDark = true) {
        BasicDialogPreview()
    }
}

// endregion Preview
