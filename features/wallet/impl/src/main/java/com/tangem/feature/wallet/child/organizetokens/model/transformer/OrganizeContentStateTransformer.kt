package com.tangem.feature.wallet.child.organizetokens.model.transformer

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensUM
import com.tangem.feature.wallet.child.organizetokens.model.converter.OrganizeTokensListConverter
import com.tangem.utils.transformer.Transformer

internal class OrganizeContentStateTransformer(
    private val accountStatusList: AccountStatusList,
    private val isAccountsMode: Boolean,
    private val appCurrency: AppCurrency,
) : Transformer<OrganizeTokensUM> {

    private val tokenListConverter by lazy(LazyThreadSafetyMode.NONE) {
        OrganizeTokensListConverter(
            isAccountsMode = isAccountsMode,
            appCurrency = appCurrency,
        )
    }

    override fun transform(prevState: OrganizeTokensUM): OrganizeTokensUM {
        val isGrouping = accountStatusList.groupType == TokensGroupType.NETWORK
        val isSortedByBalance = accountStatusList.sortType == TokensSortType.BALANCE

        return prevState.copy(
            isGrouped = isGrouping,
            isAccountsMode = isAccountsMode,
            tokenList = tokenListConverter.convert(value = accountStatusList),
            organizeMenuUM = prevState.organizeMenuUM.copy(
                isEnabled = prevState.tokenList.isNotEmpty(),
                isSortedByBalance = isSortedByBalance,
                isGrouped = isGrouping,
            ),
        )
    }
}