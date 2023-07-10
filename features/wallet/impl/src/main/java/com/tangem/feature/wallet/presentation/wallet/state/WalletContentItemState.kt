package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.components.transactions.TransactionState
import com.tangem.feature.wallet.presentation.common.state.TokenItemState

/**
 * Wallet screen content item state
 *
* [REDACTED_AUTHOR]
 */
internal sealed interface WalletContentItemState {

    /** Multi currency wallet content state */
    sealed interface MultiCurrencyItem : WalletContentItemState {

        /**
         * Network group title item
         *
         * @property networkName network name
         */
        data class NetworkGroupTitle(val networkName: String) : MultiCurrencyItem

        /**
         * Token item
         *
         * @property state token item state
         */
        data class Token(val state: TokenItemState) : MultiCurrencyItem
    }

    /** Single currency wallet content state */
    sealed interface SingleCurrencyItem : WalletContentItemState {

        /**
         * Title item
         *
         * @property onExploreClick lambda be invoke when explore button was clicked
         */
        data class Title(val onExploreClick: () -> Unit) : SingleCurrencyItem

        /**
         * Group title item
         *
         * @property title title
         */
        data class GroupTitle(val title: String) : SingleCurrencyItem

        /**
         * Transaction item
         *
         * @property state transaction state
         */
        data class Transaction(val state: TransactionState) : SingleCurrencyItem
    }

    object Loading : WalletContentItemState
}
