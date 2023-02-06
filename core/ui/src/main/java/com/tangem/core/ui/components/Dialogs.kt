package com.tangem.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

/**
 * Dialog button params
 *
 * @param title Button text. If not provided default values will be used
 * @param warning If true then button text will be in theme warning color
 * @param enabled If false button will be disabled
 * @param onClick Button click callback
 */
data class DialogButton(
    val title: String? = null,
    val warning: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Additional params for dialog text field
 */
data class AdditionalTextInputDialogParams(
    val label: String? = null,
    val placeholder: String? = null,
    val caption: String? = null,
    val enabled: Boolean = true,
    val isError: Boolean = false,
)

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
    TangemDialog(
        type = DialogType.Message(message),
        confirmButton = confirmButton,
        onDismissDialog = onDismissDialog,
        title = title,
        dismissButton = dismissButton,
    )
}

@Composable
fun SimpleOkDialog(message: String, onDismissDialog: () -> Unit) {
    TangemDialog(
        type = DialogType.Message(message),
        confirmButton = DialogButton(onClick = onDismissDialog),
        onDismissDialog = onDismissDialog,
        title = null,
        dismissButton = null,
    )
}

/**
 * Dialog with text field
 *
 * @param fieldValue Text field value
 * @param onValueChange Text field value callback
 * @param title title to show (no title if null)
 * @param confirmButton title and action for confirm button
 * @param dismissButton title and action for dismiss button (no dismiss button if null)
 * @param onDismissDialog action to perform when dialog is closed
 * @param textFieldParams Additional params for dialog text field
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=268%3A273&t=9uHKoudX78ySqium-4"
 * >Figma component</a>
 */
@Composable
fun TextInputDialog(
    fieldValue: TextFieldValue,
    confirmButton: DialogButton,
    onDismissDialog: () -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    title: String? = null,
    dismissButton: DialogButton? = null,
    textFieldParams: AdditionalTextInputDialogParams,
) {
    TangemDialog(
        type = DialogType.TextInput(
            value = fieldValue,
            onValueChange = onValueChange,
            params = textFieldParams,
        ),
        confirmButton = confirmButton,
        onDismissDialog = onDismissDialog,
        title = title,
        dismissButton = dismissButton,
    )
}

// region Defaults
@Composable
private fun TangemDialog(
    type: DialogType,
    confirmButton: DialogButton,
    onDismissDialog: () -> Unit,
    title: String? = null,
    dismissButton: DialogButton? = null,
) {
    Dialog(onDismissRequest = onDismissDialog) {
        Column(
            modifier = Modifier
                .background(
                    shape = TangemTheme.shapes.roundedCornersLarge,
                    color = TangemTheme.colors.background.plain,
                )
                .padding(all = TangemTheme.dimens.spacing24),
        ) {
            if (title != null) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = when (type) {
                        is DialogType.Message -> TangemTheme.typography.h2
                        is DialogType.TextInput -> TangemTheme.typography.h3
                    },
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerH16()
            }
            DialogContent(type = type)
            SpacerH24()
            DialogButtons(confirmButton = confirmButton, dismissButton = dismissButton)
        }
    }
}

@Composable
private fun DialogContent(
    modifier: Modifier = Modifier,
    type: DialogType,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (type) {
            is DialogType.Message -> {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = type.message,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
            }
            is DialogType.TextInput -> {
                OutlineTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = type.value,
                    label = type.params.label,
                    placeholder = type.params.placeholder,
                    caption = type.params.caption,
                    enabled = type.params.enabled,
                    isError = type.params.isError,
                    onValueChange = { newValue ->
                        type.onValueChange(newValue)
                    },
                )
            }
        }
    }
}

@Composable
private fun DialogButtons(
    modifier: Modifier = Modifier,
    confirmButton: DialogButton,
    dismissButton: DialogButton?,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = TangemTheme.dimens.spacing4,
            alignment = Alignment.End,
        ),
    ) {
        if (dismissButton != null) {
            DialogButton(
                text = dismissButton.title ?: stringResource(id = R.string.common_cancel),
                warning = dismissButton.warning,
                enabled = dismissButton.enabled,
                onClick = dismissButton.onClick,
            )
        }
        DialogButton(
            text = confirmButton.title ?: stringResource(id = R.string.common_ok),
            warning = confirmButton.warning,
            enabled = confirmButton.enabled,
            onClick = confirmButton.onClick,
        )
    }
}

@Composable
private fun DialogButton(
    modifier: Modifier = Modifier,
    text: String,
    warning: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = modifier) {
        if (warning) {
            WarningTextButton(
                text = text,
                enabled = enabled,
                onClick = onClick,
            )
        } else {
            TextButton(
                text = text,
                enabled = enabled,
                onClick = onClick,
            )
        }
    }
}

private sealed interface DialogType {
    data class Message(val message: String) : DialogType

    data class TextInput(
        val value: TextFieldValue,
        val onValueChange: (TextFieldValue) -> Unit,
        val params: AdditionalTextInputDialogParams = AdditionalTextInputDialogParams(),
    ) : DialogType
}
// endregion Defaults

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

@Composable
private fun WarningBasicDialogSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        BasicDialog(
            message = "All protected passwords will be deleted from the secure storage, you must enter the wallet " +
                "password to work with the app",
            title = "Attention",
            confirmButton = DialogButton(warning = true) {},
            dismissButton = DialogButton {},
            onDismissDialog = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WarningBasicDialogPreview_Light() {
    TangemTheme {
        WarningBasicDialogSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WarningBasicDialogPreview_Dark() {
    TangemTheme(isDark = true) {
        WarningBasicDialogSample()
    }
}

@Composable
private fun TextInputDialogSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        TextInputDialog(
            fieldValue = TextFieldValue(text = ""),
            title = "Rename Wallet",
            confirmButton = DialogButton {},
            onDismissDialog = {},
            onValueChange = {},
            textFieldParams = AdditionalTextInputDialogParams(
                label = "Wallet name",
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TextInputDialogPreview_Light() {
    TangemTheme {
        TextInputDialogSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TextInputDialogPreview_Dark() {
    TangemTheme(isDark = true) {
        TextInputDialogSample()
    }
}
// endregion Preview
