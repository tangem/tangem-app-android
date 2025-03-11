package com.tangem.core.ui.components.currency.fiaticon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.DefaultCurrencyIcon
import com.tangem.core.ui.utils.GRAY_SCALE_ALPHA
import com.tangem.core.ui.utils.GrayscaleColorFilter

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
    @DrawableRes fallbackResId: Int = R.drawable.ic_shape_circle_40,
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