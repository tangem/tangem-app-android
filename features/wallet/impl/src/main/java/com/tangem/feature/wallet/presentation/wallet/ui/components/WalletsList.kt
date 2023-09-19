package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletCard

private const val SHORT_SNAP_ELEMENT_COUNT = 50

/**
 * Wallets list component
 *
 * @param config        config
 * @param lazyListState main content container list state
 *
 * @author Andrew Khokhlov on 30/05/2023
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WalletsList(config: WalletsListConfig, lazyListState: LazyListState) {
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
        items(
            items = config.wallets,
            key = { it.id.stringValue },
            contentType = { it::class.java },
        ) { state ->
            WalletCard(
                state = state,
                modifier = Modifier
                    .animateItemPlacement()
                    .width(itemWidth),
            )
        }
    }
}

/**
 * Custom implementation of fling behaviour that overrides 'shortSnapVelocityThreshold'.
 * Every user's drag action will similar to a short snap
 * if drag offset is less than [SHORT_SNAP_ELEMENT_COUNT] * item width.
 *
 * @param lazyListState lazy list state
 * @param itemWidth     list item width
 *
 * @see rememberSnapFlingBehavior
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberWalletsFlingBehaviour(lazyListState: LazyListState, itemWidth: Dp): SnapFlingBehavior {
    val snappingLayout = remember(lazyListState) { SnapLayoutInfoProvider(lazyListState) }
    val density = LocalDensity.current
    val highVelocityApproachSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()

    return remember(key1 = snappingLayout, key2 = highVelocityApproachSpec, key3 = density) {
        SnapFlingBehavior(
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
@Composable
private fun Preview_WalletsList_LightTheme() {
    TangemTheme(isDark = false) {
        WalletsList(config = WalletPreviewData.walletListConfig, lazyListState = rememberLazyListState())
    }
}

@Preview
@Composable
private fun Preview_WalletsList_DarkTheme() {
    TangemTheme(isDark = true) {
        WalletsList(config = WalletPreviewData.walletListConfig, lazyListState = rememberLazyListState())
    }
}
