package com.tangem.feature.wallet.presentation.wallet.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

private const val SHORT_SNAP_ELEMENT_COUNT = 25

/**
 * Wallets list component
 *
 * @param lazyListState main content container list state
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WalletsList(
    lazyListState: LazyListState,
    wallets: ImmutableList<WalletCardState>,
    isBalanceHidden: Boolean,
) {
    val horizontalCardPadding = TangemTheme.dimens.spacing16
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val itemWidth by remember(screenWidth) { derivedStateOf { screenWidth - horizontalCardPadding * 2 } }

    LazyRow(
        modifier = Modifier.background(color = TangemTheme.colors.background.secondary),
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        flingBehavior = rememberWalletsFlingBehaviour(lazyListState = lazyListState, itemWidth = itemWidth),
    ) {
        // Using key is required to guarantee the correct wallet selection
        items(items = wallets, key = { it.id.stringValue }, contentType = { "wallet_card" }) { state ->
            WalletCard(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.width(itemWidth),
            )
        }
    }
}

/**
 * Custom implementation of fling behaviour that overrides 'shortSnapVelocityThreshold'.
 *
 * @param lazyListState lazy list state
 *
 * @see rememberSnapFlingBehavior
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberWalletsFlingBehaviour(lazyListState: LazyListState, itemWidth: Dp): TangemSnapFlingBehavior {
    val snappingLayout = remember(lazyListState) { TangemSnapLayoutInfoProvider(lazyListState) }
    val density = LocalDensity.current
    val highVelocityApproachSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()

    return remember(key1 = snappingLayout, key2 = highVelocityApproachSpec, key3 = density) {
        TangemSnapFlingBehavior(
            snapLayoutInfoProvider = snappingLayout,
            lowVelocityAnimationSpec = tween(durationMillis = 1000, easing = LinearEasing),
            highVelocityAnimationSpec = highVelocityApproachSpec,
            snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            density = density,
            shortSnapVelocityThreshold = itemWidth * SHORT_SNAP_ELEMENT_COUNT,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_WalletsList() {
    TangemThemePreview {
        WalletsList(
            lazyListState = rememberLazyListState(),
            wallets = WalletPreviewData.wallets.values.toPersistentList(),
            isBalanceHidden = false,
        )
    }
}