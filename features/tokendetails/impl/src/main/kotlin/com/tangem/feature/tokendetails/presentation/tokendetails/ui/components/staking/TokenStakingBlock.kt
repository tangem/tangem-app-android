package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.staking

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.getGreyScaleColorFilter
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData.stakingAvailableBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData.stakingErrorBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData.stakingLoadingBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.state.IconState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.CurrencyIcon
import com.tangem.features.tokendetails.impl.R

/**
 * Token staking block
 *
 * @param state    component state
 * @param modifier modifier
 */
@Composable
internal fun TokenStakingBlock(state: StakingBlockUM, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = state,
        contentAlignment = Alignment.CenterStart,
        label = "Staking block animation",
    ) {
        when (it) {
            is StakingBlockUM.Error -> Row {} // TODO staking error
            is StakingBlockUM.Loading -> StakingLoading(
                iconState = it.iconState,
                modifier = modifier,
            )
            is StakingBlockUM.Staked -> StakingBalanceBlock(
                state = it,
                modifier = modifier,
            )
            is StakingBlockUM.StakeAvailable -> StakingAvailableContent(
                state = it,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun StakingAvailableContent(state: StakingBlockUM.StakeAvailable, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .fillMaxWidth()
            .padding(all = TangemTheme.dimens.spacing12),
    ) {
        Row {
            val (alpha, colorFilter) = remember(state.iconState.isGrayscale) {
                getGreyScaleColorFilter(state.iconState.isGrayscale)
            }
            CurrencyIcon(
                modifier = Modifier
                    .size(TangemTheme.dimens.size20)
                    .clip(TangemTheme.shapes.roundedCorners8)
                    .align(Alignment.CenterVertically),
                icon = state.iconState,
                alpha = alpha,
                colorFilter = colorFilter,
            )
            SpacerW8()
            Column {
                Text(
                    text = stringResource(
                        R.string.token_details_staking_block_title,
                        state.interestRate,
                    ),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle2,
                )

                Spacer(modifier = Modifier.size(TangemTheme.dimens.size4))

                Text(
                    text = stringResource(
                        R.string.token_details_staking_block_subtitle,
                        state.tokenSymbol,
                        state.periodInDays,
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
            onClick = state.onStakeClicked,
        )
    }
}

@Composable
private fun StakingLoading(iconState: IconState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size72)
            .padding(all = TangemTheme.dimens.spacing12),

    ) {
        Row {
            val (alpha, colorFilter) = remember(iconState.isGrayscale) {
                getGreyScaleColorFilter(iconState.isGrayscale)
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
            onClick = { /* [REDACTED_TODO_COMMENT] */ },
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokenStakingBlock(
    @PreviewParameter(StakingBlockStateProvider::class)
    state: StakingBlockUM,
) {
    TangemThemePreview {
        TokenStakingBlock(state = state)
    }
}

private class StakingBlockStateProvider : CollectionPreviewParameterProvider<StakingBlockUM>(
    collection = listOf(
        stakingLoadingBlock,
        stakingAvailableBlock,
        stakingErrorBlock,
    ),
)
// endregion Preview