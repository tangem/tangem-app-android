package com.tangem.core.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.R
import com.tangem.core.ui.components.transactions.state.TxHistoryState

@Composable
internal fun TxHistoryTitle(config: TxHistoryState.TxHistoryItemState.Title, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxWidth()
            .padding(top = TangemTheme.dimens.spacing12)
            .padding(horizontal = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(id = R.string.common_transactions),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.subtitle2,
        )

        Row(
            modifier = Modifier.clickable(onClick = config.onExploreClick),
            horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing4),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_compass_24),
                contentDescription = null,
                modifier = Modifier.size(size = TangemTheme.dimens.size18),
                tint = TangemTheme.colors.icon.informative,
            )
            Text(
                text = stringResource(id = R.string.common_explorer),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
            )
        }
    }
}

@Preview
@Composable
private fun Preview_TransactionsBlockTitle_Light() {
    TangemTheme(isDark = false) {
        TxHistoryTitle(config = TxHistoryState.TxHistoryItemState.Title(onExploreClick = {}))
    }
}

@Preview
@Composable
private fun Preview_TransactionsBlockTitle_Dark() {
    TangemTheme(isDark = true) {
        TxHistoryTitle(config = TxHistoryState.TxHistoryItemState.Title(onExploreClick = {}))
    }
}

