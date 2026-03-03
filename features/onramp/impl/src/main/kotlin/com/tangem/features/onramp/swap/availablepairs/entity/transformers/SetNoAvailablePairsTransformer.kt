package com.tangem.features.onramp.swap.availablepairs.entity.transformers

import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.common.ui.account.TokensListPortfolioItemConverter
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMData
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import com.tangem.features.onramp.tokenlist.entity.utils.OnrampTokenItemStateConverterFactory
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal class SetNoAvailablePairsTransformer(
    private val appCurrency: AppCurrency,
    private val accountList: Map<Account.CryptoPortfolio, List<CryptoCurrencyStatus>>,
    private val isBalanceHidden: Boolean,
    private val isAccountsMode: Boolean,
    private val unavailableErrorText: TextReference,
) : TokenListUMTransformer {
    private val unavailableConverter = OnrampTokenItemStateConverterFactory
        .createUnavailableItemConverterV2(appCurrency = appCurrency, unavailableErrorText = unavailableErrorText)

    override fun transform(prevState: TokenListUM): TokenListUM {
        val totalTokensCount = accountList.values.sumOf { it.size }

        return prevState.copy(
            availableItems = persistentListOf(),
            unavailableItems = persistentListOf(),
            tokensListData = if (isAccountsMode) {
                TokenListUMData.AccountList(
                    tokensList = accountList.map { (account, cryptoCurrencies) ->
                        TokensListPortfolioItemConverter(
                            tokenItemUM = AccountCryptoPortfolioItemStateConverter(
                                appCurrency = appCurrency,
                                account = account,
                                onItemClick = null,
                            ).convert(TotalFiatBalance.Failed),
                            isExpanded = true,
                            isCollapsable = false,
                            tokens = unavailableConverter.convertList(cryptoCurrencies)
                                .map(TokensListItemUM::Token)
                                .toPersistentList(),
                        ).convert(Unit)
                    }.toPersistentList(),
                    totalTokensCount = totalTokensCount,
                )
            } else {
                TokenListUMData.TokenList(
                    tokensList = accountList.flatMap { (_, cryptoCurrencies) ->
                        unavailableConverter.convertList(cryptoCurrencies)
                            .map(TokensListItemUM::Token)
                    }.toPersistentList(),
                    totalTokensCount = totalTokensCount,
                )
            },
            isBalanceHidden = isBalanceHidden,
            warning = NotificationUM.Warning.SwapNoAvailablePair,
        )
    }
}