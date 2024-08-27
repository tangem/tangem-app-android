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

@Composable
fun TokenPriceText(
    price: String,
    modifier: Modifier = Modifier,
    priceChangeType: PriceChangeType? = null,
    skipFirstAnimation: Boolean = false,
) {
    val growColor = TangemTheme.colors.text.accent
    val fallColor = TangemTheme.colors.text.warning
    val generalColor = TangemTheme.colors.text.primary1

    val color = remember { Animatable(generalColor) }
    var animationSkipped by remember { mutableStateOf(skipFirstAnimation.not()) }

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
