package com.tangem.core.ui.components.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.currency.tokenicon.LoadingIcon
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.ImageBackgroundContrastChecker
import kotlinx.coroutines.launch

@Composable
internal inline fun DefaultCurrencyIcon(
    iconData: Any,
    alpha: Float,
    colorFilter: ColorFilter?,
    crossinline errorIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var iconBackgroundColor by remember { mutableStateOf(Color.Transparent) }
    val itemBackgroundColor = TangemTheme.colors.background.primary.toArgb()
    val isDarkTheme = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    SubcomposeAsyncImage(
        modifier = modifier
            .background(
                color = iconBackgroundColor,
                shape = TangemTheme.shapes.roundedCorners8,
            ),
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(iconData)
            .crossfade(enable = true)
            .allowHardware(false)
            .listener(
                onSuccess = { _, result ->
                    if (isDarkTheme) {
                        coroutineScope.launch {
                            val color = ImageBackgroundContrastChecker(
                                drawable = result.drawable,
                                backgroundColor = itemBackgroundColor,
                            ).getContrastColorIfNeeded(isDarkTheme)
                            iconBackgroundColor = color
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