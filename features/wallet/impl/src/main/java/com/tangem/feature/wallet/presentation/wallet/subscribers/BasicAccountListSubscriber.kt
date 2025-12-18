package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TokenConverterParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import java.math.BigDecimal

/**
 * Basic implementation of [WalletSubscriber] for wallet with accounts.
 *
[REDACTED_AUTHOR]
 */
internal abstract class BasicAccountListSubscriber : BasicWalletSubscriber() {

    abstract val accountDependencies: AccountDependencies
    abstract val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase
    abstract val stateController: WalletStateController
    abstract val clickIntents: WalletClickIntents

    override val singleAccountStatusListSupplier: SingleAccountStatusListSupplier
        get() = accountDependencies.singleAccountStatusListSupplier

    protected fun getAppCurrencyFlow(): Flow<AppCurrency> {
        return getSelectedAppCurrencyUseCase.invokeOrDefault()
            .distinctUntilChanged()
    }

    protected fun updateState(
        accountList: AccountStatusList,
        appCurrency: AppCurrency,
        expandedAccounts: Set<AccountId>,
        isAccountMode: Boolean,
        yieldSupplyApyMap: Map<String, BigDecimal> = emptyMap(),
        stakingApyMap: Map<String, List<StakingTarget>> = emptyMap(),
        shouldShowMainPromo: Boolean = false,
    ) {
        val mainAccount = accountList.mainAccount

        when {
            !isAccountMode -> {
                val isMainAccountEmpty = mainAccount.tokenList.flattenCurrencies().isEmpty()
                val maybeTokenList = if (isMainAccountEmpty) {
                    Lce.Error(TokenListError.EmptyTokens)
                } else {
                    Lce.Content(mainAccount.tokenList)
                }

                singleAccountTransform(
                    maybeTokenList = maybeTokenList,
                    appCurrency = appCurrency,
                    portfolioId = PortfolioId(mainAccount.accountId),
                    yieldSupplyApyMap = yieldSupplyApyMap,
                    stakingApyMap = stakingApyMap,
                    shouldShowMainPromo = shouldShowMainPromo,
                )
            }
            isAccountMode -> {
                val convertParams = TokenConverterParams.Account(accountList, expandedAccounts)
                updateContent(
                    params = convertParams,
                    appCurrency = appCurrency,
                    yieldSupplyApyMap = yieldSupplyApyMap,
                    stakingApyMap = stakingApyMap,
                    shouldShowMainPromo = shouldShowMainPromo,
                )
            }
        }
    }

    private fun singleAccountTransform(
        maybeTokenList: Lce<TokenListError, TokenList>,
        appCurrency: AppCurrency,
        portfolioId: PortfolioId,
        yieldSupplyApyMap: Map<String, BigDecimal> = emptyMap(),
        stakingApyMap: Map<String, List<StakingTarget>> = emptyMap(),
        shouldShowMainPromo: Boolean,
    ) {
        val tokenList = maybeTokenList.getOrElse(
            ifLoading = { maybeContent ->
                val isRefreshing = stateController.getWalletState(userWallet.walletId)
                    ?.pullToRefreshConfig
                    ?.isRefreshing == true

                maybeContent
                    ?.takeIf { !isRefreshing }
                    ?: return
            },
            ifError = { e ->
                Timber.e("Failed to load token list: $e")
                stateController.update(
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
            shouldShowMainPromo = shouldShowMainPromo,
        )
    }

    private fun updateContent(
        params: TokenConverterParams,
        appCurrency: AppCurrency,
        yieldSupplyApyMap: Map<String, BigDecimal> = emptyMap(),
        stakingApyMap: Map<String, List<StakingTarget>> = emptyMap(),
        shouldShowMainPromo: Boolean,
    ) {
        stateController.update(
            SetTokenListTransformer(
                params = params,
                userWallet = userWallet,
                appCurrency = appCurrency,
                clickIntents = clickIntents,
                yieldSupplyApyMap = yieldSupplyApyMap,
                stakingApyMap = stakingApyMap,
                shouldShowMainPromo = shouldShowMainPromo,
            ),
        )
    }
}