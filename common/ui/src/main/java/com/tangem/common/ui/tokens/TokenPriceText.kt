package com.tangem.common.ui.tokens

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme
import java.math.BigDecimal

/**
 * Text view for token price.
 *
 * @param priceValue Numeric price; drives the change animation (used as the LaunchedEffect key).
 * @param priceAnnotated Styled price (locale-aware decimal separator + secondary fractional color).
 * @param priceChangeType Type of the price change.
 */
@Composable
fun TokenPriceText(
    priceValue: BigDecimal,
    priceAnnotated: TextReference,
    modifier: Modifier = Modifier,
    priceChangeType: PriceChangeType? = null,
) {
    val growColor = TangemTheme.colors2.text.status.accent
    val fallColor = TangemTheme.colors2.text.status.warning
    val generalColor = TangemTheme.colors2.text.neutral.primary

    val color = remember(generalColor) { Animatable(generalColor) }
    var isAnimationSkipped by remember { mutableStateOf(false) }

    LaunchedEffect(priceValue) {
        if (!isAnimationSkipped) {
            isAnimationSkipped = true
            return@LaunchedEffect
        }

        if (priceChangeType != null) {
            val nextColor = when (priceChangeType) {
                PriceChangeType.UP -> growColor
                PriceChangeType.DOWN -> fallColor
                PriceChangeType.NEUTRAL -> return@LaunchedEffect
            }

            color.animateTo(nextColor, snap())
            color.animateTo(generalColor, tween(durationMillis = 500))
        }
    }

    Text(
        modifier = modifier,
        text = priceAnnotated.resolveAnnotatedReference(),
        color = color.value,
        maxLines = 1,
        style = TangemTheme.typography2.bodySemibold16,
        overflow = TextOverflow.Visible,
    )
}