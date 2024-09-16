package com.tangem.common.ui.tokens

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.res.TangemTheme

/**
 * Text view for token price.
 *
 * @param price Price of the token.
 * @param priceChangeType Type of the price change.
 */
@Composable
fun TokenPriceText(price: String, modifier: Modifier = Modifier, priceChangeType: PriceChangeType? = null) {
    val growColor = TangemTheme.colors.text.accent
    val fallColor = TangemTheme.colors.text.warning
    val generalColor = TangemTheme.colors.text.primary1

    val color = remember(generalColor) { Animatable(generalColor) }
    var animationSkipped by remember { mutableStateOf(false) }

    LaunchedEffect(price) {
        if (animationSkipped.not()) {
            animationSkipped = true
            return@LaunchedEffect
        }

        if (priceChangeType != null) {
            val nextColor = when (priceChangeType) {
                PriceChangeType.UP,
                -> growColor
                PriceChangeType.DOWN -> fallColor
                PriceChangeType.NEUTRAL -> return@LaunchedEffect
            }

            color.animateTo(nextColor, snap())
            color.animateTo(generalColor, tween(durationMillis = 500))
        }
    }

    Text(
        modifier = modifier,
        text = price,
        color = color.value,
        maxLines = 1,
        style = TangemTheme.typography.body2,
        overflow = TextOverflow.Visible,
    )
}