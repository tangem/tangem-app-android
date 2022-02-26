package com.tangem.tap.features.wallet.ui

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
import com.squareup.picasso.Picasso
import com.tangem.common.extensions.guard
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.onboarding.getQRReceiveMessage
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.adapters.PendingTransactionsAdapter
import com.tangem.tap.features.wallet.ui.dialogs.AmountToSendDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletDetailsBinding
import org.rekotlin.StoreSubscriber

class WalletDetailsFragment : Fragment(R.layout.fragment_wallet_details),
    StoreSubscriber<WalletState> {

    private lateinit var pendingTransactionAdapter: PendingTransactionsAdapter
    private var dialog: Dialog? = null

    private val binding: FragmentWalletDetailsBinding by viewBinding(FragmentWalletDetailsBinding::bind)

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
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }


        setupTransactionsRecyclerView()
        setupButtons()
    }


    private fun setupTransactionsRecyclerView() = with(binding) {
        pendingTransactionAdapter = PendingTransactionsAdapter()
        rvPendingTransaction.layoutManager = LinearLayoutManager(requireContext())
        rvPendingTransaction.adapter = pendingTransactionAdapter
    }

    private fun setupButtons() = with(binding) {
        btnConfirm.text = getString(R.string.wallet_button_send)
        btnConfirm.setOnClickListener { store.dispatch(WalletAction.Send()) }

        binding.lWalletDetails.btnShare.setOnClickListener { store.dispatch(WalletAction.ShowDialog.QrCode) }

        btnTrade.setOnClickListener { store.dispatch(WalletAction.TradeCryptoAction.Buy) }

        btnSell.setOnClickListener { store.dispatch(WalletAction.TradeCryptoAction.Sell) }
    }

    override fun newState(state: WalletState) {
        if (activity == null || view == null) return
        if (state.selectedWallet == null) return
        val selectedWallet = state.getSelectedWalletData() ?: return


        showPendingTransactionsIfPresent(selectedWallet.pendingTransactions)
        setupCurrency(selectedWallet.currencyData, selectedWallet.currency)
        setupAddressCard(selectedWallet)
        setupNoInternetHandling(state)
        setupBalanceData(selectedWallet.currencyData)
        setupButtons(selectedWallet)

        handleDialogs(state.walletDialog)
        handleCurrencyIcon(selectedWallet)
        handleWalletRent(selectedWallet.walletRent)


        binding.srlWalletDetails.setOnRefreshListener {
            if (selectedWallet.currencyData.status != BalanceStatus.Loading) {
                store.dispatch(
                    WalletAction.LoadWallet(
                        blockchain = selectedWallet.currency.blockchain
                    )
                )
            }
        }

        if (selectedWallet.currencyData.status != BalanceStatus.Loading) {
            binding.srlWalletDetails.isRefreshing = false
        }
    }

    private fun setupCurrency(currencyData: BalanceWidgetData, currency: Currency) = with(binding) {
        tvCurrencyTitle.text = currencyData.currency
        if (currency is Currency.Token) {
            binding.tvCurrencySubtitle.text = currency.blockchain.tokenDisplayName()
            binding.tvCurrencySubtitle.show()
        } else {
            binding.tvCurrencySubtitle.hide()
        }
    }

    private fun setupButtons(selectedWallet: WalletData) = with(binding) {
        btnConfirm.isEnabled = selectedWallet.mainButton.enabled
        lWalletDetails.btnCopy.setOnClickListener {
            selectedWallet.walletAddresses?.selectedAddress?.address?.let { addressString ->
                store.dispatch(WalletAction.CopyAddress(addressString, requireContext()))
            }
        }
        lWalletDetails.btnShare.setOnClickListener {
            selectedWallet.walletAddresses?.selectedAddress?.address?.let { addressString ->
                store.dispatch(WalletAction.ShareAddress(addressString, requireContext()))
            }
        }
        btnTrade.isEnabled = selectedWallet.tradeCryptoState.buyingAllowed
        btnSell.show(selectedWallet.tradeCryptoState.sellingAllowed)
    }

    private fun handleWalletRent(rent: WalletRent?) = with(binding) {
        val rent = rent.guard {
            lRentWarning.root.hide()
            return
        }
        val warningMessage = requireContext().getString(
            R.string.solana_rent_warning, rent.minRentValue, rent.rentExemptValue
        )
        lRentWarning.tvRentWarningMessage.text = warningMessage
        lRentWarning.root.show()
    }

    private fun handleCurrencyIcon(wallet: WalletData) = with(binding.lWalletDetails.lBalance) {
        Picasso.get().loadCurrenciesIcon(
            imageView = ivCurrency,
            textView = tvTokenLetter,
            blockchain = wallet.currency.blockchain,
            token = (wallet.currency as? Currency.Token)?.token
        )
    }


    private fun showPendingTransactionsIfPresent(pendingTransactions: List<PendingTransaction>) {
        pendingTransactionAdapter.submitList(pendingTransactions)
        binding.rvPendingTransaction?.show(pendingTransactions.isNotEmpty())
    }

    private fun setupAddressCard(state: WalletData) = with(binding.lWalletDetails) {
        if (state.walletAddresses != null) {
            if (state.shouldShowMultipleAddress() && state.currency is Currency.Blockchain) {
                (cardBalance as? ViewGroup)?.beginDelayedTransition()
                chipGroupAddressType.show()
                chipGroupAddressType.fitChipsByGroupWidth()

                val checkedId =
                    MultipleAddressUiHelper.typeToId(state.walletAddresses.selectedAddress.type)
                if (checkedId != View.NO_ID) chipGroupAddressType.check(checkedId)

                chipGroupAddressType.setOnCheckedChangeListener { group, checkedId ->
                    if (checkedId == -1) return@setOnCheckedChangeListener
                    val type =
                        MultipleAddressUiHelper.idToType(checkedId, state.currency.blockchain)
                    type?.let { store.dispatch(WalletAction.ChangeSelectedAddress(type)) }
                }
            } else {
                chipGroupAddressType.hide()
            }
            tvAddress.text = state.walletAddresses.selectedAddress.address
            tvExplore?.setOnClickListener {
                store.dispatch(
                    WalletAction.ExploreAddress(
                        state.walletAddresses.selectedAddress.exploreUrl,
                        requireContext()
                    )
                )
            }
            ivQrCode.setImageBitmap(state.walletAddresses.selectedAddress.shareUrl.toQrCode())
            tvReceiveMessage.text =
                getQRReceiveMessage(tvReceiveMessage.context, state.currency)
        }
    }

    private fun setupNoInternetHandling(state: WalletState) {
        if (state.state == ProgressState.Error) {
            if (state.error == ErrorType.NoInternetConnection) {
                binding.srlWalletDetails.isRefreshing = false
                (activity as? SnackbarHandler)?.showSnackbar(
                    text = R.string.wallet_notification_no_internet,
                    buttonTitle = R.string.common_retry
                ) { store.dispatch(WalletAction.LoadData) }
            }
        } else {
            (activity as? SnackbarHandler)?.dismissSnackbar()
        }
    }

    private fun setupBalanceData(data: BalanceWidgetData) = with(binding.lWalletDetails) {
        when (data.status) {
            BalanceStatus.Loading -> {
                lBalanceError.root.hide()
                lBalance.root.show()
                lBalance.groupBalance.show()
                lBalance.tvError.hide()
                lBalance.tvAmount.text = data.amount
                lBalance.tvFiatAmount.text = data.fiatAmountFormatted
                lBalance.tvStatus.setLoadingStatus(R.string.wallet_balance_loading)
            }
            BalanceStatus.VerifiedOnline, BalanceStatus.SameCurrencyTransactionInProgress,
            BalanceStatus.TransactionInProgress -> {
                lBalanceError.root.hide()
                lBalance.root.show()
                lBalance.groupBalance.show()
                lBalance.tvError.hide()
                lBalance.tvAmount.text = data.amount
                lBalance.tvFiatAmount.text = data.fiatAmountFormatted
                when (data.status) {
                    BalanceStatus.VerifiedOnline, BalanceStatus.SameCurrencyTransactionInProgress -> {
                        lBalance.tvStatus.setVerifiedBalanceStatus(R.string.wallet_balance_verified)
                    }
                    else -> {
                        lBalance.tvStatus.setWarningStatus(R.string.wallet_balance_tx_in_progress)
                    }
                }
            }
            BalanceStatus.Unreachable -> {
                lBalanceError.root.hide()
                lBalance.root.show()
                lBalance.groupBalance.hide()
                lBalance.tvError.show()
                lBalance.tvError.setWarningStatus(R.string.wallet_balance_blockchain_unreachable, data.errorMessage)
            }
            BalanceStatus.NoAccount -> {
                lBalance.root.hide()
                lBalanceError.root.show()
                lBalanceError.tvErrorTitle.text = getText(R.string.wallet_error_no_account)
                lBalanceError.tvErrorDescriptions.text =
                    getString(
                        R.string.wallet_error_no_account_subtitle_format,
                        data.amountToCreateAccount, data.currencySymbol
                    )
            }
        }
        binding.cardPendingTransactionWarning.show(data.status == BalanceStatus.SameCurrencyTransactionInProgress)
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
            R.id.menu_remove -> {
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
        inflater.inflate(R.menu.wallet_details, menu)
        val walletCanBeRemoved = store.state.walletState.canBeRemoved(
            store.state.walletState.getSelectedWalletData()
        )
        menu.getItem(0).isEnabled = walletCanBeRemoved
    }

    private fun TextView.setWarningStatus(mainMessage: Int, error: String? = null) {
        val text = getString(mainMessage).appendIfNotNull(error, "\nError: ")
        setStatus(text, R.color.warning, R.drawable.ic_warning_small)
    }

    private fun TextView.setVerifiedBalanceStatus(mainMessage: Int) {
        setStatus(getString(mainMessage), R.color.accent, R.drawable.ic_ok)
    }

    private fun TextView.setLoadingStatus(mainMessage: Int) {
        setStatus(getString(mainMessage), R.color.darkGray4, null)
    }

    private fun TextView.setStatus(text: String, @ColorRes color: Int, @DrawableRes drawable: Int?) {
        this.text = text
        setTextColor(getColor(color))
        setCompoundDrawablesWithIntrinsicBounds(drawable ?: 0, 0, 0, 0)
    }
}