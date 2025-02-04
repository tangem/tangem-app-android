package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.RunPolkadotAccountHealthCheckUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.MultiWalletTokenListStore
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.coroutines.CoroutineScope

@Suppress("LongParameterList")
internal class MultiWalletTokenListSubscriber(
    private val userWallet: UserWallet,
    private val tokenListStore: MultiWalletTokenListStore,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
    stateHolder: WalletStateController,
    clickIntents: WalletClickIntents,
    tokenListAnalyticsSender: TokenListAnalyticsSender,
    walletWithFundsChecker: WalletWithFundsChecker,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    runPolkadotAccountHealthCheckUseCase: RunPolkadotAccountHealthCheckUseCase,
    deepLinksRegistry: DeepLinksRegistry,
) : BasicTokenListSubscriber(
    userWallet = userWallet,
    stateHolder = stateHolder,
    clickIntents = clickIntents,
    tokenListAnalyticsSender = tokenListAnalyticsSender,
    walletWithFundsChecker = walletWithFundsChecker,
    getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
    runPolkadotAccountHealthCheckUseCase = runPolkadotAccountHealthCheckUseCase,
    deepLinksRegistry = deepLinksRegistry,
) {

    override fun tokenListFlow(coroutineScope: CoroutineScope): LceFlow<TokenListError, TokenList> {
        tokenListStore.addIfNot(userWallet.walletId, coroutineScope)

        return tokenListStore.getOrThrow(userWallet.walletId)
    }

    override suspend fun onTokenListReceived(maybeTokenList: Lce<TokenListError, TokenList>) {
        updateSortingIfNeeded(maybeTokenList)
        super.onTokenListReceived(maybeTokenList)
    }

    private suspend fun updateSortingIfNeeded(maybeTokenList: Lce<*, TokenList>) {
        val tokenList = getTokenList(maybeTokenList) ?: return

        applyTokenListSortingUseCase(
            userWalletId = userWallet.walletId,
            sortedTokensIds = getCurrenciesIds(tokenList),
            isGroupedByNetwork = tokenList is TokenList.GroupedByNetwork,
            isSortedByBalance = tokenList.sortedBy == TokenList.SortType.BALANCE,
        )
    }

    private fun getTokenList(lce: Lce<*, TokenList>): TokenList? {
        val tokenList = lce.getOrNull(isPartialContentAccepted = false)
            ?: return null

        return tokenList.takeIf {
            tokenList.totalFiatBalance is TotalFiatBalance.Loaded &&
                tokenList.sortedBy == TokenList.SortType.BALANCE
        }
    }

    private fun getCurrenciesIds(tokenList: TokenList): List<CryptoCurrency.ID> {
        return tokenList.flattenCurrencies().map { it.currency.id }
    }
}