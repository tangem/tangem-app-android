package com.tangem.managetokens.presentation.managetokens.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.managetokens.state.QuotesState

@Composable
internal fun TokenPriceChange(state: QuotesState, modifier: Modifier = Modifier) {
    when (state) {
        is QuotesState.Content ->
            PriceChangeBlock(modifier = modifier, type = state.changeType, text = state.priceChange)
        QuotesState.Unknown -> PriceChangeBlock(modifier = modifier)
    }
}

@Composable
private fun PriceChangeBlock(modifier: Modifier = Modifier, type: PriceChangeType? = null, text: String? = null) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PriceChangeIcon(type = type)
        SpacerW4()
        PriceChangeText(type = type, text = text)
    }
}

@Composable
private fun PriceChangeIcon(type: PriceChangeType?) {
    AnimatedContent(targetState = type, label = "Update the price change's arrow") { animatedType ->
        animatedType ?: return@AnimatedContent

        Icon(
            painter = painterResource(
                id = when (animatedType) {
                    PriceChangeType.UP -> R.drawable.ic_arrow_up_8
                    PriceChangeType.DOWN -> R.drawable.ic_arrow_down_8
                    PriceChangeType.NEUTRAL -> R.drawable.ic_elipse_8
                },
            ),
            tint = when (animatedType) {
                PriceChangeType.UP -> TangemTheme.colors.icon.accent
                PriceChangeType.DOWN -> TangemTheme.colors.icon.warning
                PriceChangeType.NEUTRAL -> TangemTheme.colors.icon.inactive
            },
            contentDescription = null,
        )
    }
}

@Composable
private fun PriceChangeText(type: PriceChangeType?, text: String?) {
    AnimatedContent(targetState = text, label = "Update the price change's text") { animatedText ->
        animatedText ?: return@AnimatedContent

        Text(
            text = animatedText,
            color = when (type) {
                PriceChangeType.UP -> TangemTheme.colors.text.accent
                PriceChangeType.DOWN -> TangemTheme.colors.text.warning
                PriceChangeType.NEUTRAL -> TangemTheme.colors.text.disabled
                null -> TangemTheme.colors.text.primary1
            },
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TangemTheme.typography.body2,
        )
    }
}