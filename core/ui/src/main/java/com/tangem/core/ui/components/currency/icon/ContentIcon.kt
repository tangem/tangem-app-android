package com.tangem.core.ui.components.currency.icon

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
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.DefaultCurrencyIcon
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun ContentIcon(
    icon: CurrencyIconState,
    alpha: Float,
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier,
) {
    when (icon) {
        is CurrencyIconState.CoinIcon -> CoinIcon(
            modifier = modifier,
            url = icon.url,
            fallbackResId = icon.fallbackResId,
            alpha = alpha,
            colorFilter = colorFilter,
        )
        is CurrencyIconState.FiatIcon -> CoinIcon(
            modifier = modifier,
            url = icon.url,
            fallbackResId = icon.fallbackResId,
            alpha = alpha,
            colorFilter = colorFilter,
        )
        is CurrencyIconState.TokenIcon -> TokenIcon(
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
        is CurrencyIconState.CustomTokenIcon -> CustomTokenIcon(
            modifier = modifier,
            tint = icon.tint,
            background = icon.background,
            alpha = alpha,
        )
        CurrencyIconState.Loading,
        CurrencyIconState.Locked,
        is CurrencyIconState.Empty,
        -> Unit
    }
}

@Composable
fun CoinIcon(
    url: String?,
    @DrawableRes fallbackResId: Int,
    alpha: Float,
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier,
) {
    val iconData: Any = if (url.isNullOrBlank()) fallbackResId else url

    DefaultCurrencyIcon(
        iconData = iconData,
        size = TangemTheme.dimens.size36,
        alpha = alpha,
        colorFilter = colorFilter,
        errorIcon = {
            Image(
                painter = painterResource(id = fallbackResId),
                alpha = alpha,
                colorFilter = colorFilter,
                contentDescription = null,
            )
        },
        modifier = modifier,
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
            iconData = url,
            size = TangemTheme.dimens.size36,
            alpha = alpha,
            colorFilter = colorFilter,
            errorIcon = errorIcon,
            modifier = modifier,
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