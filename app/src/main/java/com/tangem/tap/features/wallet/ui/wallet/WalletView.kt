package com.tangem.tap.features.wallet.ui.wallet

import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.wallet.databinding.FragmentWalletBinding

abstract class WalletView {
    var swapInteractor: SwapInteractor? = null

    protected var fragment: WalletFragment? = null
    protected var binding: FragmentWalletBinding? = null
    fun setFragment(fragment: WalletFragment, binding: FragmentWalletBinding) {
        this.fragment = fragment
        this.binding = binding
    }

    fun removeFragment() {
        fragment = null
        binding = null
    }

    open fun onDestroyFragment() {}

    abstract fun changeWalletView(fragment: WalletFragment, binding: FragmentWalletBinding)
    abstract fun onViewCreated()
    abstract fun onNewState(state: WalletState)
}
