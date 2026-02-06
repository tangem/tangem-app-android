package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetShouldShowMainPromoUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.utils.coroutines.combine7
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.math.BigDecimal

/**
 * Subscriber that monitors account list related data and updates the wallet state accordingly.
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class AccountListSubscriber @AssistedInject constructor(
    @Assisted override val userWallet: UserWallet,
    override val accountDependencies: AccountDependencies,
    override val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    override val stateController: WalletStateController,
    override val clickIntents: WalletClickIntents,
    private val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase,
    private val stakingAvailabilityListUseCase: StakingAvailabilityListUseCase,
    private val yieldSupplyGetShouldShowMainPromoUseCase: YieldSupplyGetShouldShowMainPromoUseCase,
) : BasicAccountListSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> = combine7(
        flow1 = getAccountStatusListFlow(),
        flow2 = getAppCurrencyFlow(),
        flow3 = accountDependencies.expandedAccountsHolder.expandedAccounts(userWallet),
        flow4 = accountDependencies.isAccountsModeEnabledUseCase(),
        flow5 = yieldSupplyApyFlow(),
        flow6 = yieldSupplyGetShouldShowMainPromoFlow(),
        flow7 = stakingAvailabilityFlow(),
    ) {
            accountList, appCurrency, expandedAccounts, isAccountMode,
            yieldSupplyApyMap, shouldShowMainPromo, stakingAvailabilityMap,
        ->
        updateState(
            accountList = accountList,
            appCurrency = appCurrency,
            expandedAccounts = expandedAccounts,
            isAccountMode = isAccountMode,
            yieldSupplyApyMap = yieldSupplyApyMap,
            stakingAvailabilityMap = stakingAvailabilityMap,
            shouldShowMainPromo = shouldShowMainPromo,
        )
    }

    private fun stakingAvailabilityFlow(): Flow<Map<CryptoCurrency, StakingAvailability>> = getAccountStatusListFlow()
        .map { accountList -> accountList.flattenCurrencies().map(CryptoCurrencyStatus::currency) }
        .distinctUntilChanged()
        .mapLatest { flattenCurrencies ->
            stakingAvailabilityListUseCase.invokeSync(
                userWalletId = userWallet.walletId,
                cryptoCurrencyList = flattenCurrencies,
            )
        }
        .distinctUntilChanged()

    private fun yieldSupplyApyFlow(): Flow<Map<String, BigDecimal>> {
        return yieldSupplyApyFlowUseCase().distinctUntilChanged()
    }

    private fun yieldSupplyGetShouldShowMainPromoFlow(): Flow<Boolean> {
        return yieldSupplyGetShouldShowMainPromoUseCase().distinctUntilChanged()
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): AccountListSubscriber
    }
}