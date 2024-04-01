package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrency
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

    override fun tokenListFlow(): MaybeTokenListFlow = getTokenListUseCase.launch(userWallet.walletId)

    override suspend fun onTokenListReceived(maybeTokenList: Either<TokenListError, TokenList>) {
        // TODO disabled for 5.7.2 because of potential critical
        // updateSortingIfNeeded(maybeTokenList)
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun updateSortingIfNeeded(maybeTokenList: Either<TokenListError, TokenList>) {
        val tokenList = maybeTokenList.getOrElse { return }
        if (!checkNeedSorting(tokenList)) return

        applyTokenListSortingUseCase(
            userWalletId = userWallet.walletId,
            sortedTokensIds = getCurrenciesIds(tokenList),
            isGroupedByNetwork = tokenList is TokenList.GroupedByNetwork,
            isSortedByBalance = tokenList.sortedBy == TokenList.SortType.BALANCE,
        )
    }

    private fun checkNeedSorting(tokenList: TokenList): Boolean {
        return tokenList.totalFiatBalance !is TokenList.FiatBalance.Loading &&
            tokenList.sortedBy == TokenList.SortType.BALANCE
    }

    private fun getCurrenciesIds(tokenList: TokenList): List<CryptoCurrency.ID> {
        return when (tokenList) {
            is TokenList.GroupedByNetwork -> tokenList.groups.flatMap { group ->
                group.currencies.map { it.currency.id }
            }
            is TokenList.Ungrouped -> tokenList.currencies.map { it.currency.id }
            is TokenList.Empty -> emptyList()
        }
    }
}