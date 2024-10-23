package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.component.NetworkTitleItem
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.TokensListItemState

/**
 * Multi-currency content item
 *
 * @param state    item state
 * @param modifier modifier
 *
* [REDACTED_AUTHOR]
 */
@Composable
internal fun MultiCurrencyContentItem(
    state: TokensListItemState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    val modifierWithBackground = modifier.background(color = TangemTheme.colors.background.primary)

    when (state) {
        is TokensListItemState.NetworkGroupTitle -> {
            NetworkTitleItem(networkName = state.name.resolveReference(), modifier = modifierWithBackground)
        }
        is TokensListItemState.Token -> {
            TokenItem(
                state = state.state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifierWithBackground,
            )
        }
        is TokensListItemState.SearchBar -> {
            SearchBar(
                state = state.searchBarUM,
                modifier = modifierWithBackground.padding(all = 12.dp),
            )
        }
    }
}
