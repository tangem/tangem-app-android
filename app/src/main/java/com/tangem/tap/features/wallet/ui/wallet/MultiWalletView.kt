package com.tangem.tap.features.wallet.ui.wallet

import android.app.Dialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletDialog
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.features.wallet.ui.adapters.WalletAdapter
import com.tangem.tap.features.wallet.ui.dialogs.SignedHashesWarningDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.card_balance.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.layout_balance_error.*
import kotlinx.android.synthetic.main.layout_wallet_long_buttons.*


class MultiWalletView : WalletView {

    private var fragment: WalletFragment? = null
    private var dialog: Dialog? = null

    private lateinit var walletsAdapter: WalletAdapter


    override fun changeWalletView(fragment: WalletFragment) {
        setFragment(fragment)
        onViewCreated()
        showMultiWalletView(fragment)
        setupButtons(fragment)
    }


    private fun showMultiWalletView(fragment: WalletFragment) = with(fragment) {
        tv_twin_card_number.hide()
        rv_pending_transaction.hide()
        l_card_balance.hide()
        l_address.hide()
        l_buttons_short.hide()
        l_buttons_long.hide()
        btn_scan_multiwallet?.show()
        rv_multiwallet.show()
        btn_add_token.show()
    }

    private fun setupButtons(fragment: WalletFragment) = with(fragment) {
        btn_scan_multiwallet?.setOnClickListener { store.dispatch(WalletAction.Scan) }
    }

    override fun setFragment(fragment: WalletFragment) {
        this.fragment = fragment
    }

    override fun removeFragment() {
        this.fragment = null
    }

    override fun onViewCreated() {
        setupWalletsRecyclerView()
    }

    private fun setupWalletsRecyclerView() {
        val fragment = fragment ?: return
        walletsAdapter = WalletAdapter()
        walletsAdapter.setHasStableIds(true)
        fragment.rv_multiwallet.layoutManager = LinearLayoutManager(fragment.requireContext())
        fragment.rv_multiwallet.adapter = walletsAdapter
    }

    override fun onNewState(state: WalletState) {
        val fragment = fragment ?: return

        walletsAdapter.submitList(state.wallets, state.primaryBlockchain, state.primaryToken)

        fragment.btn_add_token?.setOnClickListener {
            store.dispatch(TokensAction.LoadCurrencies)
            store.dispatch(TokensAction.SetAddedCurrencies(state.wallets))
            store.dispatch(NavigationAction.NavigateTo(AppScreen.AddTokens))
        }
        handleErrorStates(state, fragment)
        handleDialogs(state.walletDialog)
    }

    private fun handleErrorStates(state: WalletState, fragment: WalletFragment) {
        when (state.primaryWallet?.currencyData?.status) {
            BalanceStatus.EmptyCard -> {
                showErrorState(
                    fragment,
                    fragment.getText(R.string.wallet_error_empty_card),
                    fragment.getString(R.string.wallet_error_empty_card_subtitle)
                )
                configureButtonsForEmptyWalletState(fragment)
            }
            BalanceStatus.UnknownBlockchain -> {
                showErrorState(
                    fragment,
                    fragment.getText(R.string.wallet_error_unsupported_blockchain),
                    fragment.getString(R.string.wallet_error_unsupported_blockchain_subtitle)
                )
            }
        }
    }

    private fun showErrorState(
        fragment: WalletFragment, errorTitle: CharSequence, errorDescription: CharSequence,
    ) = with(fragment) {
        l_card_balance.show()
        l_balance.hide()
        l_balance_error.show()
        rv_multiwallet.hide()
        btn_add_token.hide()
        tv_error_title.text = errorTitle
        tv_error_descriptions.text = errorDescription
    }

    private fun configureButtonsForEmptyWalletState(fragment: WalletFragment) = with(fragment) {
        btn_scan_multiwallet.hide()
        l_buttons_long.show()
        btn_scan_long.setOnClickListener { store.dispatch(WalletAction.Scan) }
        btn_confirm_long.setOnClickListener { store.dispatch(WalletAction.CreateWallet) }
        btn_confirm_long.text = fragment.getText(R.string.wallet_button_create_wallet)
    }

    private fun handleDialogs(walletDialog: StateDialog?) {
        val fragment = fragment ?: return
        val context = fragment.context ?: return
        when (walletDialog) {
            is WalletDialog.SignedHashesMultiWalletDialog -> {
                if (dialog == null) {
                    dialog = SignedHashesWarningDialog.create(context).apply { show() }
                }
            }
            else -> {
                dialog?.dismiss()
                dialog = null
            }
        }
    }
}