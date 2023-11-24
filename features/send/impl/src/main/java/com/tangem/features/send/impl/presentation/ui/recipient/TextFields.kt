package com.tangem.features.send.impl.presentation.ui.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.ui.common.FooterContainer

@Composable
internal fun TextFieldWithPasteAndIcon(
    value: String,
    placeholder: TextReference,
    label: TextReference,
    onValueChange: (String) -> Unit,
    onPasteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    footer: String? = null,
    singleLine: Boolean = false,
    error: TextReference? = null,
    isError: Boolean = false,
) {
    val (title, color) = if (isError && error != null) {
        error to TangemTheme.colors.text.warning
    } else {
        label to TangemTheme.colors.text.secondary
    }
    FooterContainer(modifier, footer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                ),
        ) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.body2,
                color = color,
                modifier = Modifier
                    .padding(
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing12,
                        top = TangemTheme.dimens.spacing12,
                    ),
            )
            Row {
                IdentIcon(
                    address = value,
                    modifier = Modifier
                        .padding(
                            start = TangemTheme.dimens.spacing16,
                            top = TangemTheme.dimens.spacing8,
                            bottom = TangemTheme.dimens.spacing10,
                        )
                        .clip(RoundedCornerShape(TangemTheme.dimens.radius20))
                        .size(TangemTheme.dimens.size40)
                        .background(TangemTheme.colors.background.tertiary),
                )
                SimpleTextField(
                    value = value,
                    placeholder = placeholder,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    modifier = Modifier
                        .padding(
                            start = TangemTheme.dimens.spacing12,
                            top = TangemTheme.dimens.spacing8,
                            bottom = TangemTheme.dimens.spacing10,
                        )
                        .weight(1f)
                        .align(CenterVertically),
                )
                PasteButton(
                    isPasteButtonVisible = value.isBlank(),
                    onClick = onPasteClick,
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(
                            start = TangemTheme.dimens.spacing4,
                            end = TangemTheme.dimens.spacing16,
                        ),
                )
            }
        }
    }
}

@Composable
internal fun TextFieldWithPaste(
    value: String,
    placeholder: TextReference,
    label: TextReference,
    onValueChange: (String) -> Unit,
    onPasteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    footer: String? = null,
    error: TextReference? = null,
    isError: Boolean = false,
) {
    val (title, color) = if (isError && error != null) {
        error to TangemTheme.colors.text.warning
    } else {
        label to TangemTheme.colors.text.secondary
    }
    FooterContainer(modifier, footer) {
        Row(
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                ),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(TangemTheme.dimens.spacing12),
            ) {
                Text(
                    text = title.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = color,
                )
                SimpleTextField(
                    value = value,
                    placeholder = placeholder,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .padding(top = TangemTheme.dimens.spacing6),
                )
            }
            PasteButton(
                isPasteButtonVisible = value.isBlank(),
                onClick = onPasteClick,
                modifier = Modifier
                    .align(CenterVertically)
                    .padding(end = TangemTheme.dimens.spacing16),
            )
        }
    }
}

@Composable
internal fun TextFieldWithInfo(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    info: TextReference? = null,
    footer: String? = null,
    isSingleLine: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    FooterContainer(
        footer = footer,
        footerTopPadding = TangemTheme.dimens.spacing6,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                )
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    top = TangemTheme.dimens.spacing12,
                    bottom = TangemTheme.dimens.spacing14,
                ),
        ) {
            Text(
                text = label,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
            Row {
                SimpleTextField(
                    value = value,
                    onValueChange = onValueChange,
                    visualTransformation = visualTransformation,
                    singleLine = isSingleLine,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier
                        .padding(top = TangemTheme.dimens.spacing6)
                        .weight(1f),
                )
                info?.let {
                    Text(
                        text = it.resolveReference(),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.tertiary,
                        modifier = Modifier
                            .padding(start = TangemTheme.dimens.spacing8)
                            .align(Alignment.Bottom),
                    )
                }
            }
        }
    }
}

@Composable
private fun PasteButton(isPasteButtonVisible: Boolean, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    if (isPasteButtonVisible) {
        Box(modifier = modifier) {
            Text(
                text = "Paste",
                style = TangemTheme.typography.button,
                color = TangemTheme.colors.text.primary2,
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.button.primary,
                        shape = TangemTheme.shapes.roundedCornersXMedium,
                    )
                    .padding(
                        horizontal = TangemTheme.dimens.spacing10,
                        vertical = TangemTheme.dimens.spacing2,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(radius = TangemTheme.dimens.radius8),
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick(
                                clipboardManager
                                    .getText()
                                    ?.toString()
                                    .orEmpty(),
                            )
                        },
                    ),
            )
        }
    } else {
        Icon(
            painter = painterResource(id = R.drawable.ic_close_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = stringResource(R.string.common_close),
            modifier = modifier
                .size(TangemTheme.dimens.size20)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(radius = TangemTheme.dimens.radius10),
                    onClick = { onClick("") },
                ),
        )
    }
}

@Composable
private fun SimpleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: TextReference? = null,
    singleLine: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val focusRequester = remember { FocusRequester() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TangemTheme.typography.body2.copy(color = TangemTheme.colors.text.primary1),
        cursorBrush = SolidColor(TangemTheme.colors.text.primary1),
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        decorationBox = { textValue ->
            Box {
                if (value.isBlank() && placeholder != null) {
                    Text(
                        text = placeholder.resolveReference(),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.disabled,
                        modifier = Modifier,
                    )
                }
                textValue()
            }
        },
        modifier = modifier
            .focusRequester(focusRequester),
    )
}

//region preview
@Preview
@Composable
private fun TextFieldPreview_Light() {
    TangemTheme {
        Column {
            TextFieldWithPaste(
                value = "",
                label = TextReference.Res(R.string.send_recipient),
                placeholder = TextReference.Res(R.string.send_enter_address_field),
                onValueChange = {},
                onPasteClick = {},
            )
            SpacerH8()
            TextFieldWithPasteAndIcon(
                value = "",
                label = TextReference.Res(R.string.send_extras_hint_memo),
                placeholder = TextReference.Res(R.string.send_optional_field),
                onValueChange = {},
                onPasteClick = {},
            )
            SpacerH8()
            TextFieldWithInfo(
                value = "Text",
                label = stringResource(R.string.send_extras_hint_memo),
                info = TextReference.Res(R.string.send_optional_field),
                footer = stringResource(R.string.send_max_fee),
                onValueChange = {},
            )
        }
    }
}

@Preview
@Composable
private fun TextFieldPreview_Dark() {
    TangemTheme(isDark = true) {
        Column {
            TextFieldWithPaste(
                value = "",
                label = TextReference.Res(R.string.send_recipient),
                placeholder = TextReference.Res(R.string.send_enter_address_field),
                onValueChange = {},
                onPasteClick = {},
            )
            SpacerH8()
            TextFieldWithPasteAndIcon(
                value = "",
                label = TextReference.Res(R.string.send_extras_hint_memo),
                placeholder = TextReference.Res(R.string.send_optional_field),
                onValueChange = {},
                onPasteClick = {},
            )
            SpacerH8()
            TextFieldWithInfo(
                value = "Text",
                label = stringResource(R.string.send_extras_hint_memo),
                info = TextReference.Res(R.string.send_optional_field),
                footer = stringResource(R.string.send_max_fee),
                onValueChange = {},
            )
        }
    }
}
//endregion