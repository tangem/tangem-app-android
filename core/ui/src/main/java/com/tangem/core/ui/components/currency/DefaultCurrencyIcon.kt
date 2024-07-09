package com.tangem.core.ui.components.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.currency.icon.LoadingIcon
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.ImageBackgroundContrastChecker
import kotlinx.coroutines.launch

@Composable
internal inline fun DefaultCurrencyIcon(
    iconData: Any,
    size: Dp,
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

    val pixelsSize = with(LocalDensity.current) { size.roundToPx() }
    SubcomposeAsyncImage(
        modifier = modifier
            .background(
                color = iconBackgroundColor,
                shape = TangemTheme.shapes.roundedCorners8,
            )
            .clip(TangemTheme.shapes.roundedCorners8),
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(iconData)
            .size(size = pixelsSize)
            .memoryCacheKey(key = iconData.toString() + pixelsSize)
            .crossfade(enable = true)
            .allowHardware(enable = false)
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
        loading = { LoadingIcon() },
        error = { errorIcon() },
        alpha = alpha,
        colorFilter = colorFilter,
        contentDescription = null,
    )
}
