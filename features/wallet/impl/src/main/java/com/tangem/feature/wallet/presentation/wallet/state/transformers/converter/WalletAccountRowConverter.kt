package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.account.AccountRowStateConverter
import com.tangem.common.ui.account.toAvatarState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.feature.wallet.presentation.wallet.state.model.TokensListItemUM2
import kotlinx.collections.immutable.ImmutableList

internal class WalletAccountRowConverter(
    appCurrency: AppCurrency,
    private val expandedAccounts: Set<AccountId>,
    private val onAccountCollapseClick: (account: Account) -> Unit,
    private val onAccountExpandClick: (account: Account) -> Unit,
    private val onManageTokensClick: (accountId: AccountId) -> Unit,
) {

    private val accountRowStateConverter = AccountRowStateConverter(appCurrency)

    fun convert(
        value: AccountStatus.CryptoPortfolio,
        tokenList: ImmutableList<TokensListItemUM2>,
    ): TokensListItemUM2.AccountV2 {
        val account = value.account
        val isExpanded = expandedAccounts.contains(account.accountId)

        return TokensListItemUM2.AccountV2(
            state = accountRowStateConverter.convert(
                value = value,
                onClick = { if (isExpanded) onAccountCollapseClick(account) else onAccountExpandClick(account) },
            ),
            avatar = account.toAvatarState(),
            isExpanded = isExpanded,
            shouldShowCompactBalance = account.tokensCount > 0,
            onEmptyClick = { onManageTokensClick(account.accountId) },
            tokenList = tokenList,
        )
    }
}