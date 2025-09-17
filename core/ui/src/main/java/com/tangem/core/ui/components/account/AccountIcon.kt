package com.tangem.core.ui.components.account

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

enum class AccountIconSize {
    Default, Large, Medium, Small, ExtraSmall
}

/**
 * Displays a portfolio icon using a predefined vector resource.
 *
 * The background color is defined by [color], and the icon tint is constant white.
 * The icon size and container shape are adapted based on the provided [size].
 *
 * @param resId The vector drawable resource ID to display.
 * @param color The background color of the icon container.
 * @param size The size of the icon, defined by [AccountIconSize].
 */
@Composable
fun AccountResIcon(@DrawableRes resId: Int, color: Color, size: AccountIconSize, modifier: Modifier = Modifier) {
    val boxModifier = modifier.selectBoxModifier(size)
    val iconSize = Modifier.selectIconSize(size)
    Box(
        contentAlignment = Alignment.Center,
        modifier = boxModifier.background(color),
    ) {
        Icon(
            modifier = iconSize,
            tint = TangemTheme.colors.text.constantWhite,
            imageVector = ImageVector.vectorResource(id = resId),
            contentDescription = null,
        )
    }
}

/**
 * Displays a portfolio icon using a single character.
 *
 * The background color is defined by [color], the text is uppercased,
 * and its style is chosen based on the provided [size].
 * The container shape and size also adapt to [size].
 *
 * @param char The character to display inside the icon.
 * @param color The background color of the icon container.
 * @param size The size of the icon, defined by [AccountIconSize].
 */
@Composable
fun AccountCharIcon(char: Char, color: Color, size: AccountIconSize, modifier: Modifier = Modifier) {
    val boxModifier = modifier.selectBoxModifier(size)
    val textStyle = when (size) {
        AccountIconSize.Default -> TangemTheme.typography.h3
        AccountIconSize.Large -> TangemTheme.typography.h1
        AccountIconSize.Medium -> TangemTheme.typography.subtitle1
        AccountIconSize.Small -> TangemTheme.typography.subtitle2
        AccountIconSize.ExtraSmall -> TangemTheme.typography.caption1
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = boxModifier.background(color),
    ) {
        Text(
            text = char.uppercase(),
            style = textStyle,
            color = TangemTheme.colors.text.constantWhite,
        )
    }
}

private fun Modifier.selectIconSize(size: AccountIconSize): Modifier = when (size) {
    AccountIconSize.Default -> this.size(20.dp)
    AccountIconSize.Large -> this.size(40.dp)
    AccountIconSize.Medium -> this.size(16.dp)
    AccountIconSize.Small -> this.size(12.dp)
    AccountIconSize.ExtraSmall -> this.size(8.dp)
}

private fun Modifier.selectBoxModifier(size: AccountIconSize): Modifier = when (size) {
    AccountIconSize.Default -> size(36.dp).clip(RoundedCornerShape(10.dp))
    AccountIconSize.Large -> size(88.dp).clip(RoundedCornerShape(24.dp))
    AccountIconSize.Medium -> size(28.dp).clip(RoundedCornerShape(8.dp))
    AccountIconSize.Small -> size(20.dp).clip(RoundedCornerShape(6.dp))
    AccountIconSize.ExtraSmall -> size(14.dp).clip(RoundedCornerShape(4.dp))
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        Row(
            modifier = Modifier.background(TangemTheme.colors.background.primary),
        ) {
            Sample()
        }
    }
}

@Composable
private fun Sample() {
    Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
        AccountResIcon(resId = R.drawable.ic_shirt_24, color = Color.Red, size = AccountIconSize.Default)
        AccountResIcon(resId = R.drawable.ic_rounded_star_24, color = Color.Blue, size = AccountIconSize.Large)
        AccountResIcon(resId = R.drawable.ic_user_24, color = Color.Magenta, size = AccountIconSize.Medium)
        AccountResIcon(resId = R.drawable.ic_family_24, color = Color.DarkGray, size = AccountIconSize.Small)
        AccountResIcon(resId = R.drawable.ic_wallet_24, color = Color.Green, size = AccountIconSize.ExtraSmall)
    }
    Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
        AccountCharIcon(char = 'D', color = Color.Red, size = AccountIconSize.Default)
        AccountCharIcon(char = 'L', color = Color.Blue, size = AccountIconSize.Large)
        AccountCharIcon(char = 'M', color = Color.Magenta, size = AccountIconSize.Medium)
        AccountCharIcon(char = 'S', color = Color.DarkGray, size = AccountIconSize.Small)
        AccountCharIcon(char = 'E', color = Color.Green, size = AccountIconSize.ExtraSmall)
    }
}