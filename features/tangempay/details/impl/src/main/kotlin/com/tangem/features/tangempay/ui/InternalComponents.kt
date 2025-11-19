package com.tangem.features.tangempay.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil.compose.rememberAsyncImagePainter
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun RemoteIcon(url: String, modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier.clip(CircleShape),
        painter = rememberAsyncImagePainter(url),
        contentDescription = null,
        tint = Color.Unspecified,
    )
}

@Composable
internal fun LocalStaticIcon(@DrawableRes id: Int, iconSize: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.icon.secondary.copy(alpha = 0.1F),
            shape = CircleShape,
        ),
    ) {
        Icon(
            painter = painterResource(id),
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.Center),
            tint = TangemTheme.colors.icon.informative,
        )
    }
}