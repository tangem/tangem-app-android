package com.tangem.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
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
        icon = TangemButtonIconPosition.None,
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
        icon = TangemButtonIconPosition.Start(iconResId),
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
        icon = TangemButtonIconPosition.None,
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
        icon = TangemButtonIconPosition.None,
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
        icon = TangemButtonIconPosition.End(iconResId),
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
        icon = TangemButtonIconPosition.Start(iconResId),
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
        icon = TangemButtonIconPosition.None,
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
        icon = TangemButtonIconPosition.End(iconResId),
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
        icon = TangemButtonIconPosition.Start(iconResId),
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
        icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_chevron_24),
        onClick = onClick,
        colors = TangemButtonsDefaults.selectorButtonColors,
        showProgress = false,
        enabled = enabled,
        size = TangemButtonSize.Selector,
    )
}
// endregion Other

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

// endregion Preview
