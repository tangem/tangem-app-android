package com.tangem.features.tangempay.tiers.select

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
internal fun ComparePlansBottomSheet(compare: TangemPaySelectPlanUM.ComparePlans?) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = compare != null,
            onDismissRequest = compare?.onDismiss ?: {},
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = resourceReference(R.string.tangempay_select_plan_compare).resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                )
                TangemButton.Close(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = compare?.onDismiss ?: {},
                )
            }
        },
        content = {
            if (compare != null) {
                ComparePlansContent(compare)
            }
        },
    )
}

@Composable
private fun ComparePlansContent(compare: TangemPaySelectPlanUM.ComparePlans) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth - CARD_PEEK
        val cardWidthPx = with(LocalDensity.current) { cardWidth.toPx() }
        val scope = rememberCoroutineScope()

        val rowStates = remember(compare.plans.size) { List(compare.plans.size) { LazyListState() } }
        val tabState = rememberLazyListState()

        rowStates.forEach { source ->
            LaunchedEffect(source) {
                snapshotFlow { source.firstVisibleItemIndex to source.firstVisibleItemScrollOffset }
                    .collect { (index, offset) ->
                        if (source.isScrollInProgress) {
                            rowStates.forEach { target -> if (target !== source) target.scrollToItem(index, offset) }
                        }
                    }
            }
        }

        val selectedIndex by remember(rowStates, cardWidthPx) {
            derivedStateOf {
                val primary = rowStates.firstOrNull() ?: return@derivedStateOf 0
                val extra = if (primary.firstVisibleItemScrollOffset > cardWidthPx / 2f) 1 else 0
                (primary.firstVisibleItemIndex + extra).coerceIn(0, compare.attributes.lastIndex.coerceAtLeast(0))
            }
        }

        LaunchedEffect(selectedIndex) { tabState.animateScrollToItem(selectedIndex) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            compare.plans.fastForEachIndexed { index, plan ->
                PlanValuesSection(plan = plan, listState = rowStates[index], cardWidth = cardWidth)
            }
            AttributeTabs(
                attributes = compare.attributes,
                selectedIndex = selectedIndex,
                listState = tabState,
                onTabClick = { index -> scope.launch { rowStates.firstOrNull()?.animateScrollToItem(index) } },
            )
        }
    }
}

@Composable
private fun PlanValuesSection(plan: TangemPaySelectPlanUM.ComparePlans.Plan, listState: LazyListState, cardWidth: Dp) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
            text = plan.name.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        ) {
            items(count = plan.values.size) { index ->
                ValueCard(value = plan.values[index], width = cardWidth)
            }
        }
    }
}

@Composable
private fun ValueCard(value: TextReference, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .heightIn(112.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.tertiary)
            .padding(16.dp),
    ) {
        Text(
            text = value.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
private fun AttributeTabs(
    attributes: ImmutableList<TextReference>,
    selectedIndex: Int,
    listState: LazyListState,
    onTabClick: (Int) -> Unit,
) {
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(attributes) { index, title ->
            AttributeTab(title = title, selected = index == selectedIndex, onClick = { onTabClick(index) })
        }
    }
}

@Composable
private fun AttributeTab(title: TextReference, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) TangemTheme.colors3.bg.tertiary else TangemTheme.colors3.bg.secondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = if (selected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
            maxLines = 1,
        )
    }
}

private val CARD_PEEK = 72.dp

@Preview(showBackground = true, widthDp = 402)
@Preview(showBackground = true, widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComparePlansContentPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.secondary),
        ) {
            ComparePlansContent(compare = previewCompare())
        }
    }
}

private fun previewCompare() = TangemPaySelectPlanUM.ComparePlans(
    attributes = persistentListOf(
        stringReference("Visa Programme"),
        stringReference("Plan fee"),
        stringReference("FX fee"),
        stringReference("Daily spending limit"),
        stringReference("Max cards issued"),
        stringReference("Additional benefits"),
    ),
    plans = persistentListOf(
        TangemPaySelectPlanUM.ComparePlans.Plan(
            name = stringReference("Basic"),
            values = persistentListOf(
                stringReference("Platinum"),
                stringReference("$0.00"),
                stringReference("1%"),
                stringReference("$10.000"),
                stringReference("3"),
                stringReference("No"),
            ),
        ),
        TangemPaySelectPlanUM.ComparePlans.Plan(
            name = stringReference("Plus"),
            values = persistentListOf(
                stringReference("Signature"),
                stringReference("$29.99/month"),
                stringReference("1%"),
                stringReference("$50.000"),
                stringReference("5"),
                stringReference("Benefit 1, Benefit 2, Benefit 3"),
            ),
        ),
    ),
    onDismiss = {},
)