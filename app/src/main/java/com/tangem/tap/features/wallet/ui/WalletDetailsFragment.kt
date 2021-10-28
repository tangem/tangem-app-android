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
import com.squareup.picasso.Picasso
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.adapters.PendingTransactionsAdapter
import com.tangem.tap.features.wallet.ui.dialogs.AmountToSendDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_wallet_details.*
import kotlinx.android.synthetic.main.item_currency_wallet.view.*
import kotlinx.android.synthetic.main.item_popular_token.view.*
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

        btn_share.setOnClickListener { store.dispatch(WalletAction.ShowDialog.QrCode) }

        btn_top_up.setOnClickListener { store.dispatch(WalletAction.TradeCryptoAction.Buy) }

        btn_sell.setOnClickListener { store.dispatch(WalletAction.TradeCryptoAction.Sell) }
    }

    override fun newState(state: WalletState) {
        if (activity == null) return
        if (state.selectedWallet == null) return
        val selectedWallet = state.getSelectedWalletData() ?: return

        tv_currency_title.text = selectedWallet.currencyData.currency
        val currency = selectedWallet.currency
        if (currency is Currency.Token) {
            tv_currency_subtitle.text = currency.blockchain.tokenDisplayName()
            tv_currency_subtitle.show()
        } else {
            tv_currency_subtitle.hide()
        }


        showPendingTransactionsIfPresent(selectedWallet.pendingTransactions)
        setupAddressCard(selectedWallet)
        setupNoInternetHandling(state)
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
                    allowToBuy = selectedWallet.tradeCryptoState.buyingAllowed,
                    allowToSell = selectedWallet.tradeCryptoState.sellingAllowed,
                    blockchain = selectedWallet.currency?.blockchain
                ))
            }
        }

        if (selectedWallet.currencyData.status != BalanceStatus.Loading) {
            srl_wallet_details.isRefreshing = false
        }

        btn_top_up.isEnabled = selectedWallet.tradeCryptoState.buyingAllowed
        btn_sell.show(selectedWallet.tradeCryptoState.sellingAllowed)
    }

    private fun handleCurrencyIcon(wallet: WalletData) {
        Picasso.get().loadCurrenciesIcon(
            imageView = iv_currency,
            textView = tv_token_letter,
            blockchain = wallet.currency?.blockchain,
            token = (wallet.currency as? Currency.Token)?.token
        )
    }


    private fun showPendingTransactionsIfPresent(pendingTransactions: List<PendingTransaction>) {
        pendingTransactionAdapter.submitList(pendingTransactions)
        rv_pending_transaction?.show(pendingTransactions.isNotEmpty())
    }

    private fun setupAddressCard(state: WalletData) {
        if (state.walletAddresses != null) {
            if (state.shouldShowMultipleAddress() && state.currency is Currency.Blockchain) {
                (card_balance as? ViewGroup)?.beginDelayedTransition()
                chip_group_address_type.show()
                chip_group_address_type.fitChipsByGroupWidth()

                val checkedId = MultipleAddressUiHelper.typeToId(state.walletAddresses.selectedAddress.type)
                if (checkedId != View.NO_ID) chip_group_address_type.check(checkedId)

                chip_group_address_type.setOnCheckedChangeListener { group, checkedId ->
                    if (checkedId == -1) return@setOnCheckedChangeListener
                    val type = MultipleAddressUiHelper.idToType(checkedId, state.currency.blockchain)
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
            iv_qr_code.setImageBitmap(state.walletAddresses.selectedAddress.shareUrl.toQrCode())
        }
    }

    private fun setupNoInternetHandling(state: WalletState) {
        if (state.state == ProgressState.Error) {
            if (state.error == ErrorType.NoInternetConnection) {
                srl_wallet_details?.isRefreshing = false
                (activity as? SnackbarHandler)?.showSnackbar(
                    text = R.string.wallet_notification_no_internet,
                    buttonTitle = R.string.common_retry
                ) { store.dispatch(WalletAction.LoadData) }
            }
        } else {
            (activity as? SnackbarHandler)?.dismissSnackbar()
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
            BalanceStatus.VerifiedOnline, BalanceStatus.SameCurrencyTransactionInProgress,
            BalanceStatus.TransactionInProgress -> {
                l_balance.show()
                l_balance_error.hide()
                val statusView = if (data.status == BalanceStatus.VerifiedOnline ||
                        data.status == BalanceStatus.SameCurrencyTransactionInProgress) {
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
        card_pending_transaction_warning.show(data.status == BalanceStatus.SameCurrencyTransactionInProgress)
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
            tv_fiat_amount.text = data.fiatAmountFormatted
        }
    }

    private fun handleDialogs(walletDialog: StateDialog?) {
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
