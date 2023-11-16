package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.GetCardTokensListUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsUseCase
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateHolderV2
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import javax.inject.Inject

/**
 * @author Andrew Khokhlov on 26/11/2023
 */
internal class SingleWalletWithTokenContentLoaderFactory @Inject constructor(
    private val stateHolder: WalletStateHolderV2,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getCardTokensListUseCase: GetCardTokensListUseCase,
    private val getMultiWalletWarningsUseCase: GetMultiWalletWarningsUseCase,
) {

    fun create(
        userWallet: UserWallet,
        appCurrency: AppCurrency,
        clickIntents: WalletClickIntentsV2,
    ): SingleWalletWithTokenContentLoader {
        return SingleWalletWithTokenContentLoader(
            userWallet = userWallet,
            appCurrency = appCurrency,
            clickIntents = clickIntents,
            stateHolder = stateHolder,
            tokenListAnalyticsSender = tokenListAnalyticsSender,
            walletWithFundsChecker = walletWithFundsChecker,
            getCardTokensListUseCase = getCardTokensListUseCase,
            getMultiWalletWarningsUseCase = getMultiWalletWarningsUseCase,
        )
    }
}
