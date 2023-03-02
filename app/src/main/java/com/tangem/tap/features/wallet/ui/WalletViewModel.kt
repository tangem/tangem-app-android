package com.tangem.tap.features.wallet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.domain.userWalletList.UserWalletsListManager
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.tap.walletStoresManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.rekotlin.StoreSubscriber

// TODO: Kill me, please
@OptIn(ExperimentalCoroutinesApi::class)
internal class WalletViewModel : ViewModel(), StoreSubscriber<UserWalletsListManager?> {
    private var observeWalletStoresUpdatesJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

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

    fun launch() {
        val manager = store.state.globalState.userWalletsListManager
        if (manager != null) {
            bootstrapSelectedWalletStoresChanges(manager)
        }
        bootstrapShowSaveWalletIfNeeded()
    }

    private fun bootstrapSelectedWalletStoresChanges(manager: UserWalletsListManager) {
        observeWalletStoresUpdatesJob = manager.selectedUserWallet
            .map { it.walletId }
            .flatMapLatest { selectedUserWalletId ->
                walletStoresManager.get(selectedUserWalletId)
            }
            .onEach { walletStores ->
                store.dispatch(WalletAction.WalletStoresChanged(walletStores))
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
}