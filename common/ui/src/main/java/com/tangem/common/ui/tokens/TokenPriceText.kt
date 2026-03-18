package com.tangem.common.ui.tokens

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme

/**
 * Text view for token price.
 *
 * @param price Price of the token.
 * @param priceChangeType Type of the price change.
 */
@Composable
fun TokenPriceText(price: String, modifier: Modifier = Modifier, priceChangeType: PriceChangeType? = null) {
    if (LocalRedesignEnabled.current) {
        TokenPriceTextV2(
            price = price,
            modifier = modifier,
            priceChangeType = priceChangeType,
        )
    } else {
        TokenPriceTextV1(
            price = price,
            modifier = modifier,
            priceChangeType = priceChangeType,
        )
    }
}

@Composable
private fun TokenPriceTextV1(price: String, modifier: Modifier = Modifier, priceChangeType: PriceChangeType? = null) {
    val growColor = TangemTheme.colors.text.accent
    val fallColor = TangemTheme.colors.text.warning
    val generalColor = TangemTheme.colors.text.primary1

    val color = remember(generalColor) { Animatable(generalColor) }
    var isAnimationSkipped by remember { mutableStateOf(false) }

    LaunchedEffect(price) {
        if (isAnimationSkipped.not()) {
            isAnimationSkipped = true
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

@Composable
private fun TokenPriceTextV2(price: String, modifier: Modifier = Modifier, priceChangeType: PriceChangeType? = null) {
    val growColor = TangemTheme.colors2.text.status.accent
    val fallColor = TangemTheme.colors2.text.status.warning
    val generalColor = TangemTheme.colors2.text.neutral.primary
    val decimalColor = TangemTheme.colors2.text.neutral.secondary

    val color = remember(generalColor) { Animatable(generalColor) }
    var isAnimationSkipped by remember { mutableStateOf(false) }

    LaunchedEffect(price) {
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

    val annotatedText = remember(price) {
        buildAnnotatedString {
            val dotIndex = price.indexOf(".")

            if (dotIndex == -1) {
                append(price)
            } else {
                append(price.take(dotIndex))

                withStyle(
                    style = SpanStyle(color = decimalColor),
                ) {
                    append(price.substring(dotIndex))
                }
            }
        }
    }

    Text(
        modifier = modifier,
        text = annotatedText,
        color = color.value,
        maxLines = 1,
        style = TangemTheme.typography2.bodySemibold16,
        overflow = TextOverflow.Visible,
    )
}