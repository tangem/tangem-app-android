package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.usecase.StakingApyFlowUseCase
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.MultiWalletTokenListStore
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import javax.inject.Inject

// TODO: Refactor
@Suppress("LongParameterList")
@ModelScoped
internal class SingleWalletWithTokenContentLoaderFactory @Inject constructor(
    private val stateHolder: WalletStateController,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val tokenListStore: MultiWalletTokenListStore,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val accountDependencies: AccountDependencies,
    private val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase,
    private val stakingApyFlowUseCase: StakingApyFlowUseCase,
) {

    fun create(userWallet: UserWallet.Cold, clickIntents: WalletClickIntents): SingleWalletWithTokenContentLoader {
        return SingleWalletWithTokenContentLoader(
            userWallet = userWallet,
            clickIntents = clickIntents,
            stateHolder = stateHolder,
            tokenListAnalyticsSender = tokenListAnalyticsSender,
            walletWithFundsChecker = walletWithFundsChecker,
            getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
            tokenListStore = tokenListStore,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
            walletWarningsSingleEventSender = walletWarningsSingleEventSender,
            getStoryContentUseCase = getStoryContentUseCase,
            shouldSaveUserWalletsUseCase = shouldSaveUserWalletsUseCase,
            accountDependencies = accountDependencies,
            yieldSupplyApyFlowUseCase = yieldSupplyApyFlowUseCase,
            stakingApyFlowUseCase = stakingApyFlowUseCase,
        )
    }
}