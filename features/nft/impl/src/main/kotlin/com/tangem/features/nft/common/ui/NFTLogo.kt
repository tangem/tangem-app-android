package com.tangem.features.nft.common.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.currency.icon.CurrencyIconTopBadge
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun NFTLogo(imageUrl: String?, @DrawableRes networkIconId: Int, background: Color = Color.Transparent) {
    val networkBadgeOffset = TangemTheme.dimens.spacing6

    Box(
        modifier = Modifier,
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(TangemTheme.dimens.size36)
                .clip(TangemTheme.shapes.roundedCorners8),
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            loading = {
                RectangleShimmer(radius = TangemTheme.dimens.radius8)
            },
            error = {
                Box(
                    modifier = Modifier
                        .clip(shape = TangemTheme.shapes.roundedCorners8)
                        .background(TangemTheme.colors.field.primary),
                )
            },
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
        CurrencyIconTopBadge(
            modifier = Modifier
                .offset(x = networkBadgeOffset, y = -networkBadgeOffset)
                .align(Alignment.TopEnd),
            iconResId = networkIconId,
            alpha = 1f,
            colorFilter = null,
            background = background,
        )
    }
}