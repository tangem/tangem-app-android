package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

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
import com.tangem.feature.wallet.presentation.wallet.subscribers.MultiWalletTokenListSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.MultiWalletWarningsSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletSubscriber
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents

@Suppress("LongParameterList")
internal class MultiWalletContentLoader(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val tokenListStore: MultiWalletTokenListStore,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val runPolkadotAccountHealthCheckUseCase: RunPolkadotAccountHealthCheckUseCase,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> {
        return listOfNotNull(
            MultiWalletTokenListSubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                tokenListAnalyticsSender = tokenListAnalyticsSender,
                walletWithFundsChecker = walletWithFundsChecker,
                tokenListStore = tokenListStore,
                getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
                applyTokenListSortingUseCase = applyTokenListSortingUseCase,
                runPolkadotAccountHealthCheckUseCase = runPolkadotAccountHealthCheckUseCase,
            ),
            MultiWalletWarningsSubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
                walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
                walletWarningsSingleEventSender = walletWarningsSingleEventSender,
            ),
        )
    }
}
