package com.tangem.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SelctorDialogParamsProvider.SelectorDialogParams
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.components.fields.SimpleDialogTextField
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Simple alert dialog with a message and 'OK' button
 *
 * @param message message to show
 * @param title title to show (no title if null)
 * @param confirmButton title and action for confirm button
 * @param dismissButton title and action for dismiss button (no dismiss button if null)
 * @param onDismissDialog action to perform when dialog is closed
 * @param isDismissable If false then dialog can not be dismissed by back button click or by outside click
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=48%3A30&t=izDPAJnDbJTTC0Fp-1"
 * >Figma component</a>
 */
@Composable
fun BasicDialog(
    message: String,
    confirmButton: DialogButtonUM,
    onDismissDialog: () -> Unit,
    title: String? = null,
    dismissButton: DialogButtonUM? = null,
    isDismissable: Boolean = true,
) {
    TangemDialog(
        type = DialogType.Message(message),
        confirmButton = confirmButton,
        onDismissDialog = onDismissDialog,
        title = title,
        dismissButton = dismissButton,
        properties = DialogProperties(
            dismissOnBackPress = isDismissable,
            dismissOnClickOutside = isDismissable,
        ),
    )
}

@Composable
fun SimpleOkDialog(message: String, onDismissDialog: () -> Unit) {
    TangemDialog(
        type = DialogType.Message(message),
        confirmButton = DialogButtonUM(onClick = onDismissDialog),
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
 * @param isDismissable If false then dialog can not be dismissed by back button click or by outside click
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=268%3A273&t=9uHKoudX78ySqium-4"
 * >Figma component</a>
 */
@Composable
fun TextInputDialog(
    fieldValue: TextFieldValue,
    confirmButton: DialogButtonUM,
    onDismissDialog: () -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    textFieldParams: AdditionalTextInputDialogUM = remember { AdditionalTextInputDialogUM() },
    title: String? = null,
    dismissButton: DialogButtonUM? = null,
    isDismissable: Boolean = true,
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
        properties = DialogProperties(
            dismissOnBackPress = isDismissable,
            dismissOnClickOutside = isDismissable,
        ),
    )
}

@Composable
fun TextInputDialog(
    fieldValue: String,
    confirmButton: DialogButtonUM,
    onDismissDialog: () -> Unit,
    onValueChange: (String) -> Unit,
    textFieldParams: AdditionalTextInputDialogUM = remember { AdditionalTextInputDialogUM() },
    title: String? = null,
    dismissButton: DialogButtonUM? = null,
    isDismissable: Boolean = true,
) {
    TangemDialog(
        type = DialogType.SimpleTextInput(
            value = fieldValue,
            onValueChange = onValueChange,
            params = textFieldParams,
        ),
        confirmButton = confirmButton,
        onDismissDialog = onDismissDialog,
        title = title,
        dismissButton = dismissButton,
        properties = DialogProperties(
            dismissOnBackPress = isDismissable,
            dismissOnClickOutside = isDismissable,
        ),
    )
}

@Composable
fun SelectorDialog(
    selectedItemIndex: Int,
    items: ImmutableList<String>,
    confirmButton: DialogButtonUM,
    onSelect: (index: Int) -> Unit,
    onDismissDialog: () -> Unit,
    title: String? = null,
    isDismissable: Boolean = true,
) {
    TangemDialog(
        type = DialogType.Selector(selectedItemIndex, items, onSelect),
        confirmButton = confirmButton,
        title = title,
        onDismissDialog = onDismissDialog,
        properties = DialogProperties(
            dismissOnBackPress = isDismissable,
            dismissOnClickOutside = isDismissable,
        ),
    )
}

/**
 * Dialog button params
 *
 * @param title Button text. If not provided default values will be used
 * @param warning If true then button text will be in theme warning color
 * @param enabled If false button will be disabled
 * @param onClick Button click callback
 */
data class DialogButtonUM(
    val title: String? = null,
    val warning: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Additional params for dialog text field
 */
data class AdditionalTextInputDialogUM(
    val label: String? = null,
    val placeholder: String? = null,
    val caption: String? = null,
    val enabled: Boolean = true,
    val isError: Boolean = false,
    val errorText: String? = null,
)

// region Defaults
@Composable
private fun TangemDialog(
    type: DialogType,
    confirmButton: DialogButtonUM,
    onDismissDialog: () -> Unit,
    title: String? = null,
    dismissButton: DialogButtonUM? = null,
    properties: DialogProperties = DialogProperties(),
) {
    Dialog(properties = properties, onDismissRequest = onDismissDialog) {
        Column(
            modifier = Modifier
                .background(
                    shape = TangemTheme.shapes.roundedCornersLarge,
                    color = TangemTheme.colors.background.primary,
                )
                .padding(vertical = TangemTheme.dimens.spacing24),
        ) {
            if (title != null) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = TangemTheme.dimens.spacing24)
                        .fillMaxWidth(),
                    text = title,
                    style = when (type) {
                        is DialogType.Message -> TangemTheme.typography.h2
                        is DialogType.TextInput -> TangemTheme.typography.h3
                        is DialogType.SimpleTextInput -> TangemTheme.typography.h3
                        is DialogType.Selector -> TangemTheme.typography.h2
                    },
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerH16()
            }
            DialogContent(type = type)
            SpacerH24()
            DialogButtons(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing24)
                    .fillMaxWidth(),
                confirmButton = confirmButton,
                dismissButton = dismissButton,
            )
        }
    }
}

@Composable
private fun DialogContent(type: DialogType, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (type) {
            is DialogType.Message -> {
                Text(
                    modifier = Modifier
                        .padding(horizontal = TangemTheme.dimens.spacing24)
                        .fillMaxWidth(),
                    text = type.message,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
            }
            is DialogType.SimpleTextInput -> {
                SimpleDialogTextField(
                    value = type.value,
                    onValueChange = type.onValueChange,
                    isError = type.params.isError,
                    errorText = type.params.errorText,
                    isEnabled = type.params.enabled,
                    placeholder = type.params.placeholder,
                )
            }
            is DialogType.TextInput -> {
                OutlineTextField(
                    modifier = Modifier
                        .padding(horizontal = TangemTheme.dimens.spacing24)
                        .fillMaxWidth(),
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
            is DialogType.Selector -> {
                SelectorDialogContent(
                    modifier = Modifier.fillMaxWidth(),
                    selectedItemIndex = type.selectedItemIndex,
                    items = type.items,
                    onSelect = type.onSelect,
                )
            }
        }
    }
}

@Composable
private fun DialogButtons(
    confirmButton: DialogButtonUM,
    dismissButton: DialogButtonUM?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = TangemTheme.dimens.spacing4,
            alignment = Alignment.End,
        ),
    ) {
        if (dismissButton != null) {
            DialogButton(
                text = dismissButton.title ?: stringResourceSafe(id = R.string.common_cancel),
                warning = dismissButton.warning,
                enabled = dismissButton.enabled,
                onClick = dismissButton.onClick,
            )
        }
        DialogButton(
            text = confirmButton.title ?: stringResourceSafe(id = R.string.common_ok),
            warning = confirmButton.warning,
            enabled = confirmButton.enabled,
            onClick = confirmButton.onClick,
        )
    }
}

@Composable
private fun DialogButton(
    text: String,
    warning: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
                colors = TangemButtonsDefaults.positiveButtonColors,
            )
        }
    }
}

@Composable
private fun SelectorDialogContent(
    selectedItemIndex: Int,
    items: ImmutableList<String>,
    onSelect: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(items = items) { index, itemText ->
            val onClick = remember(index) {
                { onSelect(index) }
            }

            val interactionSource = remember { MutableInteractionSource() }

            Row(
                modifier = Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                    )
                    .padding(
                        vertical = TangemTheme.dimens.spacing16,
                        horizontal = TangemTheme.dimens.spacing18,
                    )
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    modifier = Modifier.size(TangemTheme.dimens.size24),
                    selected = index == selectedItemIndex,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = TangemTheme.colors.control.checked,
                        unselectedColor = TangemTheme.colors.icon.secondary,
                    ),
                    interactionSource = interactionSource,
                )
                Text(
                    text = itemText,
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        }
    }
}

@Immutable
private sealed class DialogType {

    data class Message(val message: String) : DialogType()

    data class TextInput(
        val value: TextFieldValue,
        val onValueChange: (TextFieldValue) -> Unit,
        val params: AdditionalTextInputDialogUM = AdditionalTextInputDialogUM(),
    ) : DialogType()

    data class SimpleTextInput(
        val value: String,
        val onValueChange: (String) -> Unit,
        val params: AdditionalTextInputDialogUM = AdditionalTextInputDialogUM(),
    ) : DialogType()

    data class Selector(
        val selectedItemIndex: Int,
        val items: ImmutableList<String>,
        val onSelect: (index: Int) -> Unit,
    ) : DialogType()
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
        confirmButton = DialogButtonUM {},
        dismissButton = DialogButtonUM {},
        onDismissDialog = {},
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_SimpleOkDialog() {
    TangemThemePreview {
        SimpleOkDialogPreview()
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_BasicDialog() {
    TangemThemePreview {
        BasicDialogPreview()
    }
}

@Composable
private fun WarningBasicDialogSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        BasicDialog(
            message = "All protected passwords will be deleted from the secure storage, you must enter the wallet " +
                "password to work with the app",
            title = "Attention",
            confirmButton = DialogButtonUM(warning = true) {},
            dismissButton = DialogButtonUM {},
            onDismissDialog = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WarningBasicDialogPreview() {
    TangemThemePreview {
        WarningBasicDialogSample()
    }
}

@Composable
private fun TextInputDialogSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        TextInputDialog(
            fieldValue = TextFieldValue(text = ""),
            title = "Rename Wallet",
            confirmButton = DialogButtonUM {},
            onDismissDialog = {},
            onValueChange = {},
            textFieldParams = AdditionalTextInputDialogUM(
                label = "Wallet name",
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TextInputDialogPreview() {
    TangemThemePreview {
        TextInputDialogSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectorDialogPreview(@PreviewParameter(SelctorDialogParamsProvider::class) param: SelectorDialogParams) {
    TangemThemePreview {
        SelectorDialog(
            title = param.title,
            items = param.items,
            selectedItemIndex = param.selectedItemIndex,
            confirmButton = DialogButtonUM(title = "Cancel", onClick = {}),
            onSelect = {},
            onDismissDialog = {},
        )
    }
}

private class SelctorDialogParamsProvider : CollectionPreviewParameterProvider<SelectorDialogParams>(
    collection = listOf(
        SelectorDialogParams(
            title = "Theme",
            selectedItemIndex = 0,
            persistentListOf("Light", "Dark", "Follow system"),
        ),
        SelectorDialogParams(
            title = null,
            selectedItemIndex = 2,
            persistentListOf("Light", "Dark", "Follow system"),
        ),
        SelectorDialogParams(
            title = "Count",
            selectedItemIndex = 8,
            List(size = 10) { it.toString() }.toImmutableList(),
        ),
    ),
) {

    data class SelectorDialogParams(
        val title: String?,
        val selectedItemIndex: Int,
        val items: ImmutableList<String>,
    )
}
// endregion Preview