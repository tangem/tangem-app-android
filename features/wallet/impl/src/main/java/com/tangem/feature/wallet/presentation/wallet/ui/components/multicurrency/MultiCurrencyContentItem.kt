package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.feature.wallet.presentation.common.component.NetworkGroupItem
import com.tangem.feature.wallet.presentation.common.component.TokenItem
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState

/**
 * Multi-currency content item
 *
 * @param state    item state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun MultiCurrencyContentItem(state: WalletTokensListState.TokensListItemState, modifier: Modifier = Modifier) {
    when (state) {
        is WalletTokensListState.TokensListItemState.NetworkGroupTitle -> {
            NetworkGroupItem(networkName = state.value.resolveReference(), modifier = modifier)
        }
        is WalletTokensListState.TokensListItemState.Token -> {
            TokenItem(state = state.state, modifier = modifier)
        }
    }
}