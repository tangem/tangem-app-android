package com.tangem.core.ui.components.appbar

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

@Composable
fun AppBarWithBackButtonAndIcon(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    subtitle: String? = null,
    @DrawableRes backIconRes: Int? = null,
    @DrawableRes iconRes: Int? = null,
    onIconClick: (() -> Unit)? = null,
    backgroundColor: Color = TangemTheme.colors.background.secondary,
) {
    AppBarWithBackButtonAndIconContent(
        onBackClick = onBackClick,
        modifier = modifier,
        text = text,
        subtitle = subtitle,
        backIconRes = backIconRes,
        backgroundColor = backgroundColor,
        iconContent = {
            AnimatedContent(
                targetState = iconRes,
                transitionSpec = { (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut()) },
                label = "Toolbar icon change",
            ) {
                if (onIconClick != null && it != null) {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                        modifier = Modifier
                            .size(size = TangemTheme.dimens.size24)
                            .clickable { onIconClick() },
                        tint = TangemTheme.colors.icon.primary1,
                    )
                }
            }
        },
    )
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
private fun PreviewAppBarWithBackButtonAndIconInLightTheme() {
    TangemTheme(isDark = false) {
        AppBarWithBackButtonAndIcon(
            text = "Title",
            iconRes = R.drawable.ic_qrcode_scan_24,
            onBackClick = {},
            onIconClick = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
private fun PreviewAppBarWithBackButtonAndIconInDarkTheme() {
    TangemTheme(isDark = true) {
        AppBarWithBackButtonAndIcon(
            text = "Title",
            iconRes = R.drawable.ic_qrcode_scan_24,
            onBackClick = {},
            onIconClick = {},
        )
    }
}