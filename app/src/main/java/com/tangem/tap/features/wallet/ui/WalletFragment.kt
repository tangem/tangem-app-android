package com.tangem.tap.features.wallet.ui

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.view.Menu.NONE
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressType
import com.tangem.blockchain.blockchains.cardano.CardanoAddressType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.tangem_sdk_new.extensions.dpToPx
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.termsOfUse.CardTou
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.dialogs.AmountToSendDialog
import com.tangem.tap.features.wallet.ui.dialogs.PayIdDialog
import com.tangem.tap.features.wallet.ui.dialogs.QrDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.card_balance.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.layout_address.*
import kotlinx.android.synthetic.main.layout_send_fee.*
import kotlinx.android.synthetic.main.layout_wallet_long_buttons.*
import kotlinx.android.synthetic.main.layout_wallet_short_buttons.*
import org.rekotlin.StoreSubscriber


class WalletFragment : Fragment(R.layout.fragment_wallet), StoreSubscriber<WalletState> {

    private var dialog: Dialog? = null
    private var snackbar: Snackbar? = null

    private lateinit var pendingTransactionAdapter: PendingTransactionsAdapter
    private lateinit var warningsAdapter: WarningMessagesAdapter

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
        store.dispatch(WalletAction.UpdateWallet)
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
        setupWarningsRecyclerView()
        setupTransactionsRecyclerView()
    }

    private fun setupWarningsRecyclerView() {
        warningsAdapter = WarningMessagesAdapter()
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rv_warning_messages.layoutManager = layoutManager
        rv_warning_messages.addItemDecoration(SpacesItemDecoration(rv_warning_messages.dpToPx(16f).toInt()))
        rv_warning_messages.adapter = warningsAdapter
    }

    private fun setupTransactionsRecyclerView() {
        pendingTransactionAdapter = PendingTransactionsAdapter()
        rv_pending_transaction.layoutManager = LinearLayoutManager(context)
        rv_pending_transaction.adapter = pendingTransactionAdapter
    }


    override fun newState(state: WalletState) {
        if (activity == null) return

        state.twinCardsState?.cardNumber?.let { cardNumber ->
            tv_twin_card_number.show()
            iv_twin_card.show()
            val number = when (cardNumber) {
                TwinCardNumber.First -> "1"
                TwinCardNumber.Second -> "2"
            }
            tv_twin_card_number.text = getString(R.string.wallet_twins_chip_format, number)
        }
        if (state.twinCardsState?.cardNumber == null) {
            tv_twin_card_number.hide()
            iv_twin_card.hide()
        }

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

        warningsAdapter.submitList(state.mainWarningsList)
        rv_warning_messages.show(state.mainWarningsList.isNotEmpty())

        pendingTransactionAdapter.submitList(state.pendingTransactions)
        rv_pending_transaction.show(state.pendingTransactions.isNotEmpty())

        handleDialogs(state.walletDialog)

        l_balance.show()
        BalanceWidget(this, state.currencyData, state.twinCardsState != null).setup()

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
            is WalletMainButton.CreateWalletButton -> {
                if (state.twinCardsState == null) {
                    R.string.wallet_button_create_wallet
                } else {
                    R.string.wallet_button_create_twin_wallet
                }
            }
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
        if (state.walletAddresses != null) {
            l_address?.show()
            if (state.showMultipleAddress) {
                (l_address as? ViewGroup)?.beginDelayedTransition()
                chip_group_address_type.show()
                chip_group_address_type.fitChipsByGroupWidth()

                val checkedId = MultipleAddressUiHelper.typeToId(state.walletAddresses.selectedAddress.type)
                if (checkedId != View.NO_ID) chip_group_address_type.check(checkedId)

                chip_group_address_type.setOnCheckedChangeListener { group, checkedId ->
                    if (checkedId == -1) return@setOnCheckedChangeListener

                    MultipleAddressUiHelper.idToType(checkedId, state.wallet?.blockchain)?.let {
                        store.dispatch(WalletAction.ChangeSelectedAddress(it))
                    }
                }
            } else {
                chip_group_address_type.hide()
            }
            tv_address.text = state.walletAddresses.selectedAddress.address
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
            null -> {
                dialog?.dismiss()
                dialog = null
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.details_menu -> {
                store.state.globalState.scanNoteResponse?.let { scanNoteResponse ->
                    store.dispatch(DetailsAction.PrepareScreen(
                            scanNoteResponse.card, scanNoteResponse,
                            store.state.walletState.wallet,
                            store.state.globalState.configManager?.config?.isCreatingTwinCardsAllowed,
                            CardTou(),
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

class MultipleAddressUiHelper {
    companion object {
        fun typeToId(type: AddressType): Int {
            return when (type) {
                is BitcoinAddressType.Legacy -> R.id.chip_legacy
                is BitcoinAddressType.Segwit -> R.id.chip_default
                is CardanoAddressType.Byron -> R.id.chip_legacy
                is CardanoAddressType.Shelley -> R.id.chip_default
                else -> View.NO_ID
            }
        }

        fun idToType(id: Int, blockchain: Blockchain?): AddressType? {
            return when (id) {
                R.id.chip_default -> {
                    when (blockchain) {
                        Blockchain.Bitcoin -> BitcoinAddressType.Segwit
                        Blockchain.CardanoShelley -> CardanoAddressType.Shelley
                        else -> null
                    }
                }
                R.id.chip_legacy -> {
                    when (blockchain) {
                        Blockchain.Bitcoin -> BitcoinAddressType.Legacy
                        Blockchain.CardanoShelley -> CardanoAddressType.Byron
                        else -> null
                    }
                }
                else -> null
            }
        }
    }
}