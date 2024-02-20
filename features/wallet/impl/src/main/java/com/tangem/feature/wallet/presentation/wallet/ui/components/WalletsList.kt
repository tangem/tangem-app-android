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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * Wallets list component
 *
 * @param lazyListState main content container list state
 *
* [REDACTED_AUTHOR]
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
        flingBehavior = rememberWalletsFlingBehaviour(lazyListState = lazyListState),
    ) {
        items(
            items = wallets,
            key = { it.id.stringValue },
            contentType = { it.id.stringValue },
        ) { state ->
            WalletCard(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .animateItemPlacement()
                    .width(itemWidth),
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
private fun rememberWalletsFlingBehaviour(lazyListState: LazyListState): SnapFlingBehavior {
    val snappingLayout = remember(lazyListState) { SnapLayoutInfoProvider(lazyListState) }
    val density = LocalDensity.current
    val highVelocityApproachSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()

    return remember(key1 = snappingLayout, key2 = highVelocityApproachSpec, key3 = density) {
        SnapFlingBehavior(
            snapLayoutInfoProvider = snappingLayout,
            lowVelocityAnimationSpec = tween(durationMillis = 1000, easing = LinearEasing),
            highVelocityAnimationSpec = highVelocityApproachSpec,
            snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        )
    }
}

@Preview
@Composable
private fun Preview_WalletsList_LightTheme() {
    TangemTheme(isDark = false) {
        WalletsList(
            lazyListState = rememberLazyListState(),
            wallets = WalletPreviewData.wallets.values.toPersistentList(),
            isBalanceHidden = false,
        )
    }
}

@Preview
@Composable
private fun Preview_WalletsList_DarkTheme() {
    TangemTheme(isDark = true) {
        WalletsList(
            lazyListState = rememberLazyListState(),
            wallets = WalletPreviewData.wallets.values.toPersistentList(),
            isBalanceHidden = false,
        )
    }
}
