package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.ImageBackgroundContrastChecker
import com.tangem.feature.tokendetails.presentation.tokendetails.state.IconState
import com.tangem.features.tokendetails.impl.R
import kotlinx.coroutines.launch

@Composable
internal fun CurrencyIcon(icon: IconState, alpha: Float, colorFilter: ColorFilter?, modifier: Modifier = Modifier) {
    when (icon) {
        is IconState.CoinIcon -> CoinIcon(
            modifier = modifier,
            url = icon.url,
            fallbackResId = icon.fallbackResId,
            alpha = alpha,
            colorFilter = colorFilter,
        )
        is IconState.TokenIcon -> TokenIcon(
            modifier = modifier,
            url = icon.url,
            alpha = alpha,
            colorFilter = colorFilter,
            errorIcon = {
                CustomTokenIcon(
                    modifier = modifier,
                    tint = icon.fallbackTint,
                    background = icon.fallbackBackground,
                    alpha = alpha,
                )
            },
        )
        is IconState.CustomTokenIcon -> CustomTokenIcon(
            modifier = modifier,
            tint = icon.tint,
            background = icon.background,
            alpha = alpha,
        )
    }
}

@Composable
private fun CoinIcon(
    url: String?,
    @DrawableRes fallbackResId: Int,
    alpha: Float,
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
                alpha = alpha,
                colorFilter = colorFilter,
                contentDescription = null,
            )
        },
        alpha = alpha,
        colorFilter = colorFilter,
    )
}

@Composable
private fun TokenIcon(
    url: String?,
    alpha: Float,
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
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }
}

@Composable
private fun CustomTokenIcon(tint: Color, background: Color, alpha: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = background.copy(alpha = alpha),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.matchParentSize(),
            painter = painterResource(id = R.drawable.ic_custom_token_44),
            tint = tint.copy(alpha = alpha),
            contentDescription = null,
        )
    }
}

@Composable
private inline fun DefaultCurrencyIcon(
    iconData: Any,
    alpha: Float,
    colorFilter: ColorFilter?,
    crossinline errorIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var iconBackgroundColor by remember { mutableStateOf(Color.Transparent) }
    var isBackgroundColorDefined by remember { mutableStateOf(false) }
    val itemBackgroundColor = TangemTheme.colors.background.primary.toArgb()
    val isDarkTheme = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    val pixelsSize = with(LocalDensity.current) { TangemTheme.dimens.size48.roundToPx() }
    SubcomposeAsyncImage(
        modifier = modifier
            .background(
                color = iconBackgroundColor,
                shape = TangemTheme.shapes.roundedCorners8,
            ),
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(iconData)
            .size(pixelsSize)
            .memoryCacheKey(iconData.toString() + pixelsSize)
            .crossfade(enable = true)
            .allowHardware(false)
            .listener(
                onSuccess = { _, result ->
                    if (!isBackgroundColorDefined && isDarkTheme) {
                        coroutineScope.launch {
                            val color = ImageBackgroundContrastChecker(
                                drawable = result.drawable,
                                backgroundColor = itemBackgroundColor,
                                size = pixelsSize,
                            ).getContrastColor(isDarkTheme = true)
                            iconBackgroundColor = color
                            isBackgroundColorDefined = true
                        }
                    }
                },
            ).build(),
        loading = { CircleShimmer() },
        error = { errorIcon() },
        alpha = alpha,
        colorFilter = colorFilter,
        contentDescription = null,
    )
}