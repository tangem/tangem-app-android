@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.tokenrow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.row.token.TangemTokenRow
import com.tangem.core.ui.ds.row.token.internal.TangemTokenRowPreviewData
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemTokenRowStory

@Composable
internal fun TangemTokenRowStory(state: TangemTokenRowStory, modifier: Modifier = Modifier) {
    val rows = remember {
        listOf(
            TangemTokenRowPreviewData.defaultState,
            TangemTokenRowPreviewData.defaultEllipsisState,
            TangemTokenRowPreviewData.tokenState,
            TangemTokenRowPreviewData.customTokenState,
            TangemTokenRowPreviewData.draggableState,
            TangemTokenRowPreviewData.draggableStateV2,
            TangemTokenRowPreviewData.loadingState,
            TangemTokenRowPreviewData.emptyState,
            TangemTokenRowPreviewData.unreachableState,
            TangemTokenRowPreviewData.accountState,
            TangemTokenRowPreviewData.accountLetterState,
            TangemTokenRowPreviewData.accountEllipsisState,
            TangemTokenRowPreviewData.promoBannerState,
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        stickyHeader("balance_toggle") {
            BalanceToggle(
                isHidden = state.isBalanceHidden,
                onToggle = state.onBalanceHiddenToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TangemTheme.colors2.surface.level1)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        items(rows, key = { it.id }) { um ->
            TangemTokenRow(
                tokenRowUM = um,
                isBalanceHidden = state.isBalanceHidden,
                reorderableState = null,
                modifier = Modifier.background(TangemTheme.colors2.surface.level1),
            )
            HorizontalDivider(
                color = TangemTheme.colors2.border.neutral.secondary,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun BalanceToggle(isHidden: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier,
    ) {
        Text(
            text = "isBalanceHidden",
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        Switch(checked = isHidden, onCheckedChange = { onToggle() })
    }
}