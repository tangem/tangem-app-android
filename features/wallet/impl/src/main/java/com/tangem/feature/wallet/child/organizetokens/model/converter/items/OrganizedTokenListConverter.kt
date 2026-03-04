package com.tangem.feature.wallet.child.organizetokens.model.converter.items

import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal class OrganizedTokenListConverter(
    private val appCurrency: AppCurrency,
) : Converter<AccountStatus.CryptoPortfolio, PersistentList<DraggableItem>> {

    private val tokensConverter by lazy { CryptoCurrencyToDraggableItemConverter(appCurrency) }
    private val groupsConverter by lazy {
        NetworkGroupToDraggableItemsConverter(tokensConverter)
    }

    override fun convert(value: AccountStatus.CryptoPortfolio): PersistentList<DraggableItem> {
        return when (val tokenList = value.tokenList) {
            is TokenList.GroupedByNetwork -> groupsConverter.convertList(
                tokenList.groups.map { value.account to it },
            )
                .flatten()
                .toPersistentList()

            is TokenList.Ungrouped -> tokensConverter.convertList(
                value.flattenCurrencies().map { cryptoCurrencyStatus ->
                    AccountCryptoCurrencyStatus(
                        account = value.account,
                        status = cryptoCurrencyStatus,
                    )
                },
            ).toPersistentList()

            is TokenList.Empty -> persistentListOf()
        }
    }
}