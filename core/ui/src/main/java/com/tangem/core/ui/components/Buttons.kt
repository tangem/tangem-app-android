package com.tangem.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

// region TextButton
/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=97%3A103&t=TmfD6UBHPg9uYfev-4)
 * */
@Composable
fun TextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.None,
        onClick = onClick,
        enabled = enabled,
        showProgress = false,
        colors = TangemButtonsDefaults.defaultTextButtonColors,
        size = TangemButtonSize.Text,
    )
}

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=97%3A62&t=TmfD6UBHPg9uYfev-4)
 * */
@Composable
fun TextButtonIconStart(
    text: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Start(iconResId),
        onClick = onClick,
        enabled = enabled,
        showProgress = false,
        colors = TangemButtonsDefaults.defaultTextButtonColors,
        size = TangemButtonSize.Text,
    )
}

@Composable
fun WarningTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.None,
        onClick = onClick,
        enabled = enabled,
        showProgress = false,
        colors = TangemButtonsDefaults.warningTextButtonColors,
        size = TangemButtonSize.Text,
    )
}
// endregion TextButton

// region PrimaryButton
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.None,
        onClick = onClick,
        colors = TangemButtonsDefaults.primaryButtonColors,
        enabled = enabled,
        showProgress = showProgress,
    )
}

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=68%3A47&t=TmfD6UBHPg9uYfev-4)
 * */
@Composable
fun PrimaryButtonIconEnd(
    text: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.End(iconResId),
        onClick = onClick,
        colors = TangemButtonsDefaults.primaryButtonColors,
        enabled = enabled,
        showProgress = showProgress,
    )
}

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=233%3A258&t=TmfD6UBHPg9uYfev-4)
 * */
@Composable
fun PrimaryButtonIconStart(
    text: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Start(iconResId),
        onClick = onClick,
        colors = TangemButtonsDefaults.primaryButtonColors,
        enabled = enabled,
        showProgress = showProgress,
    )
}
// endregion PrimaryButton

// region SecondaryButton
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.None,
        onClick = onClick,
        colors = TangemButtonsDefaults.secondaryButtonColors,
        enabled = enabled,
        showProgress = showProgress,
    )
}

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=99%3A50&t=TmfD6UBHPg9uYfev-4)
 * */
@Composable
fun SecondaryButtonIconEnd(
    text: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.End(iconResId),
        onClick = onClick,
        colors = TangemButtonsDefaults.secondaryButtonColors,
        enabled = enabled,
        showProgress = showProgress,
    )
}

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=233%3A262&t=TmfD6UBHPg9uYfev-4)
 * */
@Composable
fun SecondaryButtonIconStart(
    text: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Start(iconResId),
        onClick = onClick,
        colors = TangemButtonsDefaults.secondaryButtonColors,
        enabled = enabled,
        showProgress = showProgress,
    )
}
// endregion SecondaryButton

// region Other
@Composable
fun SelectorButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TangemButton(
        modifier = modifier,
        text = text,
        textStyle = TangemTheme.typography.subtitle2,
        icon = TangemButtonIcon.End(iconResId = R.drawable.ic_chevron_24),
        onClick = onClick,
        colors = TangemButtonsDefaults.selectorButtonColors,
        showProgress = false,
        enabled = enabled,
        size = TangemButtonSize.Selector,
    )
}
// endregion Other

// region Action

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=290-305&t=3z98eFnTeyIx5TH5-4)
 * */
@Composable
fun RoundedActionButton(
    text: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Start(iconResId),
        onClick = onClick,
        enabled = enabled,
        showProgress = false,
        colors = TangemButtonsDefaults.secondaryButtonColors,
        size = TangemButtonSize.RoundedAction,
    )
}

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=1208-1395&t=3z98eFnTeyIx5TH5-4)
 * */
@Composable
fun ActionButton(
    text: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Start(iconResId),
        onClick = onClick,
        enabled = enabled,
        showProgress = false,
        colors = TangemButtonsDefaults.secondaryButtonColors,
        size = TangemButtonSize.Action,
    )
}

/**
 * Same as [RoundedActionButton] but colored in primary background color
 * */
@Composable
fun BackgroundActionButton(
    text: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Start(iconResId),
        onClick = onClick,
        enabled = enabled,
        showProgress = false,
        colors = TangemButtonsDefaults.backgroundButtonColors,
        size = TangemButtonSize.RoundedAction,
    )
}
// endregion Action

// region Defaults
@Suppress("LongParameterList")
@Composable
private fun TangemButton(
    text: String,
    icon: TangemButtonIcon,
    onClick: () -> Unit,
    colors: ButtonColors,
    showProgress: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    size: TangemButtonSize = TangemButtonSize.Default,
    elevation: ButtonElevation = TangemButtonsDefaults.elevation,
    textStyle: TextStyle = TangemTheme.typography.button,
) {
    Button(
        modifier = modifier.heightIn(min = size.toHeightDp()),
        onClick = { if (!showProgress) onClick() },
        enabled = enabled,
        elevation = elevation,
        shape = size.toShape(),
        colors = colors,
        contentPadding = size.toContentPadding(icon = icon),
    ) {
        ButtonContent(
            text = text,
            textStyle = textStyle,
            buttonIcon = icon,
            colors = colors,
            showProgress = showProgress,
            enabled = enabled,
            size = size,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ButtonContent(
    text: String,
    textStyle: TextStyle,
    buttonIcon: TangemButtonIcon,
    colors: ButtonColors,
    size: TangemButtonSize,
    enabled: Boolean,
    showProgress: Boolean,
) {
    val icon = @Composable { iconResId: Int ->
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size20),
            painter = painterResource(id = iconResId),
            tint = colors.contentColor(enabled = enabled).value,
            contentDescription = null,
        )
    }

    if (showProgress) {
        Box(modifier = Modifier.wrapContentSize()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(TangemTheme.dimens.size24),
                color = colors.contentColor(enabled = enabled).value,
                strokeWidth = TangemTheme.dimens.size4,
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(size.toIconPadding()),
        ) {
            if (buttonIcon is TangemButtonIcon.Start) {
                icon(buttonIcon.iconResId)
            }
            Text(
                text = text,
                style = textStyle,
                color = colors.contentColor(enabled = enabled).value,
                maxLines = 1,
            )
            if (buttonIcon is TangemButtonIcon.End) {
                icon(buttonIcon.iconResId)
            }
        }
    }
}

@Immutable
private sealed interface TangemButtonIcon {
    val iconResId: Int?

    data class Start(override val iconResId: Int) : TangemButtonIcon

    data class End(override val iconResId: Int) : TangemButtonIcon

    object None : TangemButtonIcon {
        override val iconResId: Int? = null
    }
}

private enum class TangemButtonSize {
    Default,
    Text,
    Selector,
    Action,
    RoundedAction,
}

@Composable
@ReadOnlyComposable
private fun TangemButtonSize.toHeightDp(): Dp = when (this) {
    TangemButtonSize.Default -> TangemTheme.dimens.size48
    TangemButtonSize.Text -> TangemTheme.dimens.size40
    TangemButtonSize.Selector -> TangemTheme.dimens.size24
    TangemButtonSize.Action,
    TangemButtonSize.RoundedAction,
    -> TangemTheme.dimens.size36
}

@Composable
@ReadOnlyComposable
private fun TangemButtonSize.toShape(): Shape = when (this) {
    TangemButtonSize.Default -> TangemTheme.shapes.roundedCornersMedium
    TangemButtonSize.Text -> TangemTheme.shapes.roundedCornersSmall
    TangemButtonSize.Selector -> TangemTheme.shapes.roundedCornersSmall
    TangemButtonSize.Action -> TangemTheme.shapes.roundedCornersMedium
    TangemButtonSize.RoundedAction -> TangemTheme.shapes.roundedCornersLarge
}

@Composable
@ReadOnlyComposable
private fun TangemButtonSize.toIconPadding(): Dp = when (this) {
    TangemButtonSize.Default -> TangemTheme.dimens.spacing8
    TangemButtonSize.Text -> TangemTheme.dimens.spacing8
    TangemButtonSize.Selector -> 0.dp
    TangemButtonSize.Action,
    TangemButtonSize.RoundedAction,
    -> TangemTheme.dimens.spacing8
}

@Composable
@ReadOnlyComposable
private fun TangemButtonSize.toContentPadding(icon: TangemButtonIcon): PaddingValues {
    val horizontalPadding = this.toHorizontalContentPadding(icon = icon)

    return when (this) {
        TangemButtonSize.Default -> PaddingValues(
            top = TangemTheme.dimens.spacing14,
            bottom = TangemTheme.dimens.spacing14,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.Text -> PaddingValues(
            top = TangemTheme.dimens.spacing10,
            bottom = TangemTheme.dimens.spacing10,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.Selector -> PaddingValues(
            top = TangemTheme.dimens.spacing0_5,
            bottom = TangemTheme.dimens.spacing0_5,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.Action,
        TangemButtonSize.RoundedAction,
        -> PaddingValues(
            top = TangemTheme.dimens.spacing8,
            bottom = TangemTheme.dimens.spacing8,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun TangemButtonSize.toHorizontalContentPadding(icon: TangemButtonIcon): Pair<Dp, Dp> {
    return when (this) {
        TangemButtonSize.Default -> TangemTheme.dimens.spacing32 to TangemTheme.dimens.spacing32
        TangemButtonSize.Text -> when (icon) {
            is TangemButtonIcon.None -> TangemTheme.dimens.spacing16 to TangemTheme.dimens.spacing16
            is TangemButtonIcon.Start -> TangemTheme.dimens.spacing14 to TangemTheme.dimens.spacing16
            is TangemButtonIcon.End -> TangemTheme.dimens.spacing16 to TangemTheme.dimens.spacing14
        }
        TangemButtonSize.Selector -> TangemTheme.dimens.spacing0_5 to TangemTheme.dimens.spacing0_5
        TangemButtonSize.Action,
        TangemButtonSize.RoundedAction,
        -> when (icon) {
            is TangemButtonIcon.None -> TangemTheme.dimens.spacing24 to TangemTheme.dimens.spacing24
            is TangemButtonIcon.Start -> TangemTheme.dimens.spacing16 to TangemTheme.dimens.spacing24
            is TangemButtonIcon.End -> TangemTheme.dimens.spacing24 to TangemTheme.dimens.spacing16
        }
    }
}

private object TangemButtonsDefaults {
    val elevation: ButtonElevation
        @Composable get() = ButtonDefaults
            .elevation(
                defaultElevation = TangemTheme.dimens.elevation0,
                pressedElevation = TangemTheme.dimens.elevation0,
            )

    val primaryButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = TangemTheme.colors.button.primary,
            contentColor = TangemTheme.colors.text.primary2,
            disabledBackgroundColor = TangemTheme.colors.button.disabled,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val secondaryButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = TangemTheme.colors.button.secondary,
            contentColor = TangemTheme.colors.text.primary1,
            disabledBackgroundColor = TangemTheme.colors.button.disabled,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val defaultTextButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.secondary,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val warningTextButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.warning,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val selectorButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.tertiary,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val backgroundButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = TangemTheme.colors.background.primary,
            contentColor = TangemTheme.colors.text.primary1,
            disabledBackgroundColor = TangemTheme.colors.button.disabled,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )
}

@Immutable
private open class TangemButtonColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val disabledBackgroundColor: Color,
    private val disabledContentColor: Color,
) : ButtonColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(newValue = if (enabled) backgroundColor else disabledBackgroundColor)
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(newValue = if (enabled) contentColor else disabledContentColor)
    }
}
// endregion Defaults

// region Preview
@Composable
private fun PrimaryButtonSample() {
    Column(
        modifier = Modifier.background(TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        PrimaryButton(modifier = Modifier.fillMaxWidth(), text = "Manage tokens", onClick = { })
        PrimaryButton(modifier = Modifier.fillMaxWidth(), showProgress = true, text = "Manage tokens", onClick = { })
        PrimaryButtonIconEnd(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            iconResId = R.drawable.ic_tangem_24,
            onClick = { },
        )
        PrimaryButtonIconStart(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            iconResId = R.drawable.ic_tangem_24,
            onClick = { },
        )
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            enabled = false,
            onClick = { },
        )
        PrimaryButtonIconEnd(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            iconResId = R.drawable.ic_tangem_24,
            enabled = false,
            onClick = { },
        )
        PrimaryButtonIconStart(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            iconResId = R.drawable.ic_tangem_24,
            enabled = false,
            onClick = { },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PrimaryButtonPreview_Light() {
    TangemTheme {
        PrimaryButtonSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PrimaryButtonPreview_Dark() {
    TangemTheme(isDark = true) {
        PrimaryButtonSample()
    }
}

@Composable
private fun SecondaryButtonSample() {
    Column(
        modifier = Modifier.background(TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        SecondaryButton(modifier = Modifier.fillMaxWidth(), text = "Manage tokens", onClick = { })
        SecondaryButton(modifier = Modifier.fillMaxWidth(), showProgress = true, text = "Manage tokens", onClick = { })
        SecondaryButtonIconEnd(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            iconResId = R.drawable.ic_tangem_24,
            onClick = { },
        )
        SecondaryButtonIconStart(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            iconResId = R.drawable.ic_tangem_24,
            onClick = { },
        )
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            enabled = false,
            onClick = { },
        )
        SecondaryButtonIconEnd(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            iconResId = R.drawable.ic_tangem_24,
            enabled = false,
            onClick = { },
        )
        SecondaryButtonIconStart(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            iconResId = R.drawable.ic_tangem_24,
            enabled = false,
            onClick = { },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SecondaryButtonPreview_Light() {
    TangemTheme {
        SecondaryButtonSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SecondaryButtonPreview_Dark() {
    TangemTheme(isDark = true) {
        SecondaryButtonSample()
    }
}

@Composable
private fun TextButtonSample() {
    Column(
        modifier = Modifier.background(TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        TextButton(text = "Enabled", onClick = { })
        TextButtonIconStart(text = "Enabled", iconResId = R.drawable.ic_plus_24, onClick = { })
        TextButton(text = "Enabled", enabled = false, onClick = { })
        TextButtonIconStart(text = "Enabled", iconResId = R.drawable.ic_plus_24, enabled = false, onClick = { })
        WarningTextButton(text = "Delete", onClick = { })
        SelectorButton(text = "USD", onClick = { })
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TextButtonPreview_LightTheme() {
    TangemTheme {
        TextButtonSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TextButtonPreview_DarkTheme() {
    TangemTheme(isDark = true) {
        TextButtonSample()
    }
}

@Composable
private fun ActionButtonSample() {
    Column(
        modifier = Modifier.background(TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        RoundedActionButton(text = "Send", iconResId = R.drawable.ic_arrow_up_24, onClick = { })
        ActionButton(text = "Send", iconResId = R.drawable.ic_arrow_up_24, onClick = { })
        BackgroundActionButton(text = "Send", iconResId = R.drawable.ic_arrow_up_24, onClick = { })
        RoundedActionButton(text = "Send", iconResId = R.drawable.ic_arrow_up_24, enabled = false, onClick = { })
        ActionButton(text = "Send", iconResId = R.drawable.ic_arrow_up_24, enabled = false, onClick = { })
        BackgroundActionButton(text = "Send", iconResId = R.drawable.ic_arrow_up_24, enabled = false, onClick = { })
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ActionButtonPreview_LightTheme() {
    TangemTheme {
        ActionButtonSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ActionButtonPreview_DarkTheme() {
    TangemTheme(isDark = true) {
        ActionButtonSample()
    }
}
// endregion Preview