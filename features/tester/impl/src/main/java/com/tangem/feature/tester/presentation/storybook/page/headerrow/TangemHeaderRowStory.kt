@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.headerrow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.header.TangemHeaderRow
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.ds.row.internal.TangemRowTailUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemHeaderRowStory

@Composable
internal fun TangemHeaderRowStory(state: TangemHeaderRowStory, modifier: Modifier = Modifier) {
    val rows = remember { buildSampleRows() }

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
            TangemHeaderRow(
                headerRowUM = um,
                isBalanceHidden = state.isBalanceHidden,
                modifier = Modifier.background(TangemTheme.colors2.surface.level3),
            )
            HorizontalDivider(
                color = TangemTheme.colors2.border.neutral.secondary,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

private fun buildSampleRows(): List<TangemHeaderRowUM> = listOf(
    TangemHeaderRowUM(
        id = "icon_subtitle_no_tail",
        startIconUM = TangemIconUM.Currency(currencyIconState = CurrencyIconState.Locked),
        tailUM = TangemRowTailUM.Empty,
        title = stringReference("Account"),
        subtitle = stringReference("\$ 42,900.17"),
    ),
    TangemHeaderRowUM(
        id = "icon_subtitle_collapse",
        startIconUM = TangemIconUM.Currency(currencyIconState = CurrencyIconState.Locked),
        tailUM = TangemRowTailUM.Icon(R.drawable.ic_arrow_collapse_24),
        title = stringReference("Account"),
        subtitle = stringReference("\$ 42,900.17"),
    ),
    TangemHeaderRowUM(
        id = "icon_subtitle_group_drop",
        startIconUM = TangemIconUM.Currency(currencyIconState = CurrencyIconState.Locked),
        tailUM = TangemRowTailUM.Icon(R.drawable.ic_group_drop_24),
        title = stringReference("Account"),
        subtitle = stringReference("\$ 42,900.17"),
    ),
    TangemHeaderRowUM(
        id = "title_only",
        title = stringReference("Account"),
    ),
)

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