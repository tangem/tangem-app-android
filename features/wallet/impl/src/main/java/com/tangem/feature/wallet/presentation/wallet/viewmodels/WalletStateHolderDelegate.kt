package com.tangem.feature.wallet.presentation.wallet.viewmodels

import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class WalletStateHolderDelegate(
    private val uiStateHolder: WalletStateHolder,
) : ReadWriteProperty<Any?, WalletState> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): WalletState = uiStateHolder.uiState

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: WalletState) {
        uiStateHolder.setState(value)
    }
}

internal fun uiStateHolder(initialState: WalletState): ReadWriteProperty<Any?, WalletState> {
    return WalletStateHolderDelegate(uiStateHolder = WalletStateHolder(initialState = initialState))
}