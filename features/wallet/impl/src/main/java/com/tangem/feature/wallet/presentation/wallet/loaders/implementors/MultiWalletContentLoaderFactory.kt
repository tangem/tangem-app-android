package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateHolderV2
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
internal class MultiWalletContentLoaderFactory @Inject constructor(
    private val stateHolder: WalletStateHolderV2,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
) {

    fun create(
        userWallet: UserWallet,
        appCurrency: AppCurrency,
        clickIntents: WalletClickIntentsV2,
    ): WalletContentLoader {
        return MultiWalletContentLoader(
            userWallet = userWallet,
            appCurrency = appCurrency,
            clickIntents = clickIntents,
            stateHolder = stateHolder,
            tokenListAnalyticsSender = tokenListAnalyticsSender,
            walletWithFundsChecker = walletWithFundsChecker,
            getTokenListUseCase = getTokenListUseCase,
            getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
        )
    }
}
