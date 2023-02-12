package com.tangem.tap.features.wallet.ui.wallet

import com.tangem.domain.common.extensions.debounce
import com.tangem.tap.common.extensions.animateVisibility
import com.tangem.tap.common.extensions.formatAmountAsSpannedString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.mainScope
import com.tangem.tap.store
import com.tangem.wallet.databinding.FragmentWalletBinding
import com.tangem.wallet.databinding.LayoutSaltPayWalletBinding
import org.rekotlin.Action

class SaltPayWalletView : WalletView() {

    private lateinit var saltPayBinding: LayoutSaltPayWalletBinding
    private val actionDebouncer = debounce<Action>(500, mainScope) { store.dispatch(it) }

    override fun changeWalletView(fragment: WalletFragment, binding: FragmentWalletBinding) {
        saltPayBinding = binding.lSaltPayWallet
        setFragment(fragment, binding)
        onViewCreated()
        showSaltPayView(binding)
    }

    private fun showSaltPayView(binding: FragmentWalletBinding) = with(binding) {
        rvWarningMessages.hide()
        rvPendingTransaction.hide()
        rvMultiwallet.hide()
        tvTwinCardNumber.hide()
        lCardBalance.root.hide()
        lSingleWalletBalance.root.hide()
        lAddress.root.hide()
        rowButtons.hide()
        btnAddToken.hide()
        pbLoadingUserTokens.hide()
        lSaltPayWallet.root.show()
    }

    override fun onViewCreated() {
    }

    override fun onNewState(state: WalletState) {
        setupBalanceWidget(state)
    }

    private fun setupBalanceWidget(state: WalletState) = with(saltPayBinding.lSaltPayBalance) {
        val tokenData = state.primaryTokenData ?: return@with

        val appCurrency = store.state.globalState.appCurrency
        val mainProgressState = state.state

        if (mainProgressState == ProgressState.Loading) {
            veilBalance.veil()
            veilBalanceCrypto.veil()
        } else {
            veilBalanceCrypto.unVeil()
        }

        tvProcessing.animateVisibility(show = mainProgressState == ProgressState.Error)
        veilBalanceCrypto.animateVisibility(show = mainProgressState != ProgressState.Error)

        if (tokenData.currencyData.fiatAmount == null) {
            actionDebouncer(WalletAction.LoadFiatRate())
        } else {
            veilBalance.unVeil()
            tvBalance.text = tokenData.currencyData.fiatAmount.formatAmountAsSpannedString(
                currencySymbol = appCurrency.symbol,
            )
        }

        tvBalanceCrypto.text = tokenData.currencyData.amountFormatted

        tvCurrencyName.text = appCurrency.code
        tvCurrencyName.setOnClickListener {
            store.dispatch(WalletAction.AppCurrencyAction.ChooseAppCurrency)
        }

        btnBuy.show(tokenData.isAvailableToBuy)
        btnBuy.setOnClickListener {
            store.dispatch(WalletAction.TradeCryptoAction.Buy(false))
        }
    }
}
