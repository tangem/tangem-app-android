package com.tangem.tap.features.wallet.ui

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.tangem.tap.common.extensions.getDrawable
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.dialogs.AmountToSendDialog
import com.tangem.tap.features.wallet.ui.dialogs.PayIdDialog
import com.tangem.tap.features.wallet.ui.dialogs.QrDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.card_balance.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.layout_address.*
import org.rekotlin.StoreSubscriber


class WalletFragment : Fragment(R.layout.fragment_wallet), StoreSubscriber<WalletState> {

    private var dialog: Dialog? = null
    private var snackbar: Snackbar? = null

    private lateinit var viewAdapter: PendingTransactionsAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
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
        setupTransactionsRecyclerView()
    }


    private fun setupTransactionsRecyclerView() {
        viewAdapter = PendingTransactionsAdapter()
        rv_pending_transaction.layoutManager = LinearLayoutManager(context)
        rv_pending_transaction.adapter = viewAdapter
    }


    override fun newState(state: WalletState) {
        if (activity == null) return

        srl_wallet.setOnRefreshListener {
            store.dispatch(WalletAction.LoadData)
        }

        setupNoInternetHandling(state)

        setupButtons(state)
        setupAddressCard(state)
        setupCardImage(state.cardImage?.artwork)

        viewAdapter.submitList(state.pendingTransactions)

        handleDialogs(state.walletDialog)

        l_balance.show()
        BalanceWidget(this, state.currencyData).setup()

        if (state.state == ProgressState.Done) {
            srl_wallet.isRefreshing = false
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
    }

    private fun setupNoInternetHandling(state: WalletState) {
        if (state.state == ProgressState.Error) {
            if (state.error == ErrorType.NoInternetConnection) {
                srl_wallet?.isRefreshing = false
                snackbar = Snackbar.make(
                        coordinator_wallet, getString(R.string.notification_no_internet),
                        Snackbar.LENGTH_INDEFINITE
                ).setAction(getString(R.string.notification_no_internet_retry))
                { store.dispatch(WalletAction.LoadData) }
                        .also { it.show() }
            }
        } else {
            snackbar?.dismiss()
        }
    }

    private fun setupButtons(state: WalletState) {
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
                is WalletMainButton.SendButton -> store.dispatch(WalletAction.Send())
                is WalletMainButton.CreateWalletButton -> store.dispatch(WalletAction.CreateWallet)
            }
        }
    }

    private fun setupAddressCard(state: WalletState) {
        if (state.addressData != null) {
            l_address?.show()
            tv_address.text = state.addressData.address
            tv_explore?.setOnClickListener {
                store.dispatch(WalletAction.ExploreAddress(requireContext()))
            }
        } else {
            l_address?.hide()
        }
    }

    private fun setupCardImage(cardImage: Bitmap?) {
        if (cardImage != null) {
            iv_card.setImageBitmap(cardImage)
        } else {
            iv_card.setImageDrawable(getDrawable(R.drawable.card_default))
        }
    }

    private fun handleDialogs(walletDialog: WalletDialog?) {
        when (walletDialog) {
            is WalletDialog.QrDialog -> {
                if (walletDialog.qrCode != null && walletDialog.shareUrl != null) {
                    if (dialog == null) dialog = QrDialog(requireContext()).apply {
                        this.showQr(walletDialog.qrCode, walletDialog.shareUrl)
                    }
                }
            }
            is WalletDialog.CreatePayIdDialog -> {
                when (walletDialog.creatingPayIdState) {
                    CreatingPayIdState.EnterPayId -> {
                        if (dialog == null) dialog = PayIdDialog(requireContext()).apply {
                            this.show()
                            this.stopProgress()
                        }
                    }
                    CreatingPayIdState.Waiting -> (dialog as? PayIdDialog)?.showProgress()
                }
            }
            is WalletDialog.SelectAmountToSendDialog -> {
                if (dialog == null) dialog = AmountToSendDialog(requireContext()).apply {
                    this.show(walletDialog.amounts)
                }
            }
            null -> {
                dialog?.dismiss()
                dialog = null
            }
        }
    }

}