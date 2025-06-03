package com.tangem.features.markets.tokenlist.impl.ui.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.notifications.CloseableIconButton
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

private val bgColor = Color(0x1F8CD9FF)
private val borderColor = Color(0x3D8CD9FF)

@Composable
fun StakingInMarketsPromoNotification(config: NotificationConfig, modifier: Modifier = Modifier) {
    var textHeightDp by remember { mutableStateOf(0.dp) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                shape = TangemTheme.shapes.roundedCornersXMedium,
                color = borderColor,
            )
            .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
            .background(bgColor)
            .clickable { config.onClick?.invoke() },
    ) {
        PromoImage(
            iconRes = config.iconResId,
            modifier = Modifier.height(textHeightDp),
        )
        PromoText(
            title = config.title,
            subtitle = config.subtitle,
            onSizeChange = { textHeightDp = it },
        )
        CloseableIconButton(
            onClick = config.onCloseClick,
            modifier = Modifier.align(alignment = Alignment.TopEnd),
            iconTint = TangemTheme.colors.icon.secondary,
        )
    }
}

@Composable
private fun PromoImage(@DrawableRes iconRes: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .requiredWidth(56.dp)
                .wrapContentHeight(Alignment.Top, unbounded = true),
        )
    }
}

@Composable
private fun PromoText(title: TextReference?, subtitle: TextReference, onSizeChange: (Dp) -> Unit) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier.onSizeChanged {
            with(density) { onSizeChange(it.height.toDp()) }
        },
    ) {
        TextsBlock(
            title = title,
            subtitle = subtitle,
            modifier = Modifier
                .wrapContentHeight()
                .align(Alignment.CenterStart)
                .padding(start = 76.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun TextsBlock(title: TextReference?, subtitle: TextReference, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val titleText = title?.resolveReference()

        if (titleText != null) {
            Text(
                text = titleText,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.button,
            )

            SpacerH(height = TangemTheme.dimens.spacing2)
        }

        Text(
            text = subtitle.resolveAnnotatedReference(),
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.caption2,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RingPromoNotification_Preview() {
    TangemThemePreview {
        StakingInMarketsPromoNotification(
            config = NotificationConfig(
                title = stringReference("Earn up to 14% APY"),
                subtitle = stringReference("Staking is the easiest way to earn rewards on your crypto. Show more"),
                iconResId = R.drawable.img_staking_in_market_notification,
                onCloseClick = { },
            ),
        )
    }
}

// endregion