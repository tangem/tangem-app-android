package com.tangem.tap.features.wallet.ui.wallet

import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.WalletFragment

interface WalletView {

    fun setFragment(fragment: WalletFragment)

    fun removeFragment()

    fun changeWalletView(fragment: WalletFragment)

    fun onViewCreated()

    fun onNewState(state: WalletState)

}