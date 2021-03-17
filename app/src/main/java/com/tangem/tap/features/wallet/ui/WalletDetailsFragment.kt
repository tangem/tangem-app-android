package com.tangem.tap.features.wallet.ui

import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionInflater
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletDialog
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.adapters.PendingTransactionsAdapter
import com.tangem.tap.features.wallet.ui.dialogs.AmountToSendDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_details_twin_cards.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.fragment_wallet_details.*
import kotlinx.android.synthetic.main.fragment_wallet_details.rv_pending_transaction
import kotlinx.android.synthetic.main.fragment_wallet_details.toolbar
import kotlinx.android.synthetic.main.item_currency_wallet.view.*
import kotlinx.android.synthetic.main.layout_balance_error.*
import kotlinx.android.synthetic.main.layout_balance_wallet_details.*
import kotlinx.android.synthetic.main.layout_wallet_details.*
import org.rekotlin.StoreSubscriber

class WalletDetailsFragment : Fragment(R.layout.fragment_wallet_details), StoreSubscriber<WalletState> {

    private lateinit var pendingTransactionAdapter: PendingTransactionsAdapter
    private var dialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(WalletAction.MultiWallet.SelectWallet(null))
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
        store.dispatch(WalletAction.UpdateWallet())
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { activity?.onBackPressed() }


        setupTransactionsRecyclerView()
        setupButtons()
    }


    private fun setupTransactionsRecyclerView() {
        pendingTransactionAdapter = PendingTransactionsAdapter()
        rv_pending_transaction.layoutManager = LinearLayoutManager(requireContext())
        rv_pending_transaction.adapter = pendingTransactionAdapter
    }

    private fun setupButtons() {
        btn_confirm.text = getString(R.string.wallet_button_send)
        btn_confirm.setOnClickListener { store.dispatch(WalletAction.Send()) }

        btn_share.setOnClickListener { store.dispatch(WalletAction.ShowQrCode) }

        btn_top_up.setOnClickListener {
            store.dispatch(
                    WalletAction.TopUpAction.TopUp(requireContext(), R.color.backgroundLightGray)
            )
        }
    }

    override fun newState(state: WalletState) {
        if (activity == null) return
        if (state.selectedWallet == null) return
        val selectedWallet = state.getSelectedWalletData() ?: return

        tv_currency_title.text = selectedWallet.currencyData.currency

        showPendingTransactionsIfPresent(selectedWallet.pendingTransactions)
        setupAddressCard(selectedWallet)
        setupBalanceData(selectedWallet.currencyData)

        btn_confirm.isEnabled = selectedWallet.mainButton.enabled
        btn_copy.setOnClickListener {
            selectedWallet.walletAddresses?.selectedAddress?.address?.let { addressString ->
                store.dispatch(WalletAction.CopyAddress(addressString, requireContext()))
            }
        }
        btn_share.setOnClickListener {
            selectedWallet.walletAddresses?.selectedAddress?.address?.let { addressString ->
                store.dispatch(WalletAction.ShareAddress(addressString, requireContext()))
            }
        }

        handleDialogs(state.walletDialog)
        handleCurrencyIcon(selectedWallet)

        srl_wallet_details.setOnRefreshListener {
            if (selectedWallet.currencyData.status != BalanceStatus.Loading) {
                store.dispatch(WalletAction.LoadWallet(
                        selectedWallet.topUpState.allowed,
                        selectedWallet.currencyData.currencySymbol
                ))
            }
        }

        if (selectedWallet.currencyData.status != BalanceStatus.Loading) {
            srl_wallet_details.isRefreshing = false
        }
    }

    private fun handleCurrencyIcon(wallet: WalletData) {
        val blockchain = wallet.currencyData.currencySymbol?.let { Blockchain.fromCurrency(it) }
        if (blockchain != null && blockchain != Blockchain.Unknown) {
            tv_token_letter.text = null
            iv_currency.colorFilter = null
            iv_currency.setImageResource(blockchain.getIconRes())
        } else {
            tv_token_letter.text = wallet.currencyData.currencySymbol?.take(1)
            wallet.token?.getColor()?.let { iv_currency.setColorFilter(it) }
            iv_currency.setImageResource(R.drawable.shape_circle)
        }
    }


    private fun showPendingTransactionsIfPresent(pendingTransactions: List<PendingTransaction>) {
        pendingTransactionAdapter.submitList(pendingTransactions)
        rv_pending_transaction?.show(pendingTransactions.isNotEmpty())
    }

    private fun setupAddressCard(state: WalletData) {
        if (state.walletAddresses != null) {
            if (state.shouldShowMultipleAddress()) {
                (card_balance as? ViewGroup)?.beginDelayedTransition()
                chip_group_address_type.show()
                chip_group_address_type.fitChipsByGroupWidth()

                val checkedId = MultipleAddressUiHelper.typeToId(state.walletAddresses.selectedAddress.type)
                if (checkedId != View.NO_ID) chip_group_address_type.check(checkedId)

                chip_group_address_type.setOnCheckedChangeListener { group, checkedId ->
                    if (checkedId == -1) return@setOnCheckedChangeListener
                    val type = MultipleAddressUiHelper.idToType(checkedId, state.currencyData.currencySymbol)
                    type?.let { store.dispatch(WalletAction.ChangeSelectedAddress(type)) }
                }
            } else {
                chip_group_address_type.hide()
            }
            tv_address.text = state.walletAddresses.selectedAddress.address
            tv_explore?.setOnClickListener {
                store.dispatch(WalletAction.ExploreAddress(
                        state.walletAddresses.selectedAddress.exploreUrl,
                        requireContext()))
            }
            iv_qr_code.setImageBitmap(state.walletAddresses.selectedAddress.address.toQrCode())
        }
    }

    private fun setupBalanceData(data: BalanceWidgetData) {
        when (data.status) {
            BalanceStatus.Loading -> {
                l_balance.show()
                l_balance_error.hide()
                tv_fiat_amount.hide()
                tv_amount.text = ""

                showStatus(R.id.tv_status_loading)
                showBalanceWithoutToken(data, false)
            }
            BalanceStatus.VerifiedOnline, BalanceStatus.TransactionInProgress -> {
                l_balance.show()
                l_balance_error.hide()
                val statusView = if (data.status == BalanceStatus.VerifiedOnline) {
                    R.id.tv_status_verified
                } else {
                    tv_status_error.text =
                            getText(R.string.wallet_balance_tx_in_progress)
                    R.id.group_error
                }
                showStatus(statusView)
                tv_status_error_message.hide()
                showBalanceWithoutToken(data, true)
            }
            BalanceStatus.Unreachable -> {
                l_balance.show()
                l_balance_error.hide()
                tv_fiat_amount.hide()

                tv_amount.text = ""

                tv_status_error_message.text = data.errorMessage
                tv_status_error.text =
                        getString(R.string.wallet_balance_blockchain_unreachable)

                showStatus(R.id.group_error)
                tv_status_error_message.show(!data.errorMessage.isNullOrBlank())
            }

            BalanceStatus.NoAccount -> {
                l_balance.hide()
                l_balance_error.show()
                tv_error_title.text = getText(R.string.wallet_error_no_account)
                tv_error_descriptions.text =
                        getString(
                                R.string.wallet_error_no_account_subtitle_format,
                                data.amountToCreateAccount, data.currencySymbol
                        )
            }
        }
    }

    private fun showStatus(@IdRes viewRes: Int) {
        group_error.show(viewRes == R.id.group_error)
        tv_status_loading.show(viewRes == R.id.tv_status_loading)
        tv_status_verified.show(viewRes == R.id.tv_status_verified)
    }

    private fun showBalanceWithoutToken(data: BalanceWidgetData, showAmount: Boolean) {
        tv_amount.text = if (showAmount) data.amount else ""
        if (showAmount) {
            tv_fiat_amount.show()
            tv_fiat_amount.text = data.fiatAmount
        }
    }

    private fun handleDialogs(walletDialog: WalletDialog?) {
        when (walletDialog) {
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
            R.id.meny_remove -> {
                store.state.walletState.getSelectedWalletData()?.let { walletData ->
                    store.dispatch(WalletAction.MultiWallet.RemoveWallet(walletData))
                    store.dispatch(WalletAction.MultiWallet.SelectWallet(null))
                    store.dispatch(NavigationAction.PopBackTo())
                    true
                }
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (store.state.walletState.canBeRemoved(store.state.walletState.getSelectedWalletData())) {
            inflater.inflate(R.menu.wallet_details, menu)
        }
    }

}

