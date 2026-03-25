package com.tangem.feature.wallet.child.organizetokens.model.converter

import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensListUM
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensState
import com.tangem.feature.wallet.child.organizetokens.model.common.getGroupPlaceholderLegacy
import com.tangem.feature.wallet.child.organizetokens.model.common.uniteItemsLegacy
import com.tangem.feature.wallet.child.organizetokens.model.converter.items.OrganizedTokenListConverter
import com.tangem.utils.converter.Converter
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class TokenListToStateConverter(
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
                    .filterCryptoPortfolio()
                    .flatMap { accountStatus ->
                        if (accountStatus.tokenList != TokenList.Empty) {
                            buildList {
                                add(
                                    DraggableItem.Portfolio(
                                        tokenItemState = AccountCryptoPortfolioItemStateConverter(
                                            appCurrency = appCurrency,
                                            account = accountStatus.account,
                                        ).convert(accountStatus.tokenList.totalFiatBalance),
                                    ),
                                )
                                if (isGrouping) {
                                    add(
                                        getGroupPlaceholderLegacy(
                                            accountId = accountStatus.accountId.value,
                                            index = -1,
                                        ),
                                    )
                                }
                                addAll(organizedTokenListConverter.convert(accountStatus))
                            }
                        } else {
                            emptyList()
                        }
                    }.toList()
                    .uniteItemsLegacy(true).toPersistentList(),
            )
        } else {
            OrganizeTokensListUM.TokensList(
                isGrouped = isGrouping,
                items = buildList {
                    if (isGrouping) {
                        add(getGroupPlaceholderLegacy(accountId = value.mainAccount.accountId.value, index = -1))
                    }
                    addAll(organizedTokenListConverter.convert(value.mainAccount))
                }.uniteItemsLegacy(false)
                    .toPersistentList(),
            )
        }
    }
}