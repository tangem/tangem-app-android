package com.tangem.core.ui.components.appbar

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    @DrawableRes backIconRes: Int? = null,
    @DrawableRes iconRes: Int? = null,
    onIconClick: (() -> Unit)? = null,
    backgroundColor: Color = TangemTheme.colors.background.secondary,
) {
    Row(
        modifier = modifier
            .background(color = backgroundColor)
            .fillMaxWidth()
            .padding(all = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(backIconRes ?: R.drawable.ic_back_24),
            contentDescription = null,
            modifier = Modifier
                .size(size = TangemTheme.dimens.size24)
                .clickable { onBackClick() },
            tint = TangemTheme.colors.icon.primary1,
        )
        AnimatedContent(
            targetState = text,
            modifier = Modifier.weight(1f),
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "Toolbar title change",
        ) {
            if (!it.isNullOrBlank()) {
                Text(
                    text = it,
                    color = TangemTheme.colors.text.primary1,
                    maxLines = 1,
                    style = TangemTheme.typography.subtitle1,
                )
            }
        }
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
    }
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
