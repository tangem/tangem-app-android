package com.tangem.feature.wallet.presentation.wallet.viewmodels

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.WalletState

/**
 * Wallet state cache. It allows to switch the wallets without additional loading like a PagerView.
 *
 * @author Andrew Khokhlov on 10/08/2023
 */
internal object WalletStateCache {

    private val states = mutableMapOf<UserWalletId, WalletState.ContentState>()

    /** Get state by [userWalletId] */
    fun getState(userWalletId: UserWalletId): WalletState.ContentState? = states[userWalletId]

    /** Add or update [state] by [userWalletId]  */
    fun update(userWalletId: UserWalletId, state: WalletState.ContentState) {
        states[userWalletId] = state
    }

    /** Update all content states  */
    fun updateAll(block: (WalletState.ContentState.() -> WalletState.ContentState)) {
        states.keys.forEach {
            states[it] = block(requireNotNull(states[it]))
        }
    }
}
