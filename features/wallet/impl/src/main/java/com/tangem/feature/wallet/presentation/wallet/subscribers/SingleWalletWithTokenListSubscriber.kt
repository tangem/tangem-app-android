package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.tokens.GetCardTokensListUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2

@Suppress("LongParameterList")
internal class SingleWalletWithTokenListSubscriber(
    private val userWallet: UserWallet,
    private val getCardTokensListUseCase: GetCardTokensListUseCase,
    stateHolder: WalletStateController,
    clickIntents: WalletClickIntentsV2,
    tokenListAnalyticsSender: TokenListAnalyticsSender,
    walletWithFundsChecker: WalletWithFundsChecker,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : BasicTokenListSubscriber(
    userWallet = userWallet,
    stateHolder = stateHolder,
    clickIntents = clickIntents,
    tokenListAnalyticsSender = tokenListAnalyticsSender,
    walletWithFundsChecker = walletWithFundsChecker,
    getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
) {

    override fun tokenListFlow(): MaybeTokenListFlow = getCardTokensListUseCase(userWallet.walletId)
}