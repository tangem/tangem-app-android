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
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListTransformer
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

internal typealias MaybeTokenListFlow = Flow<Either<TokenListError, TokenList>>

@Suppress("LongParameterList")
internal abstract class BasicTokenListSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : WalletSubscriber() {

    protected abstract fun tokenListFlow(): MaybeTokenListFlow

    protected open suspend fun onTokenListReceived(tokenList: TokenList) { /* no-op */
    }

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return combine(
            flow = tokenListFlow()
                .onEach { maybeTokenList ->
                    val displayedState = stateHolder.getWalletStateIfSelected(userWallet.walletId)

                    tokenListAnalyticsSender.send(
                        displayedUiState = displayedState,
                        userWallet = userWallet,
                        tokenList = maybeTokenList.getOrElse { return@onEach },
                    )
                }
                .distinctUntilChanged(),
            flow2 = getSelectedAppCurrencyUseCase().distinctUntilChanged(),
            transform = { maybeTokenList, maybeAppCurrency ->
                val tokenList = maybeTokenList.getOrElse { e ->
                    Timber.e("Failed to load token list: $e")
                    SetTokenListErrorTransformer(userWallet.walletId, e)
                    return@combine
                }
                val appCurrency = maybeAppCurrency.getOrElse { e ->
                    Timber.e("Failed to load app currency: $e")
                    AppCurrency.Default
                }

                updateContent(tokenList, appCurrency)
                walletWithFundsChecker.check(tokenList)
                onTokenListReceived(tokenList)
            },
        )
    }

    private fun updateContent(tokenList: TokenList, appCurrency: AppCurrency) {
        stateHolder.update(
            SetTokenListTransformer(
                tokenList = tokenList,
                userWallet = userWallet,
                appCurrency = appCurrency,
                clickIntents = clickIntents,
            ),
        )
    }
}