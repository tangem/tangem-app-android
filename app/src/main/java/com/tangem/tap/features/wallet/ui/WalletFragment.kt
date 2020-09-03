package com.tangem.tap.features.wallet.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.redux.PayIdState
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.card_balance.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.layout_address.*
import org.rekotlin.StoreSubscriber

class WalletFragment : Fragment(R.layout.fragment_wallet), StoreSubscriber<WalletState> {

    private var qrDialog: QrDialog? = null
    private var payIdDialog: PayIdDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.walletState == newState.walletState
            }.select { it.walletState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }

        btn_scan.setOnClickListener {
            store.dispatch(WalletAction.Scan)
        }
        btn_main.setOnClickListener {
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Send))
        }
    }

    override fun newState(state: WalletState) {
        if (activity == null) return

        if (state.wallet?.address != null) {
            l_address?.show()
            tv_address.text = state.wallet.address
            tv_explore?.setOnClickListener {
                store.dispatch(WalletAction.ExploreAddress(requireContext()))
            }
        } else {
            l_address?.hide()
        }


        btn_copy.setOnClickListener { store.dispatch(WalletAction.CopyAddress(requireContext())) }
        btn_show_qr.setOnClickListener { store.dispatch(WalletAction.ShowQrCode) }

        val buttonTitle = when (state.mainButton) {
            is WalletMainButton.SendButton -> R.string.wallet_button_send
            is WalletMainButton.CreateWalletButton -> R.string.wallet_button_create_wallet
        }
        btn_main.text = getString(buttonTitle)
        btn_main.isEnabled = state.mainButton.enabled

        btn_main.setOnClickListener {
            when (state.mainButton) {
                is WalletMainButton.SendButton -> TODO()
                is WalletMainButton.CreateWalletButton -> store.dispatch(WalletAction.CreateWallet)
            }
        }

        if (state.qrCode != null && state.wallet?.shareUrl != null) {
            qrDialog = QrDialog(requireContext())
            qrDialog?.showQr(state.qrCode, state.wallet.shareUrl)
        } else {
            qrDialog?.dismiss()
        }

        when (state.state) {
            ProgressState.Loading -> {
                l_balance.show()
                BalanceWidget(this, state.currencyData).setup()
            }
            ProgressState.Done -> {
                l_balance.show()
                BalanceWidget(this, state.currencyData).setup()
            }
            ProgressState.Error -> {

            }
        }

        when (state.payIdData.payIdState) {
            PayIdState.Disabled, PayIdState.Loading -> group_payid.hide()
            PayIdState.NotCreated -> {
                group_payid.show()
                tv_create_payid.show()
                tv_create_payid.setOnClickListener { store.dispatch(WalletAction.CreatePayId) }
                tv_payid_address.hide()
            }
            PayIdState.Created -> {
                group_payid.show()
                tv_create_payid.hide()
                tv_payid_address.show()
                tv_payid_address.text = state.payIdData.payId

            }
            PayIdState.ErrorLoading -> {
            }
        }

        when (state.creatingPayIdState) {
            CreatingPayIdState.EnterPayId -> {
                if (payIdDialog == null) payIdDialog = PayIdDialog(requireContext())
                payIdDialog?.show()
                payIdDialog?.stopProgress()
            }
            CreatingPayIdState.Waiting -> payIdDialog?.showProgress()
            null -> {
                payIdDialog?.dismiss()
                payIdDialog = null
            }
        }
    }


}