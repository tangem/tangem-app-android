package com.tangem.feature.wallet.presentation.common.component.token

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.tangem.core.ui.components.marketprice.PriceChangeConfig
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
                PriceChangeBlock(config = animatedState.tokenOptions.config)
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
private fun PriceChangeBlock(config: PriceChangeConfig) {
    Row(horizontalArrangement = Arrangement.End) {
        PriceChangeIcon(
            type = config.type,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        SpacerW4()
        PriceChangeText(config = config, modifier = Modifier.align(Alignment.CenterVertically))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PriceChangeIcon(type: PriceChangeConfig.Type, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = type,
        label = "Update the price change's arrow",
        modifier = modifier,
    ) { animatedType ->
        Icon(
            painter = painterResource(
                id = when (animatedType) {
                    PriceChangeConfig.Type.UP -> R.drawable.ic_arrow_up_8
                    PriceChangeConfig.Type.DOWN -> R.drawable.ic_arrow_down_8
                },
            ),
            tint = when (animatedType) {
                PriceChangeConfig.Type.UP -> TangemTheme.colors.icon.accent
                PriceChangeConfig.Type.DOWN -> TangemTheme.colors.icon.warning
            },
            contentDescription = null,
        )
    }
}

@Composable
private fun PriceChangeText(config: PriceChangeConfig, modifier: Modifier = Modifier) {
    Text(
        text = config.valueInPercent,
        modifier = modifier,
        color = when (config.type) {
            PriceChangeConfig.Type.UP -> TangemTheme.colors.text.accent
            PriceChangeConfig.Type.DOWN -> TangemTheme.colors.text.warning
        },
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = TangemTypography.body2,
    )
}

private fun Modifier.placeholderSize(): Modifier = composed {
    return@composed this
        .padding(vertical = TangemTheme.dimens.spacing4)
        .size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12)
}