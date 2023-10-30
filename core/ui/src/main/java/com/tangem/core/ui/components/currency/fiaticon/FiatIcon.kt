package com.tangem.core.ui.components.currency.fiaticon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.DefaultCurrencyIcon

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
    modifier: Modifier = Modifier,
    @DrawableRes fallbackResId: Int = R.drawable.ic_shape_circle,
) {
    val iconData: Any = if (url.isNullOrBlank()) fallbackResId else url

    DefaultCurrencyIcon(
        modifier = modifier,
        iconData = iconData,
        errorIcon = {
            Image(
                painter = painterResource(id = fallbackResId),
                contentDescription = null,
            )
        },
        alpha = 1f,
        colorFilter = null,
    )
}