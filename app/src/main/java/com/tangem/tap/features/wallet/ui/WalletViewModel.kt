package com.tangem.tap.features.wallet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletStoresManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class WalletViewModel : ViewModel() {
    fun launch() {
        bootstrapSelectedWalletStoresChanges()
        bootstrapShowSaveWalletIfNeeded()
    }

    private fun bootstrapSelectedWalletStoresChanges() {
        userWalletsListManager.selectedUserWallet
            .flatMapLatest { selectedWallet ->
                walletStoresManager.get(selectedWallet.walletId)
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
}
