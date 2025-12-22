package com.tangem.core.ui.components.icons

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
import com.tangem.core.ui.res.TangemTheme

@Composable
fun HighlightedIcon(
    @DrawableRes icon: Int,
    iconTint: Color,
    modifier: Modifier = Modifier,
    backgroundColor: Color = iconTint,
) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size56)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.1F)),
        contentAlignment = Alignment.Center,
        content = {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size32),
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
            )
        },
    )
}