package com.tangem.tap.features.wallet.ui

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.tap.common.analytics.converters.ParamCardCurrencyConverter
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.ui.analytics.WalletAnalyticsEventsMapper
import com.tangem.tap.store
import com.tangem.tap.walletStoresManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

// TODO: Kill me, please
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class WalletViewModel @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
) : ViewModel(), StoreSubscriber<UserWalletsListManager?>, DefaultLifecycleObserver {
    private var observeWalletStoresUpdatesJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    private val walletAnalyticsEventsMapper = WalletAnalyticsEventsMapper()

    init {
        subscribeToUserWalletsListManagerUpdates()
    }

    override fun onCleared() {
        store.unsubscribe(this)
    }

    override fun newState(state: UserWalletsListManager?) {
        // Restarting observing of wallet store updates when the manager changes
        if (state != null) {
            bootstrapSelectedWalletStoresChanges(state)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        launch()
        val scanResponse = store.state.globalState.scanResponse
        if (scanResponse != null) {
            val currency = ParamCardCurrencyConverter().convert(scanResponse.cardTypesResolver)
            val signInType = store.state.signInState.type
            if (currency != null && signInType != null) {
                analyticsEventHandler.send(
                    Basic.SignedIn(
                        currency = currency,
                        batch = scanResponse.card.batchId,
                        signInType = signInType,
                        walletsCount = store.state.globalState.userWalletsListManager?.walletsCount.toString(),
                    ),
                )
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        analyticsEventHandler.send(MainScreen.ScreenOpened())
    }

    fun onBalanceLoaded(totalBalance: TotalFiatBalance?) {
        if (totalBalance != null) {
            walletAnalyticsEventsMapper.convert(totalBalance)?.let { balanceParam ->
                analyticsEventHandler.send(
                    Basic.BalanceLoaded(
                        balance = balanceParam,
                    ),
                )
            }
        }
    }

    private fun launch() {
        val manager = store.state.globalState.userWalletsListManager
        if (manager != null) {
            bootstrapSelectedWalletStoresChanges(manager)
        }
        bootstrapShowSaveWalletIfNeeded()
    }

    @OptIn(FlowPreview::class)
    private fun bootstrapSelectedWalletStoresChanges(manager: UserWalletsListManager) {
        observeWalletStoresUpdatesJob = manager.selectedUserWallet
            .map { it.walletId }
            .flatMapLatest(walletStoresManager::get)
            .debounce { walletStores ->
                if (walletStores.isNotEmpty()) WALLET_STORES_DEBOUNCE_TIMEOUT else 0
            }
            .onEach { walletStores ->
                store.dispatchOnMain(WalletAction.WalletStoresChanged(walletStores))
            }
            .launchIn(viewModelScope)
    }

    private fun bootstrapShowSaveWalletIfNeeded() {
        viewModelScope.launch {
            delay(timeMillis = 1_800)
            store.dispatchOnMain(WalletAction.ShowSaveWalletIfNeeded)
        }
    }

    private fun subscribeToUserWalletsListManagerUpdates() {
        store.subscribe(this) { appState ->
            appState
                .skip { old, new ->
                    old.globalState.userWalletsListManager == new.globalState.userWalletsListManager
                }
                .select { it.globalState.userWalletsListManager }
        }
    }

    companion object {
        private const val WALLET_STORES_DEBOUNCE_TIMEOUT = 100L
    }
}