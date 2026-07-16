package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.common.ui.tokens.TokenConverterParams
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListTransformer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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

    protected fun updateState2(
        accountList: AccountStatusList,
        appCurrency: AppCurrency,
        expandedAccounts: Set<AccountId>,
        isAccountMode: Boolean,
        isMultipleCardsEnabled: Boolean,
        isPolymarketEnabled: Boolean = false,
        yieldSupplyApyMap: Map<String, BigDecimal> = emptyMap(),
        stakingAvailabilityMap: Map<CryptoCurrency, StakingAvailability> = emptyMap(),
        shouldShowMainPromo: Boolean = false,
    ) {
        stateController.update(
            SetTokenListTransformer(
                params = TokenConverterParams.Account(accountList, expandedAccounts),
                userWallet = userWallet,
                appCurrency = appCurrency,
                clickIntents = clickIntents,
                yieldSupplyApyMap = yieldSupplyApyMap,
                stakingAvailabilityMap = stakingAvailabilityMap,
                shouldShowMainPromo = shouldShowMainPromo,
                isAccountsModeEnabled = isAccountMode,
                isMultipleCardsEnabled = isMultipleCardsEnabled,
                isPolymarketEnabled = isPolymarketEnabled,
            ),
        )
    }
}