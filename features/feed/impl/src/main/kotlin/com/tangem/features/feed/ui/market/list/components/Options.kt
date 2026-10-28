package com.tangem.features.feed.ui.market.list.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_down_20
import com.tangem.features.feed.model.market.list.state.MarketsListUM
import com.tangem.features.feed.model.market.list.state.SortByMenuUM
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun Options(
    sortMenuUM: SortByMenuUM,
    trendInterval: MarketsListUM.TrendInterval,
    onIntervalClick: (MarketsListUM.TrendInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isShowDropdownMenu by rememberSaveable { mutableStateOf(false) }
    val hapticManager = LocalHapticManager.current

    val segmentItems = remember(trendInterval, onIntervalClick) {
        MarketsListUM.TrendInterval.entries.map { interval ->
            TangemSegmentedControl.Item(
                id = interval.name,
                label = interval.text,
                isSelected = interval == trendInterval,
                onClick = { onIntervalClick(interval) },
            )
        }.toImmutableList()
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TangemButton(
                text = sortMenuUM.selectedOption.text,
                onClick = {
                    hapticManager.perform(TangemHapticEffect.View.ContextClick)
                    isShowDropdownMenu = true
                },
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X9,
                iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_chevron_down_20),
                modifier = Modifier.weight(weight = 1f, fill = false),
            )

            TangemSegmentedControl(items = segmentItems)
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