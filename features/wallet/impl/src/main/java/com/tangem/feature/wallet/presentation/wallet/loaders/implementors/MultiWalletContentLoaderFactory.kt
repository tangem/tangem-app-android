package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.RunPolkadotAccountHealthCheckUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.MultiWalletTokenListStore
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@Suppress("LongParameterList")
@ViewModelScoped
internal class MultiWalletContentLoaderFactory @Inject constructor(
    private val stateHolder: WalletStateController,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val tokenListStore: MultiWalletTokenListStore,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
    private val runPolkadotAccountHealthCheckUseCase: RunPolkadotAccountHealthCheckUseCase,
    private val deepLinksRegistry: DeepLinksRegistry,
) {

    fun create(userWallet: UserWallet, clickIntents: WalletClickIntents): WalletContentLoader {
        return MultiWalletContentLoader(
            userWallet = userWallet,
            clickIntents = clickIntents,
            stateHolder = stateHolder,
            tokenListAnalyticsSender = tokenListAnalyticsSender,
            walletWithFundsChecker = walletWithFundsChecker,
            tokenListStore = tokenListStore,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
            walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
            walletWarningsSingleEventSender = walletWarningsSingleEventSender,
            applyTokenListSortingUseCase = applyTokenListSortingUseCase,
            runPolkadotAccountHealthCheckUseCase = runPolkadotAccountHealthCheckUseCase,
            deepLinksRegistry = deepLinksRegistry,
        )
    }
}