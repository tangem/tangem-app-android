package com.tangem.core.ui.components.tokenlist.internal

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.rows.NetworkTitle
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Group title item
 *
 * @param state    state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
fun GroupTitleItem(state: TokensListItemUM.GroupTitle, modifier: Modifier = Modifier) {
    NetworkTitle(
        title = {
            Text(
                text = state.text.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
        },
        modifier = modifier,
    )
}