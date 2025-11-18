package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.usecase.StakingApyFlowUseCase
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.MultiWalletTokenListStore
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

@Suppress("LongParameterList")
internal class MultiWalletTokenListSubscriber(
    override val userWallet: UserWallet,
    private val tokenListStore: MultiWalletTokenListStore,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
    override val stateHolder: WalletStateController,
    override val clickIntents: WalletClickIntents,
    override val tokenListAnalyticsSender: TokenListAnalyticsSender,
    override val walletWithFundsChecker: WalletWithFundsChecker,
    override val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    override val accountDependencies: AccountDependencies,
    override val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase,
    override val stakingApyFlowUseCase: StakingApyFlowUseCase,
) : BasicTokenListSubscriber() {

    override fun tokenListFlow(coroutineScope: CoroutineScope): LceFlow<TokenListError, TokenList> {
        tokenListStore.addIfNot(userWallet.walletId, coroutineScope)

        return tokenListStore.getOrThrow(userWallet.walletId)
    }

    override fun accountListFlow(coroutineScope: CoroutineScope): Flow<AccountStatusList> {
        val params = SingleAccountStatusListProducer.Params(userWallet.walletId)
        return accountDependencies.singleAccountStatusListSupplier(params)
    }

    override suspend fun onTokenListReceived(maybeTokenList: Lce<TokenListError, TokenList>) {
        updateSortingIfNeeded(maybeTokenList)
    }

    private suspend fun updateSortingIfNeeded(maybeTokenList: Lce<*, TokenList>) {
        val tokenList = getTokenList(maybeTokenList) ?: return

        applyTokenListSortingUseCase(
            userWalletId = userWallet.walletId,
            sortedTokensIds = getCurrenciesIds(tokenList),
            isGroupedByNetwork = tokenList is TokenList.GroupedByNetwork,
            isSortedByBalance = tokenList.sortedBy == TokensSortType.BALANCE,
        )
    }

    private fun getTokenList(lce: Lce<*, TokenList>): TokenList? {
        val tokenList = lce.getOrNull(isPartialContentAccepted = false)
            ?: return null

        return tokenList.takeIf {
            tokenList.totalFiatBalance is TotalFiatBalance.Loaded &&
                tokenList.sortedBy == TokensSortType.BALANCE
        }
    }

    private fun getCurrenciesIds(tokenList: TokenList): List<CryptoCurrency.ID> {
        return tokenList.flattenCurrencies().map { it.currency.id }
    }
}