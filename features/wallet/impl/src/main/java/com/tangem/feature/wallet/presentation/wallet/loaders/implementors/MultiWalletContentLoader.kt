package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.subscribers.MultiWalletWarningsSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.TokenListSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletConnectNetworksSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletSubscriber
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2

@Suppress("LongParameterList")
internal class MultiWalletContentLoader(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntentsV2,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val reduxStateHolder: ReduxStateHolder,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> {
        return listOf(
            TokenListSubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                tokenListAnalyticsSender = tokenListAnalyticsSender,
                walletWithFundsChecker = walletWithFundsChecker,
                getTokenListUseCase = getTokenListUseCase,
                getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            ),
            MultiWalletWarningsSubscriber(
                userWalletId = userWallet.walletId,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
                walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
            ),
            WalletConnectNetworksSubscriber(
                userWallet = userWallet,
                getTokenListUseCase = getTokenListUseCase,
                reduxStateHolder = reduxStateHolder,
            ),
        )
    }
}