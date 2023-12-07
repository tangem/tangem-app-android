package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.Either
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.GetCardTokensListUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetTokenListErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetTokenListTransformer
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

@Suppress("LongParameterList")
internal class SingleWalletWithTokenListSubscriber(
    private val userWallet: UserWallet,
    private val appCurrency: AppCurrency,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntentsV2,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getCardTokensListUseCase: GetCardTokensListUseCase,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<Either<TokenListError, TokenList>> {
        return getCardTokensListUseCase(userWalletId = userWallet.walletId)
            .conflate()
            .distinctUntilChanged()
            .onEach(::updateContent)
            .onEach { tokenListAnalyticsSender.send(userWallet, maybeTokenList = it) }
            .onEach(walletWithFundsChecker::check)
    }

    private fun updateContent(maybeTokenList: Either<TokenListError, TokenList>) {
        stateHolder.update(
            maybeTokenList.fold(
                ifLeft = { SetTokenListErrorTransformer(userWalletId = userWallet.walletId, error = it) },
                ifRight = {
                    SetTokenListTransformer(
                        tokenList = it,
                        userWallet = userWallet,
                        appCurrency = appCurrency,
                        clickIntents = clickIntents,
                    )
                },
            ),
        )
    }
}