package com.tangem.core.ui.components.currency.fiaticon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.DefaultCurrencyIcon

private const val GRAY_SCALE_SATURATION = 0f
private const val GRAY_SCALE_ALPHA = 0.4f

/**
 * Simple icon from network
 *
 * @param url link to icon
 * @param fallbackResId fallback icon
 * @param modifier component modifier
 */
@Composable
fun FiatIcon(
    url: String?,
    size: Dp,
    isGrayscale: Boolean,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackResId: Int = R.drawable.ic_shape_circle,
) {
    val iconData: Any = if (url.isNullOrBlank()) fallbackResId else url
    val alpha = if (isGrayscale) GRAY_SCALE_ALPHA else 1f
    val colorFilter = if (isGrayscale) GrayscaleColorFilter else null

    DefaultCurrencyIcon(
        iconData = iconData,
        size = size,
        alpha = alpha,
        colorFilter = colorFilter,
        errorIcon = {
            Image(
                painter = painterResource(id = fallbackResId),
                contentDescription = null,
            )
        },
        modifier = modifier,
    )
}

private val GrayscaleColorFilter: ColorFilter
    get() = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(GRAY_SCALE_SATURATION) })
