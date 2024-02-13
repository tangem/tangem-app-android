package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents

@Suppress("LongParameterList")
internal class MultiWalletTokenListSubscriber(
    private val userWallet: UserWallet,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
    stateHolder: WalletStateController,
    clickIntents: WalletClickIntents,
    tokenListAnalyticsSender: TokenListAnalyticsSender,
    walletWithFundsChecker: WalletWithFundsChecker,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : BasicTokenListSubscriber(
    userWallet = userWallet,
    stateHolder = stateHolder,
    clickIntents = clickIntents,
    tokenListAnalyticsSender = tokenListAnalyticsSender,
    walletWithFundsChecker = walletWithFundsChecker,
    getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
) {

    override fun tokenListFlow(): MaybeTokenListFlow = getTokenListUseCase(userWallet.walletId)

    override suspend fun onTokenListReceived(tokenList: TokenList) {
        updateSortingIfNeeded(tokenList)
    }

    private suspend fun updateSortingIfNeeded(tokenList: TokenList) {
        if (tokenList.totalFiatBalance is TokenList.FiatBalance.Loading ||
            tokenList.sortedBy == TokenList.SortType.NONE
        ) {
            return
        }

        applyTokenListSortingUseCase(
            userWalletId = userWallet.walletId,
            sortedTokensIds = when (tokenList) {
                is TokenList.GroupedByNetwork -> tokenList.groups.flatMap { group ->
                    group.currencies.map { it.currency.id }
                }
                is TokenList.Ungrouped -> tokenList.currencies.map { it.currency.id }
                is TokenList.Empty -> return
            },
            isGroupedByNetwork = tokenList is TokenList.GroupedByNetwork,
            isSortedByBalance = true,
        )
    }
}
