package com.tangem.features.commonfeatures.api.choosetoken.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ChooseTokenPortfolioFullBlockUM(
    val walletList: WalletListUM,
    val isBalanceHidden: Boolean,
    val isSearching: Boolean,
    val tokensListData: TokenListUMData,
)

data class WalletListUM(
    val items: ImmutableList<WalletTabUM>,
)

data class WalletTabUM(
    val text: TextReference,
    val count: TextReference?,
    val isSelected: Boolean,
    val onClick: () -> Unit,
)

@Immutable
sealed interface TokenListUMData {

    val tokensList: ImmutableList<TokensListItemUM>
    val totalTokensCount: Int

    data class AccountList(
        override val tokensList: ImmutableList<TokensListItemUM.Portfolio>,
        override val totalTokensCount: Int,
    ) : TokenListUMData

    data class TokenList(
        override val tokensList: ImmutableList<TokensListItemUM>,
        override val totalTokensCount: Int,
    ) : TokenListUMData

    data object EmptyList : TokenListUMData {
        override val tokensList: ImmutableList<TokensListItemUM> = persistentListOf()
        override val totalTokensCount: Int = EMPTY_TOKENS_COUNT
    }

    private companion object {
        const val EMPTY_TOKENS_COUNT = 0
    }
}