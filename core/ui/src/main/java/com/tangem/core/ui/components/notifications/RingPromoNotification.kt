package com.tangem.core.ui.components.notifications

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun RingPromoNotification(config: NotificationConfig, modifier: Modifier = Modifier) {
    var textHeightDp by remember { mutableStateOf(0.dp) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemColorPalette.Dark6),
    ) {
        PromoImage(
            backgroundRes = config.backgroundResId,
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
            iconTint = TangemTheme.colors.icon.informative,
        )
    }
}

@Composable
private fun PromoImage(@DrawableRes backgroundRes: Int?, @DrawableRes iconRes: Int, modifier: Modifier = Modifier) {
    if (backgroundRes == null) return
    ConstraintLayout(
        modifier = modifier,
    ) {
        val (background, image) = createRefs()

        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            contentScale = FixedScale(2f),
            modifier = Modifier
                .constrainAs(background) {
                    top.linkTo(parent.top)
                    start.linkTo(image.start)
                    end.linkTo(image.end)
                },
        )
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .requiredWidth(width = 60.dp)
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .constrainAs(image) {
                    top.linkTo(parent.top, 11.dp)
                    start.linkTo(parent.start, 9.dp)
                },
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
            subtitleColor = TangemTheme.colors.text.tertiary,
            titleColor = TangemTheme.colors.text.constantWhite,
            modifier = Modifier
                .wrapContentHeight()
                .align(Alignment.CenterStart)
                .padding(start = 80.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, locale = "fr")
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RingPromoNotification_Preview() {
    TangemThemePreview {
        RingPromoNotification(
            NotificationConfig(
                title = resourceReference(id = R.string.ring_promo_title),
                subtitle = resourceReference(id = R.string.ring_promo_text),
                iconResId = R.drawable.img_card_with_ring,
                backgroundResId = R.drawable.img_ring_promo_background,
                onCloseClick = { },
            ),
        )
    }
}

// endregion
