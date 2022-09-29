package com.tangem.tap.features.wallet.ui.wallet

import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.store
import com.tangem.wallet.databinding.FragmentWalletBinding

class SaltPaySingleWalletView : WalletView() {
    override fun changeWalletView(fragment: WalletFragment, binding: FragmentWalletBinding) {
        setFragment(fragment, binding)
        onViewCreated()
        showSingleWalletView(binding)
    }

    private fun showSingleWalletView(binding: FragmentWalletBinding) = with(binding) {
        rvMultiwallet.hide()
        btnAddToken.hide()
        rvPendingTransaction.hide()
        tvTwinCardNumber.hide()
        pbLoadingUserTokens.hide()
        lCardBalance.root.hide()
        lAddress.root.hide()
        lSingleWalletBalance.root.show()
    }

    override fun onViewCreated() {
    }

    override fun onNewState(state: WalletState) {
        val binding = binding ?: return
        state.primaryWallet ?: return

        setupBalance(state, state.primaryWallet, binding)
    }

    private fun setupBalance(state: WalletState, primaryWallet: WalletData, binding: FragmentWalletBinding) {
        binding.lSingleWalletBalance.root.show()
        SaltPayBalanceWidget(
            binding = binding.lSingleWalletBalance,
            data = SaltPayBalanceWidgetData(
                state = state.state,
                currencySymbol = primaryWallet.currencyData.currencySymbol,
                currency = primaryWallet.currencyData.amountFormatted,
                fiatAmount = primaryWallet.currencyData.fiatAmount,
                fiatCurrency = store.state.globalState.appCurrency,
            ),
        ).setup()
    }
}

