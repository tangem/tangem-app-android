package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.utils.getOrElse
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

    protected abstract fun tokenListFlow(): LceFlow<TokenListError, TokenList>

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
                val appCurrency = maybeAppCurrency.getOrElse { e ->
                    Timber.e("Failed to load app currency: $e")
                    AppCurrency.Default
                }

                val tokenList = maybeTokenList.getOrElse(
                    ifLoading = { maybeContent ->
                        val isRefreshing = stateHolder.getWalletState(userWallet.walletId)
                            ?.pullToRefreshConfig
                            ?.isRefreshing
                            ?: false

                        maybeContent
                            ?.takeIf { !isRefreshing }
                            ?: return@combine
                    },
                    ifError = { e ->
                        Timber.e("Failed to load token list: $e")
                        stateHolder.update(
                            SetTokenListErrorTransformer(
                                selectedWallet = userWallet,
                                error = e,
                                appCurrency = appCurrency,
                            ),
                        )
                        return@combine
                    },
                )

                updateContent(tokenList, appCurrency)
                walletWithFundsChecker.check(tokenList)
            },
        )
    }

    private suspend fun startCheck(maybeTokenList: Lce<TokenListError, TokenList>) {
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

    protected open suspend fun onTokenListReceived(maybeTokenList: Lce<TokenListError, TokenList>) {
        /* no-op */
    }

    private suspend fun sendTokenListAnalytics(maybeTokenList: Lce<TokenListError, TokenList>) {
        val displayedState = stateHolder.getWalletStateIfSelected(userWallet.walletId)

        tokenListAnalyticsSender.send(
            displayedUiState = displayedState,
            userWallet = userWallet,
            tokenList = maybeTokenList.getOrNull() ?: return,
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
