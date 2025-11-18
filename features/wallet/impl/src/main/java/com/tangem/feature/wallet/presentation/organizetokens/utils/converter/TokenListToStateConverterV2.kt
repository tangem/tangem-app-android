package com.tangem.feature.wallet.presentation.organizetokens.utils.converter

import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListUM
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getGroupPlaceholder
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.uniteItemsV2
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items.OrganizedTokenListConverter
import com.tangem.utils.converter.Converter
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class TokenListToStateConverterV2(
    private val accountStatusList: AccountStatusList,
    private val isAccountsMode: Boolean,
    private val appCurrency: AppCurrency,
) : Transformer<OrganizeTokensState> {

    private val accountListItemConverter by lazy {
        AccountTokenItemConverter(appCurrency = appCurrency, isAccountsMode)
    }

    override fun transform(prevState: OrganizeTokensState): OrganizeTokensState {
        val tokenListUM = accountListItemConverter.convert(accountStatusList)

        return prevState.copy(
            tokenListUM = tokenListUM,
            header = prevState.header.copy(
                isEnabled = tokenListUM !is OrganizeTokensListUM.EmptyList,
                isSortedByBalance = accountStatusList.sortType == TokensSortType.BALANCE,
                isGrouped = accountStatusList.groupType == TokensGroupType.NETWORK,
            ),
            actions = prevState.actions.copy(
                canApply = tokenListUM !is OrganizeTokensListUM.EmptyList,
            ),
        )
    }
}

internal class AccountTokenItemConverter(
    private val appCurrency: AppCurrency,
    private val isAccountsMode: Boolean,
) : Converter<AccountStatusList, OrganizeTokensListUM> {

    private val organizedTokenListConverter by lazy {
        OrganizedTokenListConverter(appCurrency)
    }

    override fun convert(value: AccountStatusList): OrganizeTokensListUM {
        val isGrouping = value.groupType == TokensGroupType.NETWORK
        return if (isAccountsMode) {
            OrganizeTokensListUM.AccountList(
                isGrouped = isGrouping,
                items = value.accountStatuses
                    .asSequence()
                    .filterIsInstance<AccountStatus.CryptoPortfolio>()
                    .flatMap { accountStatus ->
                        if (accountStatus.tokenList != TokenList.Empty) {
                            buildList {
                                add(
                                    DraggableItem.Portfolio(
                                        tokenItemState = AccountCryptoPortfolioItemStateConverter(
                                            appCurrency = appCurrency,
                                            account = accountStatus.account,
                                        ).convert(TotalFiatBalance.Loading),
                                    ),
                                )
                                if (isGrouping) {
                                    add(getGroupPlaceholder(accountId = accountStatus.accountId.value, index = -1))
                                }
                                addAll(organizedTokenListConverter.convert(accountStatus))
                            }
                        } else {
                            emptyList()
                        }
                    }.toList()
                    .uniteItemsV2(true).toPersistentList(),
            )
        } else {
            OrganizeTokensListUM.TokensList(
                isGrouped = isGrouping,
                items = buildList {
                    if (isGrouping) {
                        add(getGroupPlaceholder(accountId = value.mainAccount.accountId.value, index = -1))
                    }
                    addAll(organizedTokenListConverter.convert(value.mainAccount))
                }.uniteItemsV2(false)
                    .toPersistentList(),
            )
        }
    }
}