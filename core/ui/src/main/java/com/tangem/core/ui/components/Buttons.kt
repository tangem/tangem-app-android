package com.tangem.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
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
fun TextButtonIconLeft(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Left(icon),
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
fun PrimaryButtonIconRight(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Right(icon),
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
fun PrimaryButtonIconLeft(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Left(icon),
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
fun SecondaryButtonIconRight(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Right(icon),
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
fun SecondaryButtonIconLeft(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Left(icon),
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
        icon = TangemButtonIcon.Right(painterResource(id = R.drawable.ic_chevron_24)),
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
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Left(icon),
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
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Left(icon),
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
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIcon.Left(icon),
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
        modifier = modifier
            .width(IntrinsicSize.Min)
            .heightIn(min = size.toHeightDp()),
        onClick = {
            if (!showProgress) {
                onClick()
            }
        },
        enabled = enabled,
        elevation = elevation,
        shape = size.toShape(),
        colors = colors,
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
    val icon = @Composable { painter: Painter ->
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size20),
            painter = painter,
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
            if (buttonIcon is TangemButtonIcon.Left) {
                icon(buttonIcon.painter)
            }
            Text(
                text = text,
                style = textStyle,
                color = colors.contentColor(enabled = enabled).value,
            )
            if (buttonIcon is TangemButtonIcon.Right) {
                icon(buttonIcon.painter)
            }
        }
    }
}

@Immutable
private sealed interface TangemButtonIcon {
    val painter: Painter?

    data class Left(override val painter: Painter) : TangemButtonIcon

    data class Right(override val painter: Painter) : TangemButtonIcon

    object None : TangemButtonIcon {
        override val painter: Painter? = null
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
private fun TangemButtonSize.toHeightDp(): Dp = when (this) {
    TangemButtonSize.Default -> TangemTheme.dimens.size48
    TangemButtonSize.Text -> TangemTheme.dimens.size40
    TangemButtonSize.Selector -> TangemTheme.dimens.size24
    TangemButtonSize.Action,
    TangemButtonSize.RoundedAction,
    -> TangemTheme.dimens.size36
}

@Composable
private fun TangemButtonSize.toShape(): Shape = when (this) {
    TangemButtonSize.Default -> TangemTheme.shapes.roundedCornersMedium
    TangemButtonSize.Text -> TangemTheme.shapes.roundedCornersSmall
    TangemButtonSize.Selector -> TangemTheme.shapes.roundedCornersSmall
    TangemButtonSize.Action -> TangemTheme.shapes.roundedCornersMedium
    TangemButtonSize.RoundedAction -> TangemTheme.shapes.roundedCornersLarge
}

@Composable
private fun TangemButtonSize.toIconPadding(): Dp = when (this) {
    TangemButtonSize.Default -> TangemTheme.dimens.spacing8
    TangemButtonSize.Text -> TangemTheme.dimens.spacing8
    TangemButtonSize.Selector -> 0.dp
    TangemButtonSize.Action,
    TangemButtonSize.RoundedAction,
    -> TangemTheme.dimens.spacing8
}

object TangemButtonsDefaults {
    val elevation: ButtonElevation
        @Composable get() = ButtonDefaults
            .elevation(
                defaultElevation = TangemTheme.dimens.elevation0,
                pressedElevation = TangemTheme.dimens.elevation0,
            )

    val primaryButtonColors: ButtonColors
        @Composable get() = TangemButtonColors(
            backgroundColor = TangemTheme.colors.button.primary,
            contentColor = TangemTheme.colors.text.primary2,
            disabledBackgroundColor = TangemTheme.colors.button.disabled,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val secondaryButtonColors: ButtonColors
        @Composable get() = TangemButtonColors(
            backgroundColor = TangemTheme.colors.button.secondary,
            contentColor = TangemTheme.colors.text.primary1,
            disabledBackgroundColor = TangemTheme.colors.button.disabled,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val defaultTextButtonColors: ButtonColors
        @Composable get() = TangemButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.secondary,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val warningTextButtonColors: ButtonColors
        @Composable get() = TangemButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.warning,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val selectorButtonColors: ButtonColors
        @Composable get() = TangemButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.tertiary,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val backgroundButtonColors: ButtonColors
        @Composable get() = TangemButtonColors(
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
private fun PrimaryButtonSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            showProgress = true,
            text = "Manage tokens",
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        PrimaryButtonIconRight(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem_24),
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        PrimaryButtonIconLeft(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem_24),
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            enabled = false,
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        PrimaryButtonIconRight(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem_24),
            enabled = false,
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        PrimaryButtonIconLeft(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem_24),
            enabled = false,
            onClick = { /* no-op */ },
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
private fun SecondaryButtonSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            showProgress = true,
            text = "Manage tokens",
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        SecondaryButtonIconRight(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem_24),
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        SecondaryButtonIconLeft(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem_24),
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            enabled = false,
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        SecondaryButtonIconRight(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem_24),
            enabled = false,
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        SecondaryButtonIconLeft(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem_24),
            enabled = false,
            onClick = { /* no-op */ },
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
private fun TextButtonSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        TextButton(
            text = "Enabled",
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        TextButtonIconLeft(
            text = "Enabled",
            icon = painterResource(id = R.drawable.ic_plus_24),
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        TextButton(
            text = "Enabled",
            enabled = false,
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        TextButtonIconLeft(
            text = "Enabled",
            icon = painterResource(id = R.drawable.ic_plus_24),
            enabled = false,
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        WarningTextButton(
            text = "Delete",
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        SelectorButton(
            text = "USD",
            onClick = { /* no-op */ },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TextButtonPreview_Light() {
    TangemTheme {
        TextButtonSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TextButtonPreview_Dark() {
    TangemTheme(isDark = true) {
        TextButtonSample()
    }
}

@Composable
private fun ActionButtonSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        RoundedActionButton(
            text = "Send",
            icon = painterResource(id = R.drawable.ic_arrow_up_24),
            onClick = { /* [REDACTED_TODO_COMMENT]*/ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        ActionButton(
            text = "Send",
            icon = painterResource(id = R.drawable.ic_arrow_up_24),
            onClick = { /* [REDACTED_TODO_COMMENT]*/ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        BackgroundActionButton(
            text = "Send",
            icon = painterResource(id = R.drawable.ic_arrow_up_24),
            onClick = { /* [REDACTED_TODO_COMMENT]*/ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        RoundedActionButton(
            text = "Send",
            icon = painterResource(id = R.drawable.ic_arrow_up_24),
            enabled = false,
            onClick = { /* [REDACTED_TODO_COMMENT]*/ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        ActionButton(
            text = "Send",
            icon = painterResource(id = R.drawable.ic_arrow_up_24),
            enabled = false,
            onClick = { /* [REDACTED_TODO_COMMENT]*/ },
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        BackgroundActionButton(
            text = "Send",
            icon = painterResource(id = R.drawable.ic_arrow_up_24),
            enabled = false,
            onClick = { /* [REDACTED_TODO_COMMENT]*/ },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ActionButtonPreview_Light() {
    TangemTheme {
        ActionButtonSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ActionButtonPreview_Dark() {
    TangemTheme(isDark = true) {
        ActionButtonSample()
    }
}
// endregion Preview