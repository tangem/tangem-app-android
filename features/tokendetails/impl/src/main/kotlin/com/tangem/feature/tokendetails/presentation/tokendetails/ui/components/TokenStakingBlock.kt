package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.state.IconState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlockState

private const val GRAY_SCALE_SATURATION = 0f
private const val GRAY_SCALE_ALPHA = 0.4f
private const val NORMAL_ALPHA = 1f
private val GrayscaleColorFilter: ColorFilter
    get() = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(GRAY_SCALE_SATURATION) })

/**
 * Token staking block
 *
 * @param state    component state
 * @param modifier modifier
 */
@Composable
fun TokenStakingBlock(state: StakingBlockState, modifier: Modifier = Modifier) {
    var rootWidth by remember { mutableIntStateOf(value = 0) }

    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size72)
            .padding(all = TangemTheme.dimens.spacing12)
            .onSizeChanged { rootWidth = it.width },
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        horizontalAlignment = Alignment.Start,
    ) {
        Content(state = state, iconState = iconState)
    }
}

@Composable
private fun Content(state: StakingBlockState, iconState: IconState, modifier: Modifier = Modifier) {
    AnimatedContent(
        modifier = modifier.heightIn(min = TangemTheme.dimens.size20),
        targetState = state,
        contentAlignment = Alignment.CenterStart,
        label = "Update the content",
    ) { stakingBlockState ->
        when (stakingBlockState) {
            is StakingBlockState.Content,
            is StakingBlockState.Loading,
            -> {
                StakingContent(
                    stakingBlockState = stakingBlockState,
                    iconState = iconState,
                )
            }
            is StakingBlockState.Error -> Row {} // TODO staking
        }
    }
}

@Composable
private fun StakingContent(stakingBlockState: StakingBlockState, iconState: IconState) {
    AnimatedContent(targetState = stakingBlockState, label = "Update staking block") { state ->
        Row {
            val (alpha, colorFilter) = remember(iconState.isGrayscale) {
                if (iconState.isGrayscale) {
                    GRAY_SCALE_ALPHA to GrayscaleColorFilter
                } else {
                    NORMAL_ALPHA to null
                }
            }
            CurrencyIcon(
                modifier = Modifier
                    .size(TangemTheme.dimens.size30)
                    .padding(end = TangemTheme.dimens.spacing8)
                    .clip(TangemTheme.shapes.roundedCorners8)
                    .align(Alignment.CenterVertically),
                icon = iconState,
                alpha = alpha,
                colorFilter = colorFilter,
            )
            Column {
                if (state is StakingBlockState.Content) {
                    Text(
                        text = "Earn up to ${state.percent} staking rewards yearly",
                        color = TangemTheme.colors.text.primary1,
                        style = TangemTheme.typography.subtitle2,
                    )
                } else {
                    RectangleShimmer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TangemTheme.dimens.size16),
                    )
                }

                Spacer(modifier = Modifier.size(TangemTheme.dimens.size8))

                if (state is StakingBlockState.Content) {
                    Text(
                        text = "Staking allows you to earn SOL and get rewards every ~${state.periodInDays} days",
                        color = TangemTheme.colors.text.tertiary,
                        style = TangemTheme.typography.body2,
                    )
                } else {
                    RectangleShimmer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TangemTheme.dimens.size16),
                    )
                }

                Spacer(modifier = Modifier.size(TangemTheme.dimens.size8))

                if (state is StakingBlockState.Content) {
                    SecondaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Stake",
                        onClick = { /* TODO staking AND-7134 */ },
                    )
                }
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokenStakingBlock(
    @PreviewParameter(StakingBlockStateProvider::class)
    state: StakingBlockState,
) {
    TangemThemePreview {
        TokenStakingBlock(state = state)
    }
}

private class StakingBlockStateProvider : CollectionPreviewParameterProvider<StakingBlockState>(
    collection = listOf(
        StakingBlockState.Content(
            iconState = iconState,
            percent = "10",
            periodInDays = 4,
            tokenSymbol = "SOL",
        ),
        StakingBlockState.Loading(iconState = iconState),
        StakingBlockState.Error(iconState = iconState),
    ),
)

private val iconState = IconState.TokenIcon(
    url = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
    fallbackTint = Color.Cyan,
    fallbackBackground = Color.Blue,
    isGrayscale = false,
)
// endregion Preview
