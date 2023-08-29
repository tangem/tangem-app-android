package com.tangem.feature.wallet.presentation.common.component.token

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.marketprice.PriceChangeConfig
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemTypography
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.common.state.TokenItemState.Companion.DOTS
import com.tangem.feature.wallet.presentation.common.state.TokenItemState.TokenOptionsState
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder

@Composable
internal fun TokenFiatInfoBlock(
    state: TokenItemState,
    modifier: Modifier = Modifier,
    reorderableTokenListState: ReorderableLazyListState? = null,
) {
    when (state) {
        is TokenItemState.Content -> ContentBlock(state = state.tokenOptions, modifier = modifier)
        is TokenItemState.Draggable -> {
            DraggableBlock(
                modifier = modifier,
                reorderableTokenListState = reorderableTokenListState,
            )
        }
        is TokenItemState.Unreachable -> UnreachableBlock(modifier = modifier)
        is TokenItemState.Loading -> LoadingBlock(modifier = modifier)
        is TokenItemState.Locked -> LockedBlock(modifier = modifier)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ContentBlock(state: TokenOptionsState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.requiredWidth(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
    ) {
        AnimatedContent(
            targetState = state,
            label = "Update the fiat percentage block",
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(
                text = when (it) {
                    is TokenOptionsState.Visible -> it.fiatAmount
                    is TokenOptionsState.Hidden -> DOTS
                },
                style = TangemTypography.body2,
                color = TangemTheme.colors.text.primary1,
            )
        }

        PriceChangeBlock(config = state.config)
    }
}

@Composable
private fun PriceChangeBlock(config: PriceChangeConfig) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        PriceChangeIcon(type = config.type, modifier = Modifier.align(Alignment.CenterVertically))
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
    ) {
        Image(
            painter = painterResource(
                id = when (it) {
                    PriceChangeConfig.Type.UP -> R.drawable.img_arrow_up_8
                    PriceChangeConfig.Type.DOWN -> R.drawable.img_arrow_down_8
                },
            ),
            contentDescription = null,
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PriceChangeText(config: PriceChangeConfig, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = config.type,
        label = "Update the price change's arrow",
        modifier = modifier,
    ) {
        Text(
            text = config.valueInPercent,
            style = TangemTypography.body2,
            color = when (it) {
                PriceChangeConfig.Type.UP -> TangemTheme.colors.text.accent
                PriceChangeConfig.Type.DOWN -> TangemTheme.colors.text.warning
            },
        )
    }
}

@Composable
private fun DraggableBlock(reorderableTokenListState: ReorderableLazyListState?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size32)
            .then(
                other = if (reorderableTokenListState != null) {
                    Modifier.detectReorder(reorderableTokenListState)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_drag_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
    }
}

@Composable
private fun UnreachableBlock(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(id = R.string.common_unreachable),
        style = TangemTypography.body2,
        color = TangemTheme.colors.text.tertiary,
    )
}

@Composable
private fun LoadingBlock(modifier: Modifier = Modifier) {
    NonContentContainer(modifier = modifier) {
        RectangleShimmer(modifier = Modifier.viewSize(), radius = TangemTheme.dimens.radius4)
        RectangleShimmer(modifier = Modifier.viewSize(), radius = TangemTheme.dimens.radius4)
    }
}

@Composable
private fun LockedBlock(modifier: Modifier = Modifier) {
    NonContentContainer(modifier = modifier) {
        LockedRectangle(modifier = Modifier.viewSize())
        LockedRectangle(modifier = Modifier.viewSize())
    }
}

@Composable
private fun NonContentContainer(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing10),
        content = content,
    )
}

private fun Modifier.viewSize(): Modifier = composed {
    return@composed size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12)
}