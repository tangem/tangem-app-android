package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

internal typealias MaybeTokenListFlow = Flow<Either<TokenListError, TokenList>>

@Suppress("LongParameterList")
internal abstract class BasicTokenListSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntentsV2,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : WalletSubscriber() {

    protected abstract fun tokenListFlow(): MaybeTokenListFlow

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return combine(
            flow = tokenListFlow()
                .onEach {
                    val displayedState = stateHolder.getWalletStateIfSelected(userWallet.walletId)

                    tokenListAnalyticsSender.send(displayedState, userWallet, it.getOrElse { return@onEach })
                }
                .distinctUntilChanged(),
            flow2 = getSelectedAppCurrencyUseCase().distinctUntilChanged(),
            transform = { maybeTokenList, maybeAppCurrency ->
                updateContent(maybeTokenList, maybeAppCurrency.getOrElse { AppCurrency.Default })
                walletWithFundsChecker.check(maybeTokenList)
            },
        )
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