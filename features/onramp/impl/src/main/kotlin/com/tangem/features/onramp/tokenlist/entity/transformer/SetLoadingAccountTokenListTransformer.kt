package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.onramp.swap.availablepairs.entity.converters.LoadingAccountTokenItemConverter
import com.tangem.features.onramp.swap.availablepairs.entity.converters.LoadingTokenListItemConverter
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMData
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal class SetLoadingAccountTokenListTransformer(
    appCurrency: AppCurrency,
    private val accountList: List<AccountStatus>,
    private val isAccountsMode: Boolean,
) : TokenListUMTransformer {

    private val accountListItemConverter = LoadingAccountTokenItemConverter(appCurrency)

    override fun transform(prevState: TokenListUM): TokenListUM {
        return prevState.copy(
            availableItems = persistentListOf(),
            unavailableItems = persistentListOf(),
            tokensListData = if (isAccountsMode) {
                TokenListUMData.AccountList(
                    tokensList = accountListItemConverter.convertList(
                        accountList.filterIsInstance<AccountStatus.Crypto.Portfolio>(),
                    ).toPersistentList(),
                )
            } else {
                TokenListUMData.TokenList(
                    tokensList = accountList.flatMap { account ->
                        when (account) {
                            is AccountStatus.Crypto.Portfolio -> LoadingTokenListItemConverter.convertList(
                                account.tokenList.flattenCurrencies().map(CryptoCurrencyStatus::currency),
                            )
                            is AccountStatus.Payment -> TODO("[REDACTED_JIRA]")
                        }
                    }.toPersistentList(),
                )
            },
            warning = null,
        )
    }
}