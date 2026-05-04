package com.tangem.features.feed.ui.market.list.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.button.PrimaryInverseTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPicker
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.model.market.list.state.MarketsListUM
import com.tangem.features.feed.model.market.list.state.SortByMenuUM
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import dev.chrisbanes.haze.HazeState
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.ds.button.TangemButtonIconPosition as RedesignTangemButtonIconPosition

@Suppress("LongParameterList")
@Composable
internal fun Options(
    sortMenuUM: SortByMenuUM,
    sortByTypeUM: SortByTypeUM,
    trendInterval: MarketsListUM.TrendInterval,
    hazeState: HazeState,
    onSortByClick: () -> Unit,
    onIntervalClick: (MarketsListUM.TrendInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalRedesignEnabled.current) {
        OptionsV2(
            sortMenuUM = sortMenuUM,
            trendInterval = trendInterval,
            onIntervalClick = onIntervalClick,
            modifier = modifier,
            hazeState = hazeState,
        )
    } else {
        OptionsV1(
            sortByTypeUM = sortByTypeUM,
            trendInterval = trendInterval,
            onSortByClick = onSortByClick,
            onIntervalClick = onIntervalClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun OptionsV1(
    sortByTypeUM: SortByTypeUM,
    trendInterval: MarketsListUM.TrendInterval,
    onSortByClick: () -> Unit,
    onIntervalClick: (MarketsListUM.TrendInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SecondarySmallButton(
            config = SmallButtonConfig(
                text = sortByTypeUM.text,
                onClick = onSortByClick,
                icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_chevron_24),
            ),
        )
        SegmentedButtons(
            config = persistentListOf(
                MarketsListUM.TrendInterval.H24,
                MarketsListUM.TrendInterval.D7,
                MarketsListUM.TrendInterval.M1,
            ),
            color = TangemTheme.colors.button.secondary,
            initialSelectedItem = trendInterval,
            onClick = onIntervalClick,
            modifier = Modifier
                .width(160.dp)
                .fillMaxHeight(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .padding(
                        vertical = TangemTheme.dimens.spacing4,
                    ),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = it.text.resolveReference(),
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        }
    }
}

@Composable
private fun OptionsV2(
    sortMenuUM: SortByMenuUM,
    trendInterval: MarketsListUM.TrendInterval,
    hazeState: HazeState,
    onIntervalClick: (MarketsListUM.TrendInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isShowDropdownMenu by rememberSaveable { mutableStateOf(false) }

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

    Row(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PrimaryInverseTangemButton(
            onClick = {
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
        modifier = Modifier.hazeEffectTangem(hazeState) {
            blurRadius = 10.dp
        },
    )
}