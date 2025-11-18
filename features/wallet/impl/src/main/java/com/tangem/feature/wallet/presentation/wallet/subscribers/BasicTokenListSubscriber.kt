package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.getOrElse
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.usecase.StakingApyFlowUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TokenConverterParams
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.combine6
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("LongParameterList")
internal abstract class BasicTokenListSubscriber : WalletSubscriber() {
    protected abstract val userWallet: UserWallet
    protected abstract val stateHolder: WalletStateController
    protected abstract val clickIntents: WalletClickIntents
    protected abstract val tokenListAnalyticsSender: TokenListAnalyticsSender
    protected abstract val walletWithFundsChecker: WalletWithFundsChecker
    protected abstract val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase
    protected abstract val accountDependencies: AccountDependencies
    protected abstract val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase
    protected abstract val stakingApyFlowUseCase: StakingApyFlowUseCase

    private val sendAnalyticsJobHolder = JobHolder()
    private val onTokenListReceivedJobHolder = JobHolder()
    protected val isAccountsEnabled get() = accountDependencies.accountsFeatureToggles.isFeatureEnabled

    protected abstract fun tokenListFlow(coroutineScope: CoroutineScope): LceFlow<TokenListError, TokenList>

    protected abstract fun accountListFlow(coroutineScope: CoroutineScope): Flow<AccountStatusList>

    protected abstract suspend fun onTokenListReceived(maybeTokenList: Lce<TokenListError, TokenList>)
    protected open suspend fun onAccountListReceived() = {
        // todo account updateSortingIfNeeded? like [onTokenListReceived]
    }

    override fun create(coroutineScope: CoroutineScope): Flow<*> =
        if (isAccountsEnabled) createAccountListFlow(coroutineScope) else createTokenListFlow(coroutineScope)

    private fun createTokenListFlow(coroutineScope: CoroutineScope): Flow<*> {
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
            flow4 = stakingApyFlow(),
            transform = { maybeTokenList, appCurrency, yieldSupplyApyMap, stakingApyMap ->
                singleAccountTransform(
                    maybeTokenList = maybeTokenList,
                    appCurrency = appCurrency,
                    portfolioId = PortfolioId(userWallet.walletId),
                    yieldSupplyApyMap = yieldSupplyApyMap,
                    stakingApyMap = stakingApyMap,
                )
            },
        )
    }

    private suspend fun singleAccountTransform(
        maybeTokenList: Lce<TokenListError, TokenList>,
        appCurrency: AppCurrency,
        portfolioId: PortfolioId,
        yieldSupplyApyMap: Map<String, String>,
        stakingApyMap: Map<String, List<Yield.Validator>>,
    ) {
        val tokenList = maybeTokenList.getOrElse(
            ifLoading = { maybeContent ->
                val isRefreshing = stateHolder.getWalletState(userWallet.walletId)
                    ?.pullToRefreshConfig
                    ?.isRefreshing == true

                maybeContent
                    ?.takeIf { !isRefreshing }
                    ?: return
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
                return
            },
        )
        updateContent(
            params = TokenConverterParams.Wallet(portfolioId, tokenList),
            appCurrency = appCurrency,
            yieldSupplyApyMap = yieldSupplyApyMap,
            stakingApyMap = stakingApyMap,
        )

        walletWithFundsChecker.check(tokenList)
    }

    private fun createAccountListFlow(coroutineScope: CoroutineScope): Flow<*> = combine6(
        flow1 = accountListFlow(coroutineScope)
            .onEach { accountStatusList ->
                coroutineScope.launch {
                    sendTokenListAnalytics(
                        flattenCurrencies = accountStatusList.flattenCurrencies(),
                        totalFiatBalance = accountStatusList.totalFiatBalance,
                    )
                }.saveIn(sendAnalyticsJobHolder)
            }
            .distinctUntilChanged(),
        flow2 = appCurrencyFlow(),
        flow3 = accountDependencies.expandedAccountsHolder.expandedAccounts(userWallet),
        flow4 = accountDependencies.isAccountsModeEnabledUseCase(),
        flow5 = yieldSupplyApyFlow(),
        flow6 = stakingApyFlow(),
        transform = { accountList, appCurrency, expandedAccounts, isAccountMode, yieldSupplyApyMap, stakingApyMap ->
            val accountFlattenTokensList = accountList.flattenTokens()
            val accountFlattenCurrencies = accountFlattenTokensList
                .map { it.flattenCurrencies() }
                .flatten()
            val mainAccount: AccountStatus.CryptoPortfolio = when (val mainAccount = accountList.mainAccount) {
                is AccountStatus.CryptoPortfolio -> mainAccount
            }

            suspend fun singleAccountTransform(maybeTokenList: Lce<TokenListError, TokenList>) =
                this.singleAccountTransform(
                    maybeTokenList,
                    appCurrency,
                    PortfolioId(mainAccount.account.accountId),
                    yieldSupplyApyMap,
                    stakingApyMap,
                )

            when {
                !isAccountMode -> when (mainAccount.tokenList.flattenCurrencies().isEmpty()) {
                    true -> singleAccountTransform(Lce.Error(TokenListError.EmptyTokens))
                    false -> singleAccountTransform(Lce.Content(mainAccount.tokenList))
                }

                isAccountMode -> when (accountFlattenCurrencies.isEmpty()) {
                    true -> stateHolder.update(
                        SetTokenListErrorTransformer(
                            selectedWallet = userWallet,
                            error = TokenListError.EmptyTokens,
                            appCurrency = appCurrency,
                        ),
                    )
                    false -> {
                        val convertParams = TokenConverterParams.Account(accountList, expandedAccounts)
                        updateContent(convertParams, appCurrency, yieldSupplyApyMap, stakingApyMap)
                        accountFlattenTokensList
                            .map { tokenList -> coroutineScope.launch { walletWithFundsChecker.check(tokenList) } }
                            .joinAll()
                    }
                }
            }
        },
    )

    private fun AccountStatusList.flattenTokens(): List<TokenList> = this.accountStatuses.map {
        when (it) {
            is AccountStatus.CryptoPortfolio -> it.tokenList
        }
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
        yieldSupplyApyMap: Map<String, String>,
        stakingApyMap: Map<String, List<Yield.Validator>>,
    ) {
        stateHolder.update(
            SetTokenListTransformer(
                params = params,
                userWallet = userWallet,
                appCurrency = appCurrency,
                clickIntents = clickIntents,
                yieldSupplyApyMap = yieldSupplyApyMap,
                stakingApyMap = stakingApyMap,
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

    private fun yieldSupplyApyFlow(): Flow<Map<String, String>> = yieldSupplyApyFlowUseCase()
        .distinctUntilChanged()

    private fun stakingApyFlow(): Flow<Map<String, List<Yield.Validator>>> = stakingApyFlowUseCase()
        .distinctUntilChanged()
}