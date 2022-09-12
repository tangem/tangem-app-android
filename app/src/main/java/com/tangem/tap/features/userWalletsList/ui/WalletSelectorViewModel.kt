package com.tangem.tap.features.userWalletsList.ui

import androidx.lifecycle.ViewModel
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.rekotlin.StoreSubscriber

internal class WalletSelectorViewModel : ViewModel(), StoreSubscriber<AppState> {
    private val stateInternal = MutableStateFlow(WalletSelectorScreenState())
    val state: StateFlow<WalletSelectorScreenState> = stateInternal

    fun unlock() {
        TODO("Not yet implemented")
    }

    fun addWallet() {
        TODO("Not yet implemented")
    }

    fun selectWallet(walletId: String) {
        TODO("Not yet implemented")
    }

    fun editWallet(walletId: String) {
        TODO("Not yet implemented")
    }

    fun cancelWalletEditing() {
        TODO("Not yet implemented")
    }

    fun renameWallet() {
        TODO("Not yet implemented")
    }

    fun deleteWallet() {
        TODO("Not yet implemented")
    }

    fun closeError() {
        TODO("Not yet implemented")
    }

    override fun newState(state: AppState) {
        TODO("Not yet implemented")
    }

    override fun onCleared() {
        store.unsubscribe(this)
    }
}
