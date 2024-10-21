package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.utils.toLce
import com.tangem.domain.tokens.GetNodlTokenListUseCase
import com.tangem.domain.tokens.RunPolkadotAccountHealthCheckUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

@Suppress("LongParameterList")
internal class SingleWalletWithTokenListSubscriber(
    private val userWallet: UserWallet,
    private val getNodlTokenListUseCase: GetNodlTokenListUseCase,
    stateHolder: WalletStateController,
    clickIntents: WalletClickIntents,
    tokenListAnalyticsSender: TokenListAnalyticsSender,
    walletWithFundsChecker: WalletWithFundsChecker,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    runPolkadotAccountHealthCheckUseCase: RunPolkadotAccountHealthCheckUseCase,
) : BasicTokenListSubscriber(
    userWallet = userWallet,
    stateHolder = stateHolder,
    clickIntents = clickIntents,
    tokenListAnalyticsSender = tokenListAnalyticsSender,
    walletWithFundsChecker = walletWithFundsChecker,
    getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
    runPolkadotAccountHealthCheckUseCase = runPolkadotAccountHealthCheckUseCase,
) {

    override fun tokenListFlow(coroutineScope: CoroutineScope): LceFlow<TokenListError, TokenList> =
        getNodlTokenListUseCase(
            userWallet.walletId,
        )
            .map { it.toLce() }
}
