package com.tangem.feature.wallet.presentation.common.component.token

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.common.state.TokenItemState.PriceChangeState as TokenPriceChangeState

@Composable
internal fun TokenPriceChange(state: TokenPriceChangeState?, modifier: Modifier = Modifier) {
    when (state) {
        is TokenPriceChangeState.Content -> {
            PriceChangeBlock(modifier = modifier, type = state.type, text = state.valueInPercent)
        }
        is TokenPriceChangeState.Unknown -> {
            PriceChangeBlock(modifier = modifier)
        }
        is TokenPriceChangeState.Loading -> {
            RectangleShimmer(modifier = modifier.placeholderSize(), radius = TangemTheme.dimens.radius4)
        }
        is TokenPriceChangeState.Locked -> {
            LockedRectangle(modifier = modifier.placeholderSize())
        }
        null -> Unit
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PriceChangeIcon(type: PriceChangeType?) {
    AnimatedContent(targetState = type, label = "Update the price change's arrow") { animatedType ->
        animatedType ?: return@AnimatedContent

        Icon(
            painter = painterResource(
                id = when (animatedType) {
                    PriceChangeType.UP -> R.drawable.ic_arrow_up_8
                    PriceChangeType.DOWN -> R.drawable.ic_arrow_down_8
                },
            ),
            tint = when (animatedType) {
                PriceChangeType.UP -> TangemTheme.colors.icon.accent
                PriceChangeType.DOWN -> TangemTheme.colors.icon.warning
            },
            contentDescription = null,
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PriceChangeText(type: PriceChangeType?, text: String?) {
    AnimatedContent(targetState = text, label = "Update the price change's text") { animatedText ->
        Text(
            text = animatedText ?: TokenItemState.UNKNOWN_AMOUNT_SIGN,
            color = when (type) {
                PriceChangeType.UP -> TangemTheme.colors.text.accent
                PriceChangeType.DOWN -> TangemTheme.colors.text.warning
                null -> TangemTheme.colors.text.primary1
            },
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TangemTheme.typography.body2,
        )
    }
}

private fun Modifier.placeholderSize(): Modifier = composed {
    return@composed this
        .padding(vertical = TangemTheme.dimens.spacing3)
        .size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12)
}