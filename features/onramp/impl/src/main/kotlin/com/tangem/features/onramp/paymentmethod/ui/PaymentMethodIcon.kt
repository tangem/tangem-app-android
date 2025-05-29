package com.tangem.features.onramp.paymentmethod.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun PaymentMethodIcon(imageUrl: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        modifier = modifier
            .size(TangemTheme.dimens.size40)
            .clip(TangemTheme.shapes.roundedCorners8)
            .background(TangemColorPalette.Light1)
            .padding(TangemTheme.dimens.spacing6),
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(imageUrl)
            .crossfade(enable = true)
            .allowHardware(false)
            .build(),
        contentDescription = null,
    )
}