package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateHolderV2
import com.tangem.feature.wallet.presentation.wallet.subscribers.MultiWalletWarningsSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.TokenListSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletSubscriber
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2

@Suppress("LongParameterList")
internal class MultiWalletContentLoader(
    private val userWallet: UserWallet,
    private val appCurrency: AppCurrency,
    private val clickIntents: WalletClickIntentsV2,
    private val stateHolder: WalletStateHolderV2,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> {
        return listOf(
            TokenListSubscriber(
                userWallet = userWallet,
                appCurrency = appCurrency,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                tokenListAnalyticsSender = tokenListAnalyticsSender,
                walletWithFundsChecker = walletWithFundsChecker,
                getTokenListUseCase = getTokenListUseCase,
            ),
            MultiWalletWarningsSubscriber(
                userWalletId = userWallet.walletId,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
            ),
        )
    }
}