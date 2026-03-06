package com.tangem.feature.wallet.child.organizetokens.model.converter

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeRowItemUM
import com.tangem.feature.wallet.child.organizetokens.model.common.getGroupPlaceholder
import com.tangem.feature.wallet.child.organizetokens.model.common.uniteItems
import com.tangem.feature.wallet.child.organizetokens.model.converter.items.OrganizeAccountItemConverter
import com.tangem.feature.wallet.child.organizetokens.model.converter.items.OrganizeNetworkItemConverter
import com.tangem.feature.wallet.child.organizetokens.model.converter.items.OrganizeTokenItemConverter
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.addIf
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal class OrganizeTokensListConverter(
    private val isAccountsMode: Boolean,
    private val appCurrency: AppCurrency,
) : Converter<AccountStatusList, PersistentList<OrganizeRowItemUM>> {

    private val accountItemConverter by lazy(LazyThreadSafetyMode.NONE) {
        OrganizeAccountItemConverter(appCurrency)
    }

    private val tokenItemConverter by lazy(LazyThreadSafetyMode.NONE) {
        OrganizeTokenItemConverter(appCurrency)
    }

    override fun convert(value: AccountStatusList): PersistentList<OrganizeRowItemUM> {
        return value.accountStatuses
            .asSequence()
            .filterCryptoPortfolio()
            .flatMap { accountStatus ->
                buildList {
                    addIf(
                        condition = isAccountsMode,
                        create = { accountItemConverter.convert(accountStatus) },
                    )
                    when (val tokenList = accountStatus.tokenList) {
                        is TokenList.Ungrouped -> addAll(
                            elements = tokenItemConverter.convertList(
                                input = tokenList.currencies.mapToAccountCryptoCurrencyStatus(accountStatus),
                            ),
                        )
                        is TokenList.GroupedByNetwork -> {
                            addIf(
                                condition = isAccountsMode,
                                create = { getGroupPlaceholder(-1, accountStatus.accountId.value) },
                            )
                            tokenList.groups.asSequence().forEachIndexed { index, (groupNetwork, currencies) ->
                                add(OrganizeNetworkItemConverter.convert(accountStatus.accountId to groupNetwork))
                                addAll(
                                    elements = tokenItemConverter.convertList(
                                        input = currencies.mapToAccountCryptoCurrencyStatus(accountStatus),
                                    ),
                                )
                                add(getGroupPlaceholder(index, accountStatus.accountId.value))
                            }
                        }
                        TokenList.Empty -> Unit
                    }
                }
            }.toList()
            .uniteItems(isAccountsMode).toPersistentList()
    }

    private fun List<CryptoCurrencyStatus>.mapToAccountCryptoCurrencyStatus(
        accountStatus: AccountStatus.CryptoPortfolio,
    ): List<AccountCryptoCurrencyStatus> {
        return map { cryptoCurrencyStatus ->
            AccountCryptoCurrencyStatus(
                account = accountStatus.account,
                status = cryptoCurrencyStatus,
            )
        }
    }
}