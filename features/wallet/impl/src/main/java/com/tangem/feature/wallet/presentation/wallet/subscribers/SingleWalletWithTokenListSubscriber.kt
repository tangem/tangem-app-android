package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetShouldShowMainPromoUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.MultiWalletTokenListStore
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import kotlinx.coroutines.CoroutineScope

@Deprecated("Use SingleWalletWithTokenSubscriber instead")
@Suppress("LongParameterList")
internal class SingleWalletWithTokenListSubscriber(
    private val userWallet: UserWallet.Cold,
    private val tokenListStore: MultiWalletTokenListStore,
    stateHolder: WalletStateController,
    clickIntents: WalletClickIntents,
    tokenListAnalyticsSender: TokenListAnalyticsSender,
    walletWithFundsChecker: WalletWithFundsChecker,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase,
    stakingAvailabilityListUseCase: StakingAvailabilityListUseCase,
    yieldSupplyGetShouldShowMainPromoUseCase: YieldSupplyGetShouldShowMainPromoUseCase,
) : BasicTokenListSubscriber(
    userWallet = userWallet,
    stateHolder = stateHolder,
    clickIntents = clickIntents,
    tokenListAnalyticsSender = tokenListAnalyticsSender,
    walletWithFundsChecker = walletWithFundsChecker,
    getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
    yieldSupplyApyFlowUseCase = yieldSupplyApyFlowUseCase,
    stakingAvailabilityListUseCase = stakingAvailabilityListUseCase,
    yieldSupplyGetShouldShowMainPromoUseCase = yieldSupplyGetShouldShowMainPromoUseCase,
) {

    override fun tokenListFlow(coroutineScope: CoroutineScope): LceFlow<TokenListError, TokenList> {
        tokenListStore.addIfNot(userWallet.walletId, coroutineScope)

        return tokenListStore.getOrThrow(userWallet.walletId)
    }

    override suspend fun onTokenListReceived(maybeTokenList: Lce<TokenListError, TokenList>) = Unit
}