package com.tangem.core.ui.components.appbar

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

/**
 * App bar with back button and icon content for any possible number/style of icons
 *
 * @param onBackClick callback for back button
 * @param modifier modifier
 * @param text appbar title
 * @param backIconRes icon for back button
 * @param backgroundColor background color
 * @param iconContent icon content
 */
@Composable
fun AppBarWithBackButtonAndIconContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    @DrawableRes backIconRes: Int? = null,
    backIconTint: Color = TangemTheme.colors.icon.primary1,
    backgroundColor: Color = TangemTheme.colors.background.secondary,
    iconContent: @Composable () -> Unit,
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false),
                ) { onBackClick() },
            tint = backIconTint,
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
        iconContent()
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
private fun PreviewAppBarWithBackButtonAndIconInLightTheme() {
    TangemTheme(isDark = false) {
        AppBarWithBackButtonAndIconContent(
            text = "Title",
            onBackClick = {},
            iconContent = {
                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_qrcode_scan_24),
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = null,
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_flash_on_24),
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = null,
                    )
                }
            },
        )
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
private fun PreviewAppBarWithBackButtonAndIconInDarkTheme() {
    TangemTheme(isDark = true) {
        AppBarWithBackButtonAndIconContent(
            text = "Title",
            onBackClick = {},
            iconContent = {
                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_qrcode_scan_24),
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = null,
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_flash_on_24),
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = null,
                    )
                }
            },
        )
    }
}
