package com.tangem.features.feed.ui.market.list.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.button.PrimaryInverseTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPicker
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.model.market.list.state.MarketsListUM
import com.tangem.features.feed.model.market.list.state.SortByMenuUM
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.ds.button.TangemButtonIconPosition as RedesignTangemButtonIconPosition

@Composable
internal fun Options(
    sortMenuUM: SortByMenuUM,
    trendInterval: MarketsListUM.TrendInterval,
    onIntervalClick: (MarketsListUM.TrendInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    OptionsV2(
        sortMenuUM = sortMenuUM,
        trendInterval = trendInterval,
        onIntervalClick = onIntervalClick,
        modifier = modifier,
    )
}

@Composable
private fun OptionsV2(
    sortMenuUM: SortByMenuUM,
    trendInterval: MarketsListUM.TrendInterval,
    onIntervalClick: (MarketsListUM.TrendInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isShowDropdownMenu by rememberSaveable { mutableStateOf(false) }
    val hapticManager = LocalHapticManager.current

    val segmentItems = remember {
        persistentListOf(
            TangemSegmentUM(
                id = MarketsListUM.TrendInterval.H24.name,
                title = MarketsListUM.TrendInterval.H24.text,
            ),
            TangemSegmentUM(
                id = MarketsListUM.TrendInterval.D7.name,
                title = MarketsListUM.TrendInterval.D7.text,
            ),
            TangemSegmentUM(
                id = MarketsListUM.TrendInterval.M1.name,
                title = MarketsListUM.TrendInterval.M1.text,
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PrimaryInverseTangemButton(
                onClick = {
                    hapticManager.perform(TangemHapticEffect.View.ContextClick)
                    isShowDropdownMenu = true
                },
                iconPosition = RedesignTangemButtonIconPosition.End,
                tangemIconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_chewron_down_20,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
                text = sortMenuUM.selectedOption.text,
                size = TangemButtonSize.X9,
                shape = TangemButtonShape.Rounded,
            )

            TangemSegmentedPicker(
                items = segmentItems,
                initialSelectedItem = segmentItems.firstOrNull { it.id == trendInterval.name },
                isFixed = false,
                isAltSurface = true,
                minSegmentWidth = 54.dp,
                onClick = { segment -> onIntervalClick(MarketsListUM.TrendInterval.valueOf(segment.id)) },
            )
        }

        SortByMenu(
            sortMenuUM = sortMenuUM,
            showDropdownMenu = isShowDropdownMenu,
            onDropdownDismiss = { isShowDropdownMenu = false },
            modifier = Modifier
                .align(Alignment.TopStart)
                .hazeEffectTangem { blurRadius = 10.dp },
        )
    }
}