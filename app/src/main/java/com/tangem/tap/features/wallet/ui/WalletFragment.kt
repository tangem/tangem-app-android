package com.tangem.tap.features.wallet.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.card_balance.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.layout_address.*
import org.rekotlin.StoreSubscriber

class WalletFragment : Fragment(R.layout.fragment_wallet), StoreSubscriber<WalletState> {

    var dialog: QrDialog? = null

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
    }

    override fun newState(state: WalletState) {
        if (activity == null) return

        state.wallet?.address?.let { tv_address.text = it }
        tv_explore?.setOnClickListener {
            store.dispatch(WalletAction.ExploreAddress(requireContext()))
        }

        btn_copy.setOnClickListener { store.dispatch(WalletAction.CopyAddress(requireContext())) }
        btn_show_qr.setOnClickListener { store.dispatch(WalletAction.ShowQrCode) }

        if (state.qrCode != null && state.wallet?.shareUrl != null) {
            dialog = QrDialog(requireContext())
            dialog?.show(state.qrCode, state.wallet.shareUrl)
        } else {
            dialog?.dismiss()
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

        when (state.payIdData.state) {
            PayIdState.Loading -> {
                group_payid.hide()
            }
            PayIdState.NotCreated -> {
                group_payid.show()
                tv_create_payid.show()
                tv_payid_address.hide()
            }
            PayIdState.Loaded -> {
                group_payid.show()
                tv_create_payid.hide()
                tv_payid_address.show()
                tv_payid_address.text = state.payIdData.address
            }
            PayIdState.Error -> group_payid.hide()
        }

    }


}