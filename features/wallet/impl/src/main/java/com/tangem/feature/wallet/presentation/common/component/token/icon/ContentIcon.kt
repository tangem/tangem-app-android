package com.tangem.feature.wallet.presentation.common.component.token.icon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState

@Composable
internal fun ContentIcon(icon: TokenItemState.IconState, colorFilter: ColorFilter?, modifier: Modifier = Modifier) {
    when (icon) {
        is TokenItemState.IconState.CoinIcon -> CoinIcon(
            modifier = modifier,
            url = icon.url,
            fallbackResId = icon.fallbackResId,
            colorFilter = colorFilter,
        )
        is TokenItemState.IconState.TokenIcon -> TokenIcon(
            modifier = modifier,
            url = icon.url,
            colorFilter = colorFilter,
            errorIcon = {
                CustomTokenIcon(
                    modifier = modifier,
                    tint = icon.fallbackTint,
                    background = icon.fallbackBackground,
                )
            },
        )
        is TokenItemState.IconState.CustomTokenIcon -> CustomTokenIcon(
            modifier = modifier,
            tint = icon.tint,
            background = icon.background,
        )
    }
}

@Composable
private fun CoinIcon(
    url: String?,
    @DrawableRes fallbackResId: Int,
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier,
) {
    val iconData: Any = if (url.isNullOrBlank()) fallbackResId else url

    DefaultCurrencyIcon(
        modifier = modifier,
        iconData = iconData,
        errorIcon = {
            Image(
                painter = painterResource(id = fallbackResId),
                colorFilter = colorFilter,
                contentDescription = null,
            )
        },
        colorFilter = colorFilter,
    )
}

@Composable
private fun TokenIcon(
    url: String?,
    colorFilter: ColorFilter?,
    errorIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (url == null) {
        errorIcon()
    } else {
        DefaultCurrencyIcon(
            modifier = modifier,
            iconData = url,
            errorIcon = errorIcon,
            colorFilter = colorFilter,
        )
    }
}

@Composable
private fun CustomTokenIcon(tint: Color, background: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = background,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.matchParentSize(),
            painter = painterResource(id = R.drawable.ic_custom_token_44),
            tint = tint,
            contentDescription = null,
        )
    }
}

@Composable
private inline fun DefaultCurrencyIcon(
    iconData: Any,
    colorFilter: ColorFilter?,
    crossinline errorIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(iconData)
            .crossfade(enable = true)
            .build(),
        loading = { LoadingIcon() },
        error = { errorIcon() },
        colorFilter = colorFilter,
        contentDescription = null,
    )
}