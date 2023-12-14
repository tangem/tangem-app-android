package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@Suppress("LongParameterList")
@ViewModelScoped
internal class MultiWalletContentLoaderFactory @Inject constructor(
    private val stateHolder: WalletStateController,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val reduxStateHolder: ReduxStateHolder,
) {

    fun create(userWallet: UserWallet, clickIntents: WalletClickIntentsV2): WalletContentLoader {
        return MultiWalletContentLoader(
            userWallet = userWallet,
            clickIntents = clickIntents,
            stateHolder = stateHolder,
            tokenListAnalyticsSender = tokenListAnalyticsSender,
            walletWithFundsChecker = walletWithFundsChecker,
            getTokenListUseCase = getTokenListUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
            reduxStateHolder = reduxStateHolder,
        )
    }
}