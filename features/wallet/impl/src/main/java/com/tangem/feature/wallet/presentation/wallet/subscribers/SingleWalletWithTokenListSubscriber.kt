package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
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
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class SingleWalletWithTokenListSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntentsV2,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getCardTokensListUseCase: GetCardTokensListUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<Pair<Either<TokenListError, TokenList>, AppCurrency>> {
        return combine(
            flow = getCardTokensListUseCase(userWalletId = userWallet.walletId)
                .conflate()
                .distinctUntilChanged(),
            flow2 = getSelectedAppCurrencyUseCase()
                .conflate()
                .distinctUntilChanged()
                .map { maybeAppCurrency -> maybeAppCurrency.getOrElse { AppCurrency.Default } },
            transform = { tokenList, appCurrency -> tokenList to appCurrency },
        )
            .onEach { (maybeTokenList, appCurrency) ->
                updateContent(maybeTokenList, appCurrency)
                tokenListAnalyticsSender.send(userWallet, maybeTokenList)
                walletWithFundsChecker.check(maybeTokenList)
            }
    }

    private fun updateContent(maybeTokenList: Either<TokenListError, TokenList>, appCurrency: AppCurrency) {
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