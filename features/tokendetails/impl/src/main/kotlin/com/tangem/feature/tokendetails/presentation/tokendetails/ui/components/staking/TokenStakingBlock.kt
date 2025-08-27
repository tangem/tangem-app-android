package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.staking

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import com.tangem.core.ui.utils.getGreyScaleColorFilter
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData.stakingAvailableBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData.stakingBalanceBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData.stakingLoadingBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData.stakingTemporaryUnavailableBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.CurrencyIcon
import com.tangem.features.tokendetails.impl.R

/**
 * Token staking block
 *
 * @param state            component state
 * @param isBalanceHidden  whether to hide balance
 * @param modifier         modifier
 */
@Composable
internal fun TokenStakingBlock(state: StakingBlockUM, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.primary)
            .clickable(
                enabled = state is StakingBlockUM.Staked,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { (state as? StakingBlockUM.Staked)?.onStakeClicked?.invoke() },
            )
            .fillMaxWidth()
            .padding(all = TangemTheme.dimens.spacing12),
    ) {
        AnimatedContent(
            targetState = state,
            contentAlignment = Alignment.CenterStart,
            label = "Staking block animation",
        ) {
            when (it) {
                is StakingBlockUM.TemporaryUnavailable -> StakingTemporaryUnavailableBlock()
                is StakingBlockUM.Loading -> StakingLoading()
                is StakingBlockUM.Staked -> StakingBalanceBlock(
                    state = it,
                    isBalanceHidden = isBalanceHidden,
                )
                is StakingBlockUM.StakeAvailable -> StakingAvailableContent(
                    state = it,
                )
            }
        }
    }
}

@Composable
private fun StakingAvailableContent(state: StakingBlockUM.StakeAvailable, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TokenDetailsScreenTestTags.STAKING_AVAILABLE_BLOCK),
    ) {
        Row {
            val (alpha, colorFilter) = remember(state.iconState.isGrayscale) {
                getGreyScaleColorFilter(state.iconState.isGrayscale)
            }
            CurrencyIcon(
                modifier = Modifier
                    .size(TangemTheme.dimens.size20)
                    .clip(TangemTheme.shapes.roundedCorners8)
                    .align(Alignment.CenterVertically)
                    .testTag(TokenDetailsScreenTestTags.STAKING_CURRENCY_ICON),
                icon = state.iconState,
                alpha = alpha,
                colorFilter = colorFilter,
            )
            SpacerW8()
            Column {
                Text(
                    text = state.titleText.resolveReference(),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle2,
                    modifier = Modifier.testTag(TokenDetailsScreenTestTags.STAKING_SERVICE_TITLE),
                )

                Spacer(modifier = Modifier.size(TangemTheme.dimens.size4))

                Text(
                    text = state.subtitleText.resolveReference(),
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography.body2,
                    modifier = Modifier.testTag(TokenDetailsScreenTestTags.STAKING_SERVICE_TEXT),
                )

                Spacer(modifier = Modifier.size(TangemTheme.dimens.size8))
            }
        }
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(id = R.string.common_stake),
            enabled = state.isEnabled,
            onClick = state.onStakeClicked,
        )
    }
}

@Composable
private fun StakingLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row {
            Column {
                RectangleShimmer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TangemTheme.dimens.size20),
                )
                Spacer(modifier = Modifier.size(TangemTheme.dimens.size8))
                RectangleShimmer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TangemTheme.dimens.size36),
                )
                Spacer(modifier = Modifier.size(TangemTheme.dimens.size8))
            }
        }
        RectangleShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .height(TangemTheme.dimens.size48),
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
        TokenStakingBlock(
            state = state,
            isBalanceHidden = false,
        )
    }
}

private class StakingBlockStateProvider : CollectionPreviewParameterProvider<StakingBlockUM>(
    collection = listOf(
        stakingLoadingBlock,
        stakingAvailableBlock,
        stakingTemporaryUnavailableBlock,
        stakingBalanceBlock,
    ),
)
// endregion Preview