package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.tokens.GetCardTokensListUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import javax.inject.Inject

// TODO: Refactor
@Suppress("LongParameterList")
internal class SingleWalletWithTokenContentLoaderFactory @Inject constructor(
    private val stateHolder: WalletStateController,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val getCardTokensListUseCase: GetCardTokensListUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
) {

    fun create(userWallet: UserWallet, clickIntents: WalletClickIntentsV2): SingleWalletWithTokenContentLoader {
        return SingleWalletWithTokenContentLoader(
            userWallet = userWallet,
            clickIntents = clickIntents,
            stateHolder = stateHolder,
            tokenListAnalyticsSender = tokenListAnalyticsSender,
            walletWithFundsChecker = walletWithFundsChecker,
            getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
            getCardTokensListUseCase = getCardTokensListUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
        )
    }
}