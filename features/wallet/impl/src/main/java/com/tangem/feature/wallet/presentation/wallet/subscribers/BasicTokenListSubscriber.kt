package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetShouldShowMainPromoUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TokenConverterParams
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal

@Deprecated("Use AccountListSubscriber instead")
@Suppress("LongParameterList")
internal abstract class BasicTokenListSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase,
    private val stakingAvailabilityListUseCase: StakingAvailabilityListUseCase,
    private val yieldSupplyGetShouldShowMainPromoUseCase: YieldSupplyGetShouldShowMainPromoUseCase,
) : WalletSubscriber() {

    private val sendAnalyticsJobHolder = JobHolder()
    private val onTokenListReceivedJobHolder = JobHolder()

    protected abstract fun tokenListFlow(coroutineScope: CoroutineScope): LceFlow<TokenListError, TokenList>

    protected abstract suspend fun onTokenListReceived(maybeTokenList: Lce<TokenListError, TokenList>)

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return combine(
            flow = tokenListFlow(coroutineScope)
                .onEach { maybeTokenList ->
                    coroutineScope.launch {
                        sendTokenListAnalytics(
                            flattenCurrencies = maybeTokenList.getOrNull()?.flattenCurrencies(),
                            totalFiatBalance = maybeTokenList.getOrNull()?.totalFiatBalance,
                        )
                    }.saveIn(sendAnalyticsJobHolder)
                }
                .distinctUntilChanged()
                .onEach { maybeTokenList ->
                    coroutineScope.launch {
                        onTokenListReceived(maybeTokenList)
                    }.saveIn(onTokenListReceivedJobHolder)
                },
            flow2 = appCurrencyFlow(),
            flow3 = yieldSupplyApyFlow(),
            flow4 = yieldSupplyGetShouldShowMainPromoFlow(),
            transform = { maybeTokenList, appCurrency, yieldSupplyApyMap, shouldShowMainPromo ->
                val tokenList = maybeTokenList.getOrElse(
                    ifLoading = { maybeContent ->
                        val isRefreshing = stateHolder.getWalletState(userWallet.walletId)
                            ?.pullToRefreshConfig
                            ?.isRefreshing == true

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

                updateContent(
                    params = TokenConverterParams.Wallet(PortfolioId(userWallet.walletId), tokenList),
                    appCurrency = appCurrency,
                    yieldSupplyApyMap = yieldSupplyApyMap,
                    stakingAvailabilityMap = stakingAvailabilityListUseCase.invokeSync(
                        userWalletId = userWallet.walletId,
                        cryptoCurrencyList = tokenList.flattenCurrencies().map(CryptoCurrencyStatus::currency),
                    ),
                    shouldShowMainPromo = shouldShowMainPromo,
                )

                walletWithFundsChecker.check(tokenList)
            },
        )
    }

    private suspend fun sendTokenListAnalytics(
        flattenCurrencies: List<CryptoCurrencyStatus>?,
        totalFiatBalance: TotalFiatBalance?,
    ) {
        val displayedState = stateHolder.getWalletStateIfSelected(userWallet.walletId)

        tokenListAnalyticsSender.send(
            displayedUiState = displayedState,
            userWallet = userWallet,
            flattenCurrencies = flattenCurrencies ?: return,
            totalFiatBalance = totalFiatBalance ?: return,
        )
    }

    private fun updateContent(
        params: TokenConverterParams,
        appCurrency: AppCurrency,
        yieldSupplyApyMap: Map<String, BigDecimal>,
        stakingAvailabilityMap: Map<CryptoCurrency, StakingAvailability>,
        shouldShowMainPromo: Boolean,
    ) {
        stateHolder.update(
            SetTokenListTransformer(
                params = params,
                userWallet = userWallet,
                appCurrency = appCurrency,
                clickIntents = clickIntents,
                yieldSupplyApyMap = yieldSupplyApyMap,
                stakingAvailabilityMap = stakingAvailabilityMap,
                shouldShowMainPromo = shouldShowMainPromo,
            ),
        )
    }

    private fun appCurrencyFlow(): Flow<AppCurrency> = getSelectedAppCurrencyUseCase()
        .map {
            it.getOrElse { e ->
                Timber.e("Failed to load app currency: $e")
                AppCurrency.Default
            }
        }
        .distinctUntilChanged()

    private fun yieldSupplyApyFlow(): Flow<Map<String, BigDecimal>> = yieldSupplyApyFlowUseCase()
        .distinctUntilChanged()

    private fun yieldSupplyGetShouldShowMainPromoFlow(): Flow<Boolean> = yieldSupplyGetShouldShowMainPromoUseCase()
        .distinctUntilChanged()
}