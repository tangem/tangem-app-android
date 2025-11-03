package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.onramp.swap.entity.AccountAvailabilityUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMData
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import com.tangem.features.onramp.tokenlist.entity.utils.OnrampTokenItemStateConverterFactory
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal class UpdateAccountTokenListTransformer(
    private val appCurrency: AppCurrency,
    private val onItemClick: (TokenItemState, CryptoCurrencyStatus) -> Unit,
    private val accountList: List<AccountAvailabilityUM>,
    private val isBalanceHidden: Boolean,
    private val unavailableErrorText: TextReference,
    private val warning: NotificationUM? = null,
    private val isAccountsMode: Boolean,
) : TokenListUMTransformer {

    private val accountListItemConverter = UpdateAccountTokenItemConverter(
        appCurrency = appCurrency,
        onItemClick = onItemClick,
        unavailableErrorText = unavailableErrorText,
    )

    private val availableConverter = OnrampTokenItemStateConverterFactory
        .createAvailableItemConverter(appCurrency, onItemClick)

    private val unavailableConverter = OnrampTokenItemStateConverterFactory
        .createUnavailableItemConverterV2(appCurrency = appCurrency, unavailableErrorText = unavailableErrorText)

    override fun transform(prevState: TokenListUM): TokenListUM {
        return prevState.copy(
            availableItems = persistentListOf(),
            unavailableItems = persistentListOf(),
            tokensListData = if (isAccountsMode) {
                TokenListUMData.AccountList(
                    tokensList = accountListItemConverter.convertList(accountList).toPersistentList(),
                )
            } else {
                TokenListUMData.TokenList(
                    tokensList = accountList.flatMap { (_, currencyList) ->
                        currencyList.asSequence().map { (isAvailable, status) ->
                            if (isAvailable) {
                                availableConverter.convert(status)
                            } else {
                                unavailableConverter.convert(status)
                            }
                        }.map(TokensListItemUM::Token).toPersistentList()
                    }.toPersistentList(),
                )
            },
            isBalanceHidden = isBalanceHidden,
            warning = warning,
        )
    }
}