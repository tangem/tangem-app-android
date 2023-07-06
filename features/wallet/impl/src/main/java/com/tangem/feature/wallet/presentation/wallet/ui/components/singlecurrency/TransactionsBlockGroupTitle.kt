package com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.WalletContentItemState

/**
 * Transactions block group title
 *
 * @param config   config
 * @param modifier modifier
 */
@Composable
internal fun TransactionsBlockGroupTitle(
    config: WalletContentItemState.SingleCurrencyItem.GroupTitle,
    modifier: Modifier = Modifier,
) {
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
        TransactionsBlockGroupTitle(config = WalletContentItemState.SingleCurrencyItem.GroupTitle(title = "Today"))
    }
}

@Preview
@Composable
private fun Preview_TransactionsBlockGroupTitle_Dark() {
    TangemTheme(isDark = true) {
        TransactionsBlockGroupTitle(config = WalletContentItemState.SingleCurrencyItem.GroupTitle(title = "Today"))
    }
}
