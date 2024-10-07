package com.tangem.core.ui.components.notifications

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme

@Composable
fun RingPromoNotification(config: NotificationConfig, modifier: Modifier = Modifier) {
    NotificationBaseContainer(
        backgroundResId = requireNotNull(config.backgroundResId),
        onCloseClick = config.onCloseClick,
        modifier = modifier,
    ) {
        MainContent(title = config.title, subtitle = config.subtitle)
    }
}

@Composable
private fun NotificationBaseContainer(
    @DrawableRes backgroundResId: Int,
    onCloseClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .requiredHeight(height = 78.dp)
            .fillMaxWidth()
            .clip(shape = TangemTheme.shapes.roundedCornersXMedium),
    ) {
        Image(
            painter = painterResource(backgroundResId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        content()

        CloseableIconButton(
            onClick = onCloseClick,
            modifier = Modifier.align(alignment = Alignment.TopEnd),
            iconTint = TangemTheme.colors.icon.informative,
        )
    }
}

@Composable
private fun MainContent(title: TextReference?, subtitle: TextReference) {
    Box {
        Image(
            painter = painterResource(id = R.drawable.img_card_with_ring),
            contentDescription = null,
            alignment = Alignment.TopStart,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(start = 9.dp, top = 11.dp)
                .requiredWidth(width = 60.dp),
        )

        TextsBlock(
            title = title,
            subtitle = subtitle,
            subtitleColor = TangemTheme.colors.text.tertiary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 80.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        )
    }
}