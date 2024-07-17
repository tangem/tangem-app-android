package com.tangem.core.ui.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=213%3A218&t=TmfD6UBHPg9uYfev-4)
 * */
@Composable
fun OutlineTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    caption: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    TangemTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = label,
        placeholder = placeholder,
        caption = caption,
        enabled = enabled,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
    )
}

@Composable
fun OutlineTextFieldWithIcon(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    @DrawableRes iconResId: Int,
    iconColor: Color,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    TangemTextFieldWithIcon(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        iconResId = iconResId,
        iconColor = iconColor,
        singleLine = true,
        label = label,
        placeholder = placeholder,
        enabled = enabled,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        onIconClick = onIconClick,
    )
}

// region Defaults
@Suppress("LongMethod")
@Composable
private fun TangemTextField(
    value: TextFieldValue,
    singleLine: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    caption: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TangemTextFieldColors = TangemTextFieldsDefault.defaultTextFieldColors,
    size: TangemTextFieldSize = TangemTextFieldSize.Default,
    onClear: () -> Unit = { onValueChange(TextFieldValue()) },
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .heightIn(size.toHeightDp()),
            value = value,
            textStyle = TangemTheme.typography.body1,
            onValueChange = onValueChange,
            singleLine = singleLine,
            enabled = enabled,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardActions = keyboardActions,
            keyboardOptions = keyboardOptions,
            interactionSource = interactionSource,
            shape = size.toShape(),
            colors = colors,
            label = {
                if (!label.isNullOrEmpty()) {
                    Text(
                        text = label,
                        style = TangemTheme.typography.caption2,
                        color = colors.labelColor(
                            enabled = enabled,
                            error = isError,
                            interactionSource = interactionSource,
                        ).value,
                    )
                }
            },
            placeholder = {
                if (!placeholder.isNullOrEmpty()) {
                    Text(
                        text = placeholder,
                        style = TangemTheme.typography.body1,
                        color = colors.placeholderColor(enabled = enabled).value,
                    )
                }
            },
            trailingIcon = {
                val iconRes by rememberUpdatedState(
                    newValue = if (isError) {
                        R.drawable.ic_alert_24
                    } else if (value.text.isNotEmpty()) {
                        R.drawable.ic_close_24
                    } else {
                        null
                    },
                )
                if (iconRes != null) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onClear,
                        enabled = !isError && enabled,
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = iconRes!!),
                            tint = colors.trailingIconColor(enabled = enabled, isError = isError).value,
                            contentDescription = "Clear input",
                        )
                    }
                }
            },
        )

        AnimatedVisibility(
            visible = !caption.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            if (caption.isNullOrEmpty()) return@AnimatedVisibility
            Column {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = caption,
                    style = TangemTheme.typography.body1,
                    color = colors.captionColor(enabled = enabled, isError = isError).value,
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun TangemTextFieldWithIcon(
    value: TextFieldValue,
    singleLine: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    @DrawableRes iconResId: Int,
    iconColor: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TangemTextFieldColors = TangemTextFieldsDefault.defaultTextFieldColors,
    size: TangemTextFieldSize = TangemTextFieldSize.Default,
    onIconClick: () -> Unit = {},
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .heightIn(size.toHeightDp()),
            value = value,
            textStyle = TangemTheme.typography.body1,
            onValueChange = onValueChange,
            singleLine = singleLine,
            enabled = enabled,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardActions = keyboardActions,
            keyboardOptions = keyboardOptions,
            interactionSource = interactionSource,
            shape = size.toShape(),
            colors = colors,
            label = {
                if (!label.isNullOrEmpty()) {
                    Text(
                        text = label,
                        style = TangemTheme.typography.caption2,
                        color = colors.labelColor(
                            enabled = enabled,
                            error = isError,
                            interactionSource = interactionSource,
                        ).value,
                    )
                }
            },
            placeholder = {
                if (!placeholder.isNullOrEmpty()) {
                    Text(
                        text = placeholder,
                        style = TangemTheme.typography.body1,
                        color = colors.placeholderColor(enabled = enabled).value,
                    )
                }
            },
            trailingIcon = {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = onIconClick,
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = iconResId),
                        tint = iconColor,
                        contentDescription = "Clear input",
                    )
                }
            },
        )
    }
}

private enum class TangemTextFieldSize {
    Default,
}

@Composable
private fun TangemTextFieldSize.toHeightDp(): Dp = when (this) {
    TangemTextFieldSize.Default -> TangemTheme.dimens.size56
}

@Composable
private fun TangemTextFieldSize.toShape(): Shape = when (this) {
    TangemTextFieldSize.Default -> TangemTheme.shapes.roundedCornersSmall2
}

object TangemTextFieldsDefault {
    val defaultTextFieldColors: TangemTextFieldColors
        @Composable @Stable get() = TangemTextFieldColors(
            textColor = TangemTheme.colors.text.primary1,
            disabledTextColor = TangemTheme.colors.text.disabled,
            backgroundColor = Color.Transparent,
            cursorColor = TangemTheme.colors.icon.primary1,
            errorCursorColor = TangemTheme.colors.icon.warning,
            focusedIndicatorColor = TangemTheme.colors.icon.primary1,
            unfocusedIndicatorColor = TangemTheme.colors.stroke.secondary,
            disabledIndicatorColor = TangemTheme.colors.stroke.secondary,
            errorIndicatorColor = TangemTheme.colors.icon.warning,
            leadingIconColor = TangemTheme.colors.icon.informative,
            disabledLeadingIconColor = Color.Transparent,
            errorLeadingIconColor = TangemTheme.colors.icon.warning,
            trailingIconColor = TangemTheme.colors.icon.informative,
            disabledTrailingIconColor = Color.Transparent,
            errorTrailingIconColor = TangemTheme.colors.icon.warning,
            focusedLabelColor = TangemTheme.colors.text.primary1,
            unfocusedLabelColor = TangemTheme.colors.text.secondary,
            disabledLabelColor = TangemTheme.colors.text.disabled,
            errorLabelColor = TangemTheme.colors.icon.warning,
            placeholderColor = TangemTheme.colors.text.secondary,
            disabledPlaceholderColor = TangemTheme.colors.text.disabled,
            captionColor = TangemTheme.colors.text.tertiary,
            disabledCaptionColor = TangemTheme.colors.text.disabled,
            errorCaptionColor = TangemTheme.colors.icon.warning,
        )
}

@Immutable
data class TangemTextFieldColors(
    private val textColor: Color,
    private val disabledTextColor: Color,
    private val cursorColor: Color,
    private val errorCursorColor: Color,
    private val focusedIndicatorColor: Color,
    private val unfocusedIndicatorColor: Color,
    private val errorIndicatorColor: Color,
    private val disabledIndicatorColor: Color,
    private val leadingIconColor: Color,
    private val disabledLeadingIconColor: Color,
    private val errorLeadingIconColor: Color,
    private val trailingIconColor: Color,
    private val disabledTrailingIconColor: Color,
    private val errorTrailingIconColor: Color,
    private val backgroundColor: Color,
    private val focusedLabelColor: Color,
    private val unfocusedLabelColor: Color,
    private val disabledLabelColor: Color,
    private val errorLabelColor: Color,
    private val placeholderColor: Color,
    private val disabledPlaceholderColor: Color,
    private val captionColor: Color,
    private val disabledCaptionColor: Color,
    private val errorCaptionColor: Color,
) : TextFieldColors {

    @Composable
    override fun leadingIconColor(enabled: Boolean, isError: Boolean): State<Color> {
        return rememberUpdatedState(
            when {
                !enabled -> disabledLeadingIconColor
                isError -> errorLeadingIconColor
                else -> leadingIconColor
            },
        )
    }

    @Composable
    override fun trailingIconColor(enabled: Boolean, isError: Boolean): State<Color> {
        return rememberUpdatedState(
            when {
                !enabled -> disabledTrailingIconColor
                isError -> errorTrailingIconColor
                else -> trailingIconColor
            },
        )
    }

    @Composable
    override fun indicatorColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledIndicatorColor
            isError -> errorIndicatorColor
            focused -> focusedIndicatorColor
            else -> unfocusedIndicatorColor
        }
        return if (enabled) {
            animateColorAsState(
                targetValue = targetValue,
                animationSpec = tween(durationMillis = 120),
                label = "IndicatorColor",
            )
        } else {
            rememberUpdatedState(targetValue)
        }
    }

    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(backgroundColor)
    }

    @Composable
    override fun placeholderColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) placeholderColor else disabledPlaceholderColor)
    }

    @Composable
    override fun labelColor(enabled: Boolean, error: Boolean, interactionSource: InteractionSource): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledLabelColor
            error -> errorLabelColor
            focused -> focusedLabelColor
            else -> unfocusedLabelColor
        }
        return rememberUpdatedState(targetValue)
    }

    @Composable
    override fun textColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) textColor else disabledTextColor)
    }

    @Composable
    override fun cursorColor(isError: Boolean): State<Color> {
        return rememberUpdatedState(if (isError) errorCursorColor else cursorColor)
    }

    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun captionColor(enabled: Boolean, isError: Boolean): State<Color> {
        return rememberUpdatedState(
            newValue = when {
                !enabled -> disabledCaptionColor
                isError -> errorCaptionColor
                else -> captionColor
            },
        )
    }
}
// endregion Defaults

// region Preview
@Composable
private fun OutlineTextFieldSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .padding(horizontal = 16.dp),
    ) {
        val text = TextFieldValue(text = "Input")
        OutlineTextField(
            value = text,
            onValueChange = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        OutlineTextField(
            value = text,
            onValueChange = { /* no-op */ },
            label = "Default",
            placeholder = "Placeholder",
            caption = "Supporting text",
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        OutlineTextField(
            value = text,
            onValueChange = { /* no-op */ },
            enabled = false,
            label = "Disabled",
            placeholder = "Placeholder",
            caption = "Supporting text",
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        OutlineTextField(
            value = text,
            onValueChange = { /* no-op */ },
            isError = true,
            label = "Error",
            placeholder = "Placeholder",
            caption = "Supporting text",
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        OutlineTextField(
            value = TextFieldValue(),
            onValueChange = { /* no-op */ },
            label = "Default without value",
            placeholder = "Placeholder",
            caption = "Supporting text",
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        OutlineTextFieldWithIcon(
            value = TextFieldValue(),
            onValueChange = { /* no-op */ },
            iconResId = R.drawable.ic_alert_24,
            iconColor = TangemTheme.colors.icon.informative,
            label = "Default without value",
            placeholder = "Placeholder",
            onIconClick = {},
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OutlineTextFieldPreview() {
    TangemThemePreview {
        OutlineTextFieldSample()
    }
}
// endregion Preview