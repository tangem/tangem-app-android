package com.tangem.tap.features.wallet.ui.wallet

import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.wallet.databinding.FragmentWalletBinding

interface WalletView {

    fun setFragment(fragment: WalletFragment, binding: FragmentWalletBinding)

    fun removeFragment()

    fun changeWalletView(fragment: WalletFragment, binding: FragmentWalletBinding)

    fun onViewCreated()

    fun onNewState(state: WalletState)

}