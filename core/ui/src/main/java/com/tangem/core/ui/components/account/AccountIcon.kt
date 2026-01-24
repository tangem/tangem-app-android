package com.tangem.core.ui.components.account

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val boxSize by animateDpAsState(
        targetValue = size.boxSizeInDp(),
        animationSpec = animation(),
    )
    val boxShapeCornerSize by animateDpAsState(
        targetValue = size.boxShapeSizeInDp(),
        animationSpec = animation(),
    )
    val iconSize by animateDpAsState(
        targetValue = size.iconSizeInDp(),
        animationSpec = animation(),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(boxSize)
            .clip(RoundedCornerShape(boxShapeCornerSize))
            .background(color),
    ) {
        Icon(
            modifier = Modifier.size(iconSize),
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
    val boxSize by animateDpAsState(
        size.boxSizeInDp(),
        animationSpec = animation(),
    )
    val boxShapeCornerSize by animateDpAsState(
        size.boxShapeSizeInDp(),
        animationSpec = animation(),
    )
    val textStyle = when (size) {
        AccountIconSize.Default -> TangemTheme.typography.h3
        AccountIconSize.Large -> TangemTheme.typography.h1
        AccountIconSize.Medium -> TangemTheme.typography.subtitle1
        AccountIconSize.Small -> TangemTheme.typography.subtitle2
        AccountIconSize.ExtraSmall -> TangemTheme.typography.caption1
    }

    val textSize by animateFloatAsState(
        textStyle.fontSize.value,
        animationSpec = animation(),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(boxSize)
            .clip(RoundedCornerShape(boxShapeCornerSize))
            .background(color),
    ) {
        Text(
            modifier = Modifier.animateContentSize(animationSpec = animation()),
            text = char.uppercase(),
            style = textStyle.copy(fontSize = textSize.sp),
            color = TangemTheme.colors.text.constantWhite,
        )
    }
}

private fun <T> animation() = tween<T>(durationMillis = 350)

private fun AccountIconSize.iconSizeInDp(): Dp = when (this) {
    AccountIconSize.Default -> 20.dp
    AccountIconSize.Large -> 40.dp
    AccountIconSize.Medium -> 16.dp
    AccountIconSize.Small -> 12.dp
    AccountIconSize.ExtraSmall -> 8.dp
}

private fun AccountIconSize.boxSizeInDp(): Dp = when (this) {
    AccountIconSize.Default -> 36.dp
    AccountIconSize.Large -> 88.dp
    AccountIconSize.Medium -> 28.dp
    AccountIconSize.Small -> 20.dp
    AccountIconSize.ExtraSmall -> 14.dp
}

private fun AccountIconSize.boxShapeSizeInDp(): Dp = when (this) {
    AccountIconSize.Default -> 10.dp
    AccountIconSize.Large -> 24.dp
    AccountIconSize.Medium -> 8.dp
    AccountIconSize.Small -> 6.dp
    AccountIconSize.ExtraSmall -> 4.dp
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
    var sizeState by remember { mutableStateOf(AccountIconSize.ExtraSmall) }

    Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
        AccountResIcon(resId = R.drawable.ic_shirt_24, color = Color.Red, size = sizeState)
        Button(onClick = {
            sizeState = when (sizeState) {
                AccountIconSize.Default -> AccountIconSize.Large
                AccountIconSize.Large -> AccountIconSize.Medium
                AccountIconSize.Medium -> AccountIconSize.Small
                AccountIconSize.Small -> AccountIconSize.ExtraSmall
                AccountIconSize.ExtraSmall -> AccountIconSize.Default
            }
        }) { Text("Change") }

        AccountResIcon(resId = R.drawable.ic_shirt_24, color = Color.Red, size = AccountIconSize.Default)
        AccountResIcon(resId = R.drawable.ic_rounded_star_24, color = Color.Blue, size = AccountIconSize.Large)
        AccountResIcon(resId = R.drawable.ic_user_24, color = Color.Magenta, size = AccountIconSize.Medium)
        AccountResIcon(resId = R.drawable.ic_family_24, color = Color.DarkGray, size = AccountIconSize.Small)
        AccountResIcon(resId = R.drawable.ic_wallet_24, color = Color.Green, size = AccountIconSize.ExtraSmall)
    }
    Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
        AccountCharIcon(char = 'D', color = Color.Red, size = sizeState)
        AccountCharIcon(char = 'D', color = Color.Red, size = AccountIconSize.Default)
        AccountCharIcon(char = 'L', color = Color.Blue, size = AccountIconSize.Large)
        AccountCharIcon(char = 'M', color = Color.Magenta, size = AccountIconSize.Medium)
        AccountCharIcon(char = 'S', color = Color.DarkGray, size = AccountIconSize.Small)
        AccountCharIcon(char = 'E', color = Color.Green, size = AccountIconSize.ExtraSmall)
    }
}