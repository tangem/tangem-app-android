package com.tangem.tap.common.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.wallet.R

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ButtonInternal(
        modifier = modifier,
        text = text,
        icon = ButtonIcon.None,
        onClick = onClick,
        colors = TangemButtonDefaults.primaryButtonColors,
        enabled = enabled,
    )
}

@Composable
fun PrimaryButtonIconRight(
    modifier: Modifier = Modifier,
    text: String,
    icon: Painter,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ButtonInternal(
        modifier = modifier,
        text = text,
        icon = ButtonIcon.Right(icon),
        onClick = onClick,
        colors = TangemButtonDefaults.primaryButtonColors,
        enabled = enabled,
    )
}

@Composable
fun PrimaryButtonIconLeft(
    modifier: Modifier = Modifier,
    text: String,
    icon: Painter,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ButtonInternal(
        modifier = modifier,
        text = text,
        icon = ButtonIcon.Left(icon),
        onClick = onClick,
        colors = TangemButtonDefaults.primaryButtonColors,
        enabled = enabled,
    )
}

@Composable
fun SecondaryButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ButtonInternal(
        modifier = modifier,
        text = text,
        icon = ButtonIcon.None,
        onClick = onClick,
        colors = TangemButtonDefaults.secondaryButtonColors,
        enabled = enabled,
    )
}

@Composable
fun SecondaryButtonIconRight(
    modifier: Modifier = Modifier,
    text: String,
    icon: Painter,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ButtonInternal(
        modifier = modifier,
        text = text,
        icon = ButtonIcon.Right(icon),
        onClick = onClick,
        colors = TangemButtonDefaults.secondaryButtonColors,
        enabled = enabled,
    )
}

@Composable
fun SecondaryButtonIconLeft(
    modifier: Modifier = Modifier,
    text: String,
    icon: Painter,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ButtonInternal(
        modifier = modifier,
        text = text,
        icon = ButtonIcon.Left(icon),
        onClick = onClick,
        colors = TangemButtonDefaults.secondaryButtonColors,
        enabled = enabled,
    )
}

@Composable
private fun ButtonInternal(
    modifier: Modifier = Modifier,
    text: String,
    icon: ButtonIcon,
    onClick: () -> Unit,
    colors: ButtonColors,
    enabled: Boolean,
    shape: Shape = TangemButtonDefaults.defaultShape,
    elevation: ButtonElevation = ButtonDefaults
        .elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
) {
    Button(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .height(IntrinsicSize.Min)
            .heightIn(min = 48.dp),
        onClick = onClick,
        enabled = enabled,
        elevation = elevation,
        shape = shape,
        colors = colors,
    ) {
        ButtonContent(
            text = text,
            buttonIcon = icon,
            colors = colors,
            enabled = enabled,
        )
    }
}

@Composable
private fun ButtonContent(
    text: String,
    buttonIcon: ButtonIcon,
    colors: ButtonColors,
    enabled: Boolean,
) {
    val icon = @Composable { painter: Painter ->
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painter,
            tint = colors.contentColor(enabled = enabled).value,
            contentDescription = null,
        )
    }
    if (buttonIcon is ButtonIcon.Left) {
        icon(buttonIcon.painter)
        Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
        text = text,
        style = TangemTypography.button,
        color = colors.contentColor(enabled = enabled).value,
    )
    if (buttonIcon is ButtonIcon.Right) {
        Spacer(modifier = Modifier.width(8.dp))
        icon(buttonIcon.painter)
    }
}

sealed interface ButtonIcon {
    val painter: Painter?

    @Immutable
    data class Left(override val painter: Painter) : ButtonIcon

    @Immutable
    data class Right(override val painter: Painter) : ButtonIcon

    object None : ButtonIcon {
        override val painter: Painter? = null
    }
}

private object TangemButtonDefaults {
    val primaryButtonColors: ButtonColors
        @Composable get() = TangemButtonColors(
            backgroundColor = colorResource(id = R.color.button_primary),
            contentColor = colorResource(id = R.color.text_primary_2),
            disabledBackgroundColor = colorResource(id = R.color.button_disabled),
            disabledContentColor = colorResource(id = R.color.text_disabled),
        )

    val secondaryButtonColors: ButtonColors
        @Composable get() = TangemButtonColors(
            backgroundColor = colorResource(id = R.color.button_secondary),
            contentColor = colorResource(id = R.color.text_primary_1),
            disabledBackgroundColor = colorResource(id = R.color.button_disabled),
            disabledContentColor = colorResource(id = R.color.text_disabled),
        )

    val defaultShape: Shape = RoundedCornerShape(size = 12.dp)
}

@Immutable
private class TangemButtonColors(
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

// region Preview
@Composable
private fun PrimaryButtonSample(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        PrimaryButtonIconRight(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem),
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        PrimaryButtonIconLeft(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem),
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            enabled = false,
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        PrimaryButtonIconRight(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem),
            enabled = false,
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        PrimaryButtonIconLeft(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem),
            enabled = false,
            onClick = { /* no-op */ },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PrimaryButtonPreview() {
    AppCompatTheme {
        PrimaryButtonSample()
    }
}

@Composable
private fun SecondaryButtonSample(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SecondaryButtonIconRight(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem),
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SecondaryButtonIconLeft(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem),
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            enabled = false,
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SecondaryButtonIconRight(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem),
            enabled = false,
            onClick = { /* no-op */ },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SecondaryButtonIconLeft(
            modifier = Modifier.fillMaxWidth(),
            text = "Manage tokens",
            icon = painterResource(id = R.drawable.ic_tangem),
            enabled = false,
            onClick = { /* no-op */ },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SecondaryButtonPreview() {
    AppCompatTheme {
        SecondaryButtonSample()
    }
}
// endregion Preview
