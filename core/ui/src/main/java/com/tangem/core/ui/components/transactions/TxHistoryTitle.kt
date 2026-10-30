package com.tangem.core.ui.components.transactions

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TransactionHistoryBlockTestTags

/**
 * Transactions block title
 *
 * @param onExploreClick lambda be invoke when explore button was clicked
 * @param modifier modifier
 */
@Composable
fun TxHistoryTitle(onExploreClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .padding(top = 12.dp)
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResourceSafe(id = R.string.common_transactions),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.subtitle2,
            modifier = Modifier.testTag(TransactionHistoryBlockTestTags.TITLE_TEXT),
        )

        Row(
            modifier = Modifier
                .clickable(onClick = onExploreClick)
                .testTag(TransactionHistoryBlockTestTags.EXPLORER_ICON),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 2.dp),
        ) {
            Icon(
                modifier = Modifier.size(size = 20.dp),
                painter = painterResource(id = R.drawable.ic_compass_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
            Text(
                text = stringResourceSafe(id = R.string.common_explorer),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
                modifier = Modifier.testTag(TransactionHistoryBlockTestTags.EXPLORER_TEXT),
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TransactionsBlockTitle() {
    TangemThemePreview {
        TxHistoryTitle(onExploreClick = {})
    }
}