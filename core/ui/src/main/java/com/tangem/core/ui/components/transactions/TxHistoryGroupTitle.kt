package com.tangem.core.ui.components.transactions

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.transactions.state.TxHistoryState.TxHistoryItemState
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import java.util.UUID

/**
 * Transactions block group title
 *
 * @param config   config
 * @param modifier modifier
 */
@Composable
internal fun TxHistoryGroupTitle(config: TxHistoryItemState.GroupTitle, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .padding(
                vertical = TangemTheme.dimens.spacing8,
                horizontal = TangemTheme.dimens.spacing12,
            )
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size24),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = config.title,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TransactionsBlockGroupTitle() {
    TangemThemePreview {
        TxHistoryGroupTitle(
            config = TxHistoryItemState.GroupTitle(title = "Today", itemKey = UUID.randomUUID().toString()),
        )
    }
}