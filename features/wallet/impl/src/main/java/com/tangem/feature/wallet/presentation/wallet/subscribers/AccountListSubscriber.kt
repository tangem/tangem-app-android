package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.jointaccount.store.JointAccountInvitesStore
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.wallets.usecase.IsWalletBackedUpUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetShouldShowMainPromoUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.utils.coroutines.combine9
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
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
    private val isWalletBackedUpUseCase: IsWalletBackedUpUseCase,
    private val jointAccountInvitesStore: JointAccountInvitesStore,
) : BasicAccountListSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        val walletId = userWallet.walletId.stringValue
        TangemLogger.i("$TAG[$walletId]: create() called, building combine9")
        return combine9(
            flow1 = getAccountStatusListFlow()
                .onStart { TangemLogger.i("$TAG[$walletId]: flow1 accountStatusList subscribed") }
                .onEach { list ->
                    val count = list.flattenCurrencies().size
                    TangemLogger.i("$TAG[$walletId]: flow1 accountStatusList emitted (currencies=$count)")
                },
            flow2 = getAppCurrencyFlow()
                .onStart { TangemLogger.i("$TAG[$walletId]: flow2 appCurrency subscribed") }
                .onEach { TangemLogger.i("$TAG[$walletId]: flow2 appCurrency emitted=${it.code}") },
            flow3 = accountDependencies.expandedAccountsHolder.expandedAccounts(userWallet)
                .onStart { TangemLogger.i("$TAG[$walletId]: flow3 expandedAccounts subscribed") }
                .onEach { TangemLogger.i("$TAG[$walletId]: flow3 expandedAccounts emitted (size=${it.size})") },
            flow4 = accountDependencies.isAccountsModeEnabledUseCase()
                .onStart { TangemLogger.i("$TAG[$walletId]: flow4 isAccountsModeEnabled subscribed") }
                .onEach { TangemLogger.i("$TAG[$walletId]: flow4 isAccountsModeEnabled emitted=$it") },
            flow5 = yieldSupplyApyFlow()
                .onStart { TangemLogger.i("$TAG[$walletId]: flow5 yieldSupplyApy subscribed") }
                .onEach { TangemLogger.i("$TAG[$walletId]: flow5 yieldSupplyApy emitted (size=${it.size})") },
            flow6 = yieldSupplyGetShouldShowMainPromoFlow()
                .onStart { TangemLogger.i("$TAG[$walletId]: flow6 shouldShowMainPromo subscribed") }
                .onEach { TangemLogger.i("$TAG[$walletId]: flow6 shouldShowMainPromo emitted=$it") },
            flow7 = stakingAvailabilityFlow()
                .onStart { TangemLogger.i("$TAG[$walletId]: flow7 stakingAvailability subscribed") }
                .onEach { TangemLogger.i("$TAG[$walletId]: flow7 stakingAvailability emitted (size=${it.size})") },
            flow8 = isWalletBackedUpUseCase.flow(userWallet).distinctUntilChanged()
                .onStart { TangemLogger.i("$TAG[$walletId]: flow8 isBackedUp subscribed") }
                .onEach { TangemLogger.i("$TAG[$walletId]: flow8 isBackedUp emitted=$it") },
            flow9 = jointInvitesFlow()
                .onStart { TangemLogger.i("$TAG[$walletId]: flow9 jointInvites subscribed") }
                .onEach { TangemLogger.i("$TAG[$walletId]: flow9 jointInvites emitted (size=${it.size})") },
        ) {
                accountList, appCurrency, expandedAccounts, isAccountMode,
                yieldSupplyApyMap, shouldShowMainPromo, stakingAvailabilityMap, isHotBackedUp,
                accountIdsWithInvites,
            ->
            TangemLogger.i(
                "$TAG[$walletId]: combine9 transform fired — " +
                    "currencies=${accountList.flattenCurrencies().size}, " +
                    "appCurrency=${appCurrency.code}, " +
                    "expanded=${expandedAccounts.size}, " +
                    "isAccountMode=$isAccountMode, " +
                    "apyMap=${yieldSupplyApyMap.size}, " +
                    "promo=$shouldShowMainPromo, " +
                    "stakingMap=${stakingAvailabilityMap.size}, " +
                    "isBackedUp=$isHotBackedUp",
            )
            updateState2(
                accountList = accountList,
                appCurrency = appCurrency,
                expandedAccounts = expandedAccounts,
                isAccountMode = isAccountMode,
                yieldSupplyApyMap = yieldSupplyApyMap,
                stakingAvailabilityMap = stakingAvailabilityMap,
                shouldShowMainPromo = shouldShowMainPromo,
                isHotBackedUp = isHotBackedUp,
                accountIdsWithInvites = accountIdsWithInvites,
            )
        }
    }

    /** The ids of the joint accounts whose unused invites live on this device - only the creator ever holds them */
    private fun jointInvitesFlow(): Flow<Set<String>> = getAccountStatusListFlow()
        .map { list ->
            list.accountStatuses
                .filterIsInstance<AccountStatus.Joint>()
                .mapTo(hashSetOf()) { it.account.accountId.value }
        }
        .distinctUntilChanged()
        .mapLatest { accountIds ->
            accountIds.filterTo(hashSetOf()) { accountId ->
                !jointAccountInvitesStore.get(userWallet.walletId, accountId).isNullOrEmpty()
            }
        }
        .distinctUntilChanged()

    private fun stakingAvailabilityFlow(): Flow<Map<CryptoCurrency, StakingAvailability>> = getAccountStatusListFlow()
        .map { accountList -> accountList.flattenPersonalCurrencies().map(CryptoCurrencyStatus::currency) }
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

    private companion object {
        const val TAG = "AccountListSubscriber"
    }
}