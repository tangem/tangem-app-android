package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.RunPolkadotAccountHealthCheckUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListTransformer
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
    private val runPolkadotAccountHealthCheckUseCase: RunPolkadotAccountHealthCheckUseCase,
) : WalletSubscriber() {

    private val sendAnalyticsJobHolder = JobHolder()
    private val onTokenListReceivedJobHolder = JobHolder()

    protected abstract fun tokenListFlow(): MaybeTokenListFlow

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return combine(
            flow = tokenListFlow()
                .onEach { maybeTokenList ->
                    coroutineScope.launch {
                        sendTokenListAnalytics(maybeTokenList)
                    }.saveIn(sendAnalyticsJobHolder)
                }
                .distinctUntilChanged()
                .onEach { maybeTokenList ->
                    coroutineScope.launch {
                        onTokenListReceived(maybeTokenList)
                    }.saveIn(onTokenListReceivedJobHolder)

                    coroutineScope.launch { startCheck(maybeTokenList) }
                },
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
            },
        )
    }

    private suspend fun startCheck(maybeTokenList: Either<TokenListError, TokenList>) {
        // Run Polkadot account health check
        maybeTokenList.getOrNull()?.let { tokenList ->
            val cryptoCurrencies = when (tokenList) {
                is TokenList.GroupedByNetwork -> tokenList.groups.flatMap(NetworkGroup::currencies)
                is TokenList.Ungrouped -> tokenList.currencies
                is TokenList.Empty -> emptyList()
            }

            cryptoCurrencies.forEach {
                runPolkadotAccountHealthCheckUseCase(userWallet.walletId, it.currency.network)
            }
        }
    }

    protected open suspend fun onTokenListReceived(maybeTokenList: Either<TokenListError, TokenList>) {
        /* no-op */
    }

    private suspend fun sendTokenListAnalytics(maybeTokenList: Either<TokenListError, TokenList>) {
        val displayedState = stateHolder.getWalletStateIfSelected(userWallet.walletId)

        tokenListAnalyticsSender.send(
            displayedUiState = displayedState,
            userWallet = userWallet,
            tokenList = maybeTokenList.getOrElse { return },
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