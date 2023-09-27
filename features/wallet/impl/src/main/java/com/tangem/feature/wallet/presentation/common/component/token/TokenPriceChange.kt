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
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemTypography
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun TokenPriceChange(state: TokenItemState, modifier: Modifier = Modifier) {
    AnimatedContent(targetState = state, label = "Update the price change", modifier = modifier) { animatedState ->
        when (animatedState) {
            is TokenItemState.Content -> {
                PriceChangeBlock(state = animatedState.tokenOptions.priceChangeState)
            }
            is TokenItemState.Loading -> {
                RectangleShimmer(modifier = Modifier.placeholderSize(), radius = TangemTheme.dimens.radius4)
            }
            is TokenItemState.Locked -> {
                LockedRectangle(modifier = Modifier.placeholderSize())
            }
            is TokenItemState.Unreachable,
            is TokenItemState.Draggable,
            is TokenItemState.NoAddress,
            -> Unit
        }
    }
}

@Composable
private fun PriceChangeBlock(state: PriceChangeState) {
    Row(horizontalArrangement = Arrangement.End) {
        PriceChangeIcon(
            type = (state as? PriceChangeState.Content)?.type,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        SpacerW4()
        PriceChangeText(state = state, modifier = Modifier.align(Alignment.CenterVertically))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PriceChangeIcon(type: PriceChangeType?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = type,
        label = "Update the price change's arrow",
        modifier = modifier,
    ) { animatedType ->
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
private fun PriceChangeText(state: PriceChangeState, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = state,
        label = "Update the price change's text",
        modifier = modifier,
    ) { animatedState ->
        Text(
            text = when (animatedState) {
                is PriceChangeState.Content -> animatedState.valueInPercent
                is PriceChangeState.Unknown -> TokenItemState.UNKNOWN_AMOUNT_SIGN
            },
            color = when (animatedState) {
                is PriceChangeState.Content -> {
                    when (animatedState.type) {
                        PriceChangeType.UP -> TangemTheme.colors.text.accent
                        PriceChangeType.DOWN -> TangemTheme.colors.text.warning
                    }
                }
                PriceChangeState.Unknown -> TangemTheme.colors.text.primary1
            },
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TangemTypography.body2,
        )
    }
}

private fun Modifier.placeholderSize(): Modifier = composed {
    return@composed this
        .padding(vertical = TangemTheme.dimens.spacing4)
        .size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12)
}