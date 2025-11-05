package com.tangem.features.send.v2.subcomponents.destination.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.components.inputrow.inner.CrossIcon
import com.tangem.core.ui.components.inputrow.inner.PasteButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.test.SendAddressScreenTestTags

@Suppress("LongMethod", "LongParameterList")
@Composable
internal fun TextFieldWithPaste(
    value: String,
    placeholder: TextReference,
    label: TextReference,
    onValueChange: (String) -> Unit,
    onPasteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    footer: TextReference? = null,
    labelStyle: TextStyle = TangemTheme.typography.body2,
    error: TextReference? = null,
    isError: Boolean = false,
    isReadOnly: Boolean = false,
    isValuePasted: Boolean = false,
) {
    val (title, color) = when {
        isError && error != null -> error to TangemTheme.colors.text.warning
        isReadOnly -> label to TangemTheme.colors.text.tertiary
        else -> label to TangemTheme.colors.text.secondary
    }
    val placeholderColor = if (isReadOnly) TangemTheme.colors.text.tertiary else TangemTheme.colors.text.disabled
    FooterContainer(modifier, footer) {
        Box(
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                )
                .padding(end = 12.dp),
        ) {
            Row {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                ) {
                    Text(
                        text = title.resolveReference(),
                        style = labelStyle,
                        color = color,
                        modifier = Modifier.testTag(SendAddressScreenTestTags.DESTINATION_TAG_TEXT_FIELD_TITLE),
                    )
                    SimpleTextField(
                        value = value,
                        placeholder = placeholder,
                        placeholderColor = placeholderColor,
                        onValueChange = onValueChange,
                        readOnly = isReadOnly,
                        isValuePasted = isValuePasted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag(SendAddressScreenTestTags.DESTINATION_TAG_TEXT_FIELD),
                    )
                }
                AnimatedVisibility(
                    visible = !isReadOnly,
                    label = "Animate read only status change",
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(CenterVertically),
                ) {
                    CrossIcon(
                        onClick = onPasteClick,
                        modifier = Modifier.testTag(SendAddressScreenTestTags.DESTINATION_TAG_CLEAR_TEXT_FIELD_BUTTON),
                    )
                }
            }
            AnimatedVisibility(
                visible = !isReadOnly,
                label = "Animate read only status change",
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(CenterEnd),
            ) {
                PasteButton(
                    isPasteButtonVisible = value.isBlank(),
                    onClick = onPasteClick,
                    backgroundColorEnabled = TangemTheme.colors.button.secondary,
                    textColor = TangemTheme.colors.text.primary1,
                    modifier = Modifier
                        .padding(start = TangemTheme.dimens.spacing8)
                        .testTag(SendAddressScreenTestTags.DESTINATION_TAG_PASTE_BUTTON),
                )
            }
        }
    }
}