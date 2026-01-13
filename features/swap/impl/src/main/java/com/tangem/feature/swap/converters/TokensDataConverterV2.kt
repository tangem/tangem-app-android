package com.tangem.feature.swap.converters

import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.feature.swap.domain.models.ui.CurrenciesGroup
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.feature.swap.presentation.R
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList

internal class TokensDataConverterV2(
    private val onSearchEntered: (String) -> Unit,
    private val onTokenSelected: (String) -> Unit,
    private val tokensDataState: CurrenciesGroup,
    private val isBalanceHidden: Boolean,
    private val isAccountsMode: Boolean,
    appCurrencyProvider: Provider<AppCurrency>,
) : Transformer<SwapStateHolder> {

    private val accountListItemConverter = AccountTokenItemConverter(
        appCurrency = appCurrencyProvider(),
        unavailableErrorText = resourceReference(R.string.tokens_list_unavailable_to_swap_source_header),
        onItemClick = onTokenSelected,
    )

    override fun transform(prevState: SwapStateHolder): SwapStateHolder {
        val accountList = tokensDataState.accountCurrencyList
        val currentMarketsState = prevState.selectTokenState?.marketsState
        return prevState.copy(
            selectTokenState = SwapSelectTokenStateHolder(
                availableTokens = persistentListOf(),
                unavailableTokens = persistentListOf(),
                tokensListData = if (isAccountsMode) {
                    TokenListUMData.AccountList(
                        tokensList = accountListItemConverter.convertList(accountList).toPersistentList(),
                    )
                } else {
                    TokenListUMData.TokenList(
                        tokensList = persistentListOf(
                            TokensListItemUM.GroupTitle(
                                id = "available_tokens_title",
                                text = resourceReference(R.string.exchange_tokens_available_tokens_header),
                            ),
                        ) + accountList.flatMap { (_, currencyList) ->
                            currencyList.asSequence().map { accountSwapCurrency ->
                                if (accountSwapCurrency.isAvailable) {
                                    accountListItemConverter.createAvailableItemConverter()
                                } else {
                                    accountListItemConverter.createUnavailableItemConverter()
                                }.convert(accountSwapCurrency.cryptoCurrencyStatus)
                            }.map(TokensListItemUM::Token).toPersistentList()
                        }.toPersistentList(),
                    )
                },
                marketsState = currentMarketsState,
                onSearchEntered = onSearchEntered,
                onTokenSelected = onTokenSelected,
                isBalanceHidden = isBalanceHidden,
                isAfterSearch = tokensDataState.isAfterSearch,
            ),
        )
    }
}