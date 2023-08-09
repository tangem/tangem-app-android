package com.tangem.core.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun TxHistoryGroupTitle(config: TxHistoryState.TxHistoryItemState.GroupTitle, modifier: Modifier = Modifier) {
    Text(
        text = config.title,
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxWidth()
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing14,
            ),
        color = TangemTheme.colors.text.tertiary,
        textAlign = TextAlign.Start,
        style = TangemTheme.typography.body2,
    )
}

@Preview
@Composable
private fun Preview_TransactionsBlockGroupTitle_Light() {
    TangemTheme(isDark = false) {
        TxHistoryGroupTitle(config = TxHistoryState.TxHistoryItemState.GroupTitle(title = "Today"))
    }
}

@Preview
@Composable
private fun Preview_TransactionsBlockGroupTitle_Dark() {
    TangemTheme(isDark = true) {
        TxHistoryGroupTitle(config = TxHistoryState.TxHistoryItemState.GroupTitle(title = "Today"))
    }
}
