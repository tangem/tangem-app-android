package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.feature.wallet.presentation.common.component.NetworkGroupItem
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.TokensListItemState

/**
 * Multi-currency content item
 *
 * @param state    item state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun MultiCurrencyContentItem(
    state: TokensListItemState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TokensListItemState.NetworkGroupTitle -> {
            NetworkGroupItem(networkName = state.name.resolveReference(), modifier = modifier)
        }
        is TokensListItemState.Token -> {
            TokenItem(state = state.state, isBalanceHidden = isBalanceHidden, modifier = modifier)
        }
    }
}