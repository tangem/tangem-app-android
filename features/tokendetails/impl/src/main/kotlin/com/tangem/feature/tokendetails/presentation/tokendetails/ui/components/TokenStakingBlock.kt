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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.GRAY_SCALE_ALPHA
import com.tangem.core.ui.utils.GrayscaleColorFilter
import com.tangem.core.ui.utils.NORMAL_ALPHA
import com.tangem.feature.tokendetails.presentation.tokendetails.state.IconState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlockState
import com.tangem.features.tokendetails.impl.R

/**
 * Token staking block
 *
 * @param state    component state
 * @param modifier modifier
 */
@Composable
internal fun TokenStakingBlock(state: StakingBlockState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size72)
            .padding(all = TangemTheme.dimens.spacing12),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        horizontalAlignment = Alignment.Start,
    ) {
        Content(state = state)
    }
}

@Composable
private fun Content(state: StakingBlockState, modifier: Modifier = Modifier) {
    AnimatedContent(
        modifier = modifier.heightIn(min = TangemTheme.dimens.size60),
        targetState = state,
        contentAlignment = Alignment.CenterStart,
        label = "Update the content",
    ) { stakingBlockState ->
        when (stakingBlockState) {
            is StakingBlockState.Content -> {
                StakingContent(
                    stakingBlockState = stakingBlockState,
                    iconState = stakingBlockState.iconState,
                )
            }
            is StakingBlockState.Loading -> {
                StakingLoading(
                    iconState = stakingBlockState.iconState,
                )
            }
            is StakingBlockState.Error -> Row {} // TODO staking
        }
    }
}

@Composable
private fun StakingContent(stakingBlockState: StakingBlockState.Content, iconState: IconState) {
    Column {
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
                    .size(TangemTheme.dimens.size20)
                    .clip(TangemTheme.shapes.roundedCorners8)
                    .align(Alignment.CenterVertically),
                icon = iconState,
                alpha = alpha,
                colorFilter = colorFilter,
            )
            SpacerW8()
            Column {
                Text(
                    text = stringResource(
                        R.string.token_details_staking_block_title,
                        stakingBlockState.interestRate,
                    ),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle2,
                )

                Spacer(modifier = Modifier.size(TangemTheme.dimens.size4))

                Text(
                    text = stringResource(
                        R.string.token_details_staking_block_subtitle,
                        stakingBlockState.tokenSymbol,
                        stakingBlockState.periodInDays,
                    ),
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography.body2,
                )

                Spacer(modifier = Modifier.size(TangemTheme.dimens.size8))
            }
        }
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.common_stake),
            onClick = stakingBlockState.onStakeClicked,
        )
    }
}

@Composable
private fun StakingLoading(iconState: IconState) {
    Column {
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
                    .size(TangemTheme.dimens.size20)
                    .clip(TangemTheme.shapes.roundedCorners8)
                    .align(Alignment.CenterVertically),
                icon = iconState,
                alpha = alpha,
                colorFilter = colorFilter,
            )
            SpacerW8()
            Column {
                RectangleShimmer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TangemTheme.dimens.size20),
                )
                Spacer(modifier = Modifier.size(TangemTheme.dimens.size4))
                RectangleShimmer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TangemTheme.dimens.size20),
                )
                Spacer(modifier = Modifier.size(TangemTheme.dimens.size8))
            }
        }
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Loading", // TODO staking
            onClick = { /* TODO staking AND-7134 */ },
        )
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
            interestRate = "10",
            periodInDays = 4,
            tokenSymbol = "SOL",
            onStakeClicked = {},
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
