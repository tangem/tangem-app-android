package com.tangem.tap.features.wallet.ui.wallet

import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.TokenData
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.store
import com.tangem.wallet.databinding.FragmentWalletBinding
import timber.log.Timber

class SaltPaySingleWalletView : WalletView() {
    override fun changeWalletView(fragment: WalletFragment, binding: FragmentWalletBinding) {
        setFragment(fragment, binding)
        onViewCreated()
        showSingleWalletView(binding)
    }

    private fun showSingleWalletView(binding: FragmentWalletBinding) = with(binding) {
        rvMultiwallet.hide()
        btnAddToken.hide()
        rowButtons.hide()
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
        val tokenData = state.primaryWallet?.currencyData?.token ?: return

        setupBalance(state, tokenData, binding)
    }

    private fun setupBalance(state: WalletState, tokenData: TokenData, binding: FragmentWalletBinding) {
        binding.lSingleWalletBalance.root.show()
        Timber.e("Current address is ${state.primaryWalletManager?.wallet?.address}")
        SaltPayBalanceWidget(
            binding = binding.lSingleWalletBalance,
            data = SaltPayBalanceWidgetData(
                state = state.state,
                currencySymbol = tokenData.tokenSymbol,
                currency = tokenData.amountFormatted,
                fiatAmount = tokenData.fiatAmount, // TODO: show fiatAmount
                fiatCurrency = store.state.globalState.appCurrency,
            ),
        ).setup()
    }
}
