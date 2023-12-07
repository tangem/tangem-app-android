package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.feature.wallet.presentation.common.component.NetworkGroupItem
import com.tangem.feature.wallet.presentation.common.component.TokenItem
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletTokensListState.TokensListItemState

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
    state: WalletTokensListState.TokensListItemState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is WalletTokensListState.TokensListItemState.NetworkGroupTitle -> {
            NetworkGroupItem(networkName = state.name.resolveReference(), modifier = modifier)
        }
        is WalletTokensListState.TokensListItemState.Token -> {
            TokenItem(state = state.state, isBalanceHidden = isBalanceHidden, modifier = modifier)
        }
    }
}

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