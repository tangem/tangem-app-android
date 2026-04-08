package com.tangem.features.feed.components.market.details.portfolioblock.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.components.market.details.portfolioblock.ui.state.PortfolioBlockUM
import com.tangem.features.feed.impl.R

@Composable
internal fun PortfolioBlock(state: PortfolioBlockUM, modifier: Modifier = Modifier) {
    val isVisible = state is PortfolioBlockUM.AddToken || state is PortfolioBlockUM.Content
    val screenHeightPx = with(LocalDensity.current) { LocalWindowSize.current.height.toPx() }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        ) {
            BottomFade(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            )
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300)),
        ) {
            val slideOffset by transition.animateFloat(
                transitionSpec = {
                    tween(
                        durationMillis = 600,
                        easing = CubicBezierEasing(a = 0.83f, b = 0f, c = 0.17f, d = 1f),
                    )
                },
                label = "slideOffset",
            ) { enterExitState ->
                when (enterExitState) {
                    EnterExitState.PreEnter -> screenHeightPx
                    EnterExitState.Visible -> 0f
                    EnterExitState.PostExit -> 0f
                }
            }

            Box(modifier = Modifier.graphicsLayer { translationY = slideOffset }) {
                when (state) {
                    is PortfolioBlockUM.AddToken -> AddTokenBlock(state)
                    is PortfolioBlockUM.Content -> ContentBlock(state)
                    is PortfolioBlockUM.Hidden,
                    is PortfolioBlockUM.Loading,
                    -> Unit
                }
            }
        }
    }
}

@Composable
private fun ContentBlock(state: PortfolioBlockUM.Content, modifier: Modifier = Modifier) {
    FloatingCard(
        modifier = modifier,
        onClick = state.onClick,
    ) {
        TangemRowContainer(
            contentPadding = PaddingValues(0.dp),
        ) {
            CurrencyIcon(
                modifier = Modifier
                    .padding(end = TangemTheme.dimens2.x3)
                    .layoutId(TangemRowLayoutId.HEAD),
                state = state.tokenIcon,
            )

            Text(
                modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
                text = state.tokenName,
                style = TangemTheme.typography2.bodyMedium16,
                color = TangemTheme.colors2.text.neutral.primary,
                maxLines = 1,
            )

            Text(
                modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
                text = stringResourceSafe(R.string.markets_portfolio_block_subtitle),
                style = TangemTheme.typography2.captionMedium12,
                color = TangemTheme.colors2.text.neutral.secondary,
                maxLines = 1,
            )

            Text(
                modifier = Modifier.layoutId(TangemRowLayoutId.END_TOP),
                text = state.totalBalance.orMaskWithStars(state.isBalanceHidden).resolveAnnotatedReference(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TangemTheme.colors2.text.neutral.primary,
                style = TangemTheme.typography2.bodyMedium16,
            )

            Text(
                modifier = Modifier.layoutId(TangemRowLayoutId.END_BOTTOM),
                text = state.tokenSymbol,
                style = TangemTheme.typography2.captionMedium12,
                color = TangemTheme.colors2.text.neutral.secondary,
                maxLines = 1,
            )

            TangemButton(
                modifier = Modifier
                    .padding(start = TangemTheme.dimens2.x3)
                    .layoutId(TangemRowLayoutId.TAIL),
                buttonUM = TangemButtonUM(
                    type = TangemButtonType.Secondary,
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_chevron_24,
                        tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                    ),
                    shape = TangemButtonShape.Rounded,
                    size = TangemButtonSize.X9,
                    onClick = state.onClick,
                ),
            )
        }
    }
}

@Composable
private fun AddTokenBlock(state: PortfolioBlockUM.AddToken, modifier: Modifier = Modifier) {
    FloatingCard(
        modifier = modifier,
        onClick = state.onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CurrencyIcon(state.tokenIcon)

            SpacerW(TangemTheme.dimens2.x3)

            Text(
                text = formatAnnotatedWithBoldColor(
                    rawString = stringResourceSafe(R.string.markets_portfolio_block_add_token_title),
                    boldColor = TangemTheme.colors2.text.neutral.primary,
                ),
                style = TangemTheme.typography2.captionMedium12,
                color = TangemTheme.colors2.text.neutral.secondary,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )

            SpacerW(TangemTheme.dimens2.x2)

            TangemButton(
                buttonUM = TangemButtonUM(
                    type = TangemButtonType.Secondary,
                    text = resourceReference(R.string.common_add),
                    shape = TangemButtonShape.Rounded,
                    size = TangemButtonSize.X9,
                    onClick = state.onClick,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloatingCard(modifier: Modifier = Modifier, onClick: () -> Unit = {}, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .widthIn(max = BottomSheetDefaults.SheetMaxWidth)
            .padding(
                start = TangemTheme.dimens2.x2,
                end = TangemTheme.dimens2.x2,
                bottom = TangemTheme.dimens2.x2,
            )
            .clip(RoundedCornerShape(size = TangemTheme.dimens2.x5))
            .background(
                color = TangemTheme.colors2.surface.level3,
                shape = RoundedCornerShape(size = TangemTheme.dimens2.x5),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x3),
    ) {
        content()
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreview() {
    TangemThemePreviewRedesign {
        PortfolioBlock(
            state = PortfolioBlockUM.Content(
                totalBalance = stringReference("$1,234.56"),
                tokensInPortfolioCount = 3,
                tokenIcon = previewCoinIcon,
                tokenName = "Bitcoin",
                tokenSymbol = "BTC",
                isBalanceHidden = false,
                onClick = {},
            ),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddTokenPreview() {
    TangemThemePreviewRedesign {
        PortfolioBlock(
            state = PortfolioBlockUM.AddToken(
                tokenIcon = previewCoinIcon,
                onClick = {},
            ),
        )
    }
}

private val previewCoinIcon
    get() = CurrencyIconState.CoinIcon(
        url = null,
        fallbackResId = com.tangem.core.ui.R.drawable.img_polygon_22,
        isGrayscale = false,
        shouldShowCustomBadge = false,
    )