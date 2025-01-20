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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Transactions block title
 *
 * @param onExploreClick lambda be invoke when explore button was clicked
 * @param modifier modifier
 */
@Composable
internal fun TxHistoryTitle(onExploreClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .padding(top = TangemTheme.dimens.spacing12)
            .padding(horizontal = TangemTheme.dimens.spacing12)
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size24),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResourceSafe(id = R.string.common_transactions),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.subtitle2,
        )

        Row(
            modifier = Modifier.clickable(onClick = onExploreClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing2),
        ) {
            Icon(
                modifier = Modifier.size(size = TangemTheme.dimens.size20),
                painter = painterResource(id = R.drawable.ic_compass_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
            Text(
                text = stringResourceSafe(id = R.string.common_explorer),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
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