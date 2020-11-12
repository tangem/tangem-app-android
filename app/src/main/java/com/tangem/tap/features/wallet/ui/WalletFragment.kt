package com.tangem.tap.features.wallet.ui

import android.app.Dialog
import android.os.Bundle
import android.view.Menu
import android.view.Menu.NONE
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.dialogs.AmountToSendDialog
import com.tangem.tap.features.wallet.ui.dialogs.PayIdDialog
import com.tangem.tap.features.wallet.ui.dialogs.QrDialog
import com.tangem.tap.features.wallet.ui.dialogs.WarningDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.card_balance.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.layout_address.*
import kotlinx.android.synthetic.main.layout_wallet_long_buttons.*
import kotlinx.android.synthetic.main.layout_wallet_short_buttons.*
import org.rekotlin.StoreSubscriber


class WalletFragment : Fragment(R.layout.fragment_wallet), StoreSubscriber<WalletState> {

    private var dialog: Dialog? = null
    private var snackbar: Snackbar? = null

    private lateinit var viewAdapter: PendingTransactionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
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
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
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

        if (!state.showDetails) {
            toolbar.menu.removeItem(R.id.details_menu)
        } else if (toolbar.menu.findItem(R.id.details_menu) == null) {
            toolbar.menu.add(R.menu.wallet, R.id.details_menu, NONE, R.string.details_title)
        }

        srl_wallet.setOnRefreshListener {
            store.dispatch(WalletAction.LoadData)
        }

        setupNoInternetHandling(state)

        setupButtons(state)
        setupAddressCard(state)
        setupCardImage(state.cardImage)

        viewAdapter.submitList(state.pendingTransactions)
        if (state.pendingTransactions.isEmpty()) {
            rv_pending_transaction.hide()
        } else {
            rv_pending_transaction.show()
        }

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
                        coordinator_wallet, getString(R.string.wallet_notification_no_internet),
                        Snackbar.LENGTH_INDEFINITE
                ).setAction(getString(R.string.common_retry))
                { store.dispatch(WalletAction.LoadData) }
                        .also { it.show() }
            }
        } else {
            snackbar?.dismiss()
        }
    }

    private fun setupButtons(state: WalletState) {
        if (state.topUpState.allowed) {
            l_buttons_long.hide()
            l_buttons_short.show()
        } else {
            l_buttons_long.show()
            l_buttons_short.hide()
        }

        val btnConfirm = if (state.topUpState.allowed) {
            btn_confirm_short
        } else {
            btn_confirm_long
        }
        val btnScan = if (state.topUpState.allowed) {
            btn_scan_short
        } else {
            btn_scan_long
        }

        btnScan.setOnClickListener {
            store.dispatch(WalletAction.Scan)
        }

        btn_copy.setOnClickListener { store.dispatch(WalletAction.CopyAddress(requireContext())) }
        btn_show_qr.setOnClickListener { store.dispatch(WalletAction.ShowQrCode) }

        val buttonTitle = when (state.mainButton) {
            is WalletMainButton.SendButton -> R.string.wallet_button_send
            is WalletMainButton.CreateWalletButton -> R.string.wallet_button_create_wallet
        }
        btnConfirm.text = getString(buttonTitle)
        btnConfirm.isEnabled = state.mainButton.enabled

        btnConfirm.setOnClickListener {
            when (state.mainButton) {
                is WalletMainButton.SendButton -> store.dispatch(WalletAction.Send())
                is WalletMainButton.CreateWalletButton -> store.dispatch(WalletAction.CreateWallet)
            }
        }
        btn_top_up.setOnClickListener {
            store.dispatch(
                    WalletAction.TopUpAction.TopUp(requireContext(), R.color.backgroundLightGray)
            )
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

    private fun setupCardImage(cardImage: Artwork?) {
        Picasso.get()
                .load(cardImage?.artworkId)
                .placeholder(R.drawable.card_placeholder)
                ?.error(R.drawable.card_placeholder)
                ?.into(iv_card)
    }

    private fun handleDialogs(walletDialog: WalletDialog?) {
        when (walletDialog) {
            is WalletDialog.QrDialog -> {
                if (walletDialog.qrCode != null && walletDialog.shareUrl != null) {
                    if (dialog == null) dialog = QrDialog(requireContext()).apply {
                        this.showQr(
                                walletDialog.qrCode, walletDialog.shareUrl, walletDialog.currencyName
                        )
                    }
                }
            }
            is WalletDialog.CreatePayIdDialog -> {
                when (walletDialog.creatingPayIdState) {
                    CreatingPayIdState.EnterPayId -> {
                        if (dialog == null) dialog = PayIdDialog(requireContext()).apply {
                            this.show()
                        }
                        (dialog as? PayIdDialog)?.stopProgress()
                    }
                    CreatingPayIdState.Waiting -> (dialog as? PayIdDialog)?.showProgress()
                }
            }
            is WalletDialog.SelectAmountToSendDialog -> {
                if (dialog == null) dialog = AmountToSendDialog(requireContext()).apply {
                    this.show(walletDialog.amounts)
                }
            }
            is WalletDialog.WarningDialog -> {
                if (dialog == null) dialog = WarningDialog(requireContext()).apply {
                    this.show(walletDialog.type)
                }
            }
            null -> {
                dialog?.dismiss()
                dialog = null
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.details_menu -> {
                store.state.globalState.scanNoteResponse?.card?.let { card ->
                    store.dispatch(DetailsAction.PrepareScreen(
                            card, store.state.walletState.wallet,
                            store.state.globalState.appCurrency
                    ))
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.Details))
                    true
                }
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (store.state.walletState.showDetails) inflater.inflate(R.menu.wallet, menu)
    }

}
