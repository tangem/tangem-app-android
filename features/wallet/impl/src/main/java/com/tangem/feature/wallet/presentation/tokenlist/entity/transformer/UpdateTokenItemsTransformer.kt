package com.tangem.feature.wallet.presentation.tokenlist.entity.transformer

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.wallet.presentation.tokenlist.entity.TokenListUM
import com.tangem.feature.wallet.presentation.tokenlist.entity.TokenListUMTransformer
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.TokensListItemState
import kotlinx.collections.immutable.toImmutableList

internal class UpdateTokenItemsTransformer(
    appCurrency: AppCurrency,
    onItemClick: (CryptoCurrencyStatus) -> Unit,
    private val statuses: List<CryptoCurrencyStatus>,
    private val isBalanceHidden: Boolean,
) : TokenListUMTransformer {

    private val converter = TokenItemStateConverter(appCurrency = appCurrency, onItemClick = onItemClick)

    override fun transform(prevState: TokenListUM): TokenListUM {
        val items = converter.convertList(input = statuses).map(TokensListItemState::Token)

        return prevState.copy(
            items = (listOfNotNull(prevState.getSearchBar()) + items).toImmutableList(),
            isBalanceHidden = isBalanceHidden,
        )
    }
}
