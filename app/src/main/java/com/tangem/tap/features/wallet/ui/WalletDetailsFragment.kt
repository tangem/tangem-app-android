package com.tangem.tap.features.wallet.ui

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
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.tangem_sdk_new.extensions.dpToPx
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.common.TestActions
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.DetailsScreen
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.recyclerView.SpaceItemDecoration
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.onboarding.getQRReceiveMessage
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.redux.WalletState.Companion.UNKNOWN_AMOUNT_SIGN
import com.tangem.tap.features.wallet.ui.adapters.PendingTransactionsAdapter
import com.tangem.tap.features.wallet.ui.adapters.WalletDetailWarningMessagesAdapter
import com.tangem.tap.features.wallet.ui.images.load
import com.tangem.tap.features.wallet.ui.test.TestWallet
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManagerSafe
import com.tangem.tap.walletCurrenciesManager
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletDetailsBinding
import kotlinx.coroutines.launch
import org.rekotlin.StoreSubscriber

class WalletDetailsFragment : Fragment(R.layout.fragment_wallet_details),
    StoreSubscriber<WalletState> {

    private lateinit var pendingTransactionAdapter: PendingTransactionsAdapter
    private lateinit var warningMessagesAdapter: WalletDetailWarningMessagesAdapter

    private val binding: FragmentWalletDetailsBinding by viewBinding(FragmentWalletDetailsBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        Analytics.send(DetailsScreen.ScreenOpened())
        activity?.onBackPressedDispatcher?.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    store.dispatch(WalletAction.MultiWallet.SelectWallet(null))
                    store.dispatch(NavigationAction.PopBackTo())
                }
            },
        )
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.select { it.walletState }
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
        setupWarningsRecyclerView()
        setupTestActionButton()
    }

    private fun setupTransactionsRecyclerView() = with(binding) {
        pendingTransactionAdapter = PendingTransactionsAdapter()
        rvPendingTransaction.layoutManager = LinearLayoutManager(requireContext())
        rvPendingTransaction.adapter = pendingTransactionAdapter
    }

    private fun setupWarningsRecyclerView() = with(binding) {
        warningMessagesAdapter = WalletDetailWarningMessagesAdapter()
        rvWarningMessages.layoutManager = LinearLayoutManager(requireContext())
        rvWarningMessages.adapter = warningMessagesAdapter
        rvWarningMessages.addItemDecoration(SpaceItemDecoration.vertical(8f))
    }

    private fun setupButtons() = with(binding) {
        rowButtons.onBuyClick = {
            store.dispatch(WalletAction.TradeCryptoAction.Buy())
        }
        rowButtons.onSellClick = {
            store.dispatch(WalletAction.TradeCryptoAction.Sell)
        }
        rowButtons.onTradeClick = {
            store.dispatch(WalletAction.DialogAction.ChooseTradeActionDialog)
        }
        rowButtons.onSendClick = {
            store.dispatch(WalletAction.Send())
        }
    }

    private fun setupTestActionButton() {
        view?.findViewById<View>(R.id.l_balance)?.let { view ->
            TestActions.initFor(
                view = view,
                actions = TestWallet.solanaRentExemptWarning(),
            )
        }
    }

    override fun newState(state: WalletState) {
        if (activity == null || view == null) return
        if (state.selectedCurrency == null) return
        val selectedWallet = state.getSelectedWalletData() ?: return


        showPendingTransactionsIfPresent(selectedWallet.pendingTransactions)
        setupCurrency(selectedWallet.currencyData, selectedWallet.currency)
        setupAddressCard(selectedWallet)
        setupNoInternetHandling(state)
        setupBalanceData(selectedWallet.currencyData)
        setupButtons(selectedWallet, state.isExchangeServiceFeatureOn)

        handleCurrencyIcon(selectedWallet)
        handleWarnings(selectedWallet)
        updateViewMeasurements()

        binding.srlWalletDetails.setOnRefreshListener {
            if (selectedWallet.currencyData.status != BalanceStatus.Loading) {
                Analytics.send(Token.Refreshed())
                val blockchainNetwork = BlockchainNetwork(
                    blockchain = selectedWallet.currency.blockchain,
                    derivationPath = selectedWallet.currency.derivationPath,
                    tokens = emptyList(),
                )
                val selectedUserWallet = userWalletsListManagerSafe?.selectedUserWalletSync
                if (selectedUserWallet != null) scope.launch {
                    walletCurrenciesManager.update(selectedUserWallet, blockchainNetwork)
                } else {
                    store.dispatch(
                        WalletAction.LoadWallet(
                            blockchain = BlockchainNetwork(
                                selectedWallet.currency.blockchain,
                                selectedWallet.currency.derivationPath,
                                emptyList(),
                            ),
                        ),
                    )
                    store.dispatch(WalletAction.LoadFiatRate(coinsList = listOf(selectedWallet.currency)))
                }
            }
        }

        if (selectedWallet.currencyData.status != BalanceStatus.Loading) {
            binding.srlWalletDetails.isRefreshing = false
        }
    }

    private fun updateViewMeasurements() {
        val tvFiatAmount = binding.lWalletDetails.lBalance.tvFiatAmount
        val paddingStart = when (tvFiatAmount.text) {
            UNKNOWN_AMOUNT_SIGN -> 16f
            else -> 12f
        }
        tvFiatAmount.setPadding(
            tvFiatAmount.dpToPx(paddingStart).toInt(),
            tvFiatAmount.paddingTop,
            tvFiatAmount.paddingEnd,
            tvFiatAmount.paddingBottom,
        )
    }

    private fun setupCurrency(currencyData: BalanceWidgetData, currency: Currency) = with(binding) {
        tvCurrencyTitle.text = currencyData.currency

        if (currency is Currency.Token) {
            tvCurrencySubtitle.text = tvCurrencySubtitle.getString(
                R.string.wallet_currency_subtitle,
                currency.blockchain.fullName,
            )
            tvCurrencySubtitle.show()
        } else {
            tvCurrencySubtitle.hide()
        }
    }

    private fun setupButtons(selectedWallet: WalletData, isExchangeServiceFeatureOn: Boolean) = with(binding) {
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

        rowButtons.updateButtonsVisibility(
            exchangeServiceFeatureOn = isExchangeServiceFeatureOn,
            buyAllowed = selectedWallet.isAvailableToBuy,
            sellAllowed = selectedWallet.isAvailableToSell,
            sendAllowed = selectedWallet.mainButton.enabled,
        )
    }

    private fun handleWarnings(selectedWallet: WalletData) = with(binding) {
        val converter = WalletWarningConverter(requireContext())
        val warningDetails = selectedWallet.assembleWarnings().map { converter.convert(it) }

        warningMessagesAdapter.submitList(warningDetails)
        rvWarningMessages.show(warningDetails.isNotEmpty())
    }

    private fun handleCurrencyIcon(wallet: WalletData) = with(binding.lWalletDetails.lBalance) {
        ivCurrency.load(
            currency = wallet.currency,
            derivationStyle = store.state.globalState
                .scanResponse
                ?.card
                ?.derivationStyle,
        )
    }

    private fun showPendingTransactionsIfPresent(pendingTransactions: List<PendingTransaction>) {
        pendingTransactionAdapter.submitList(pendingTransactions)
        binding.rvPendingTransaction.show(pendingTransactions.isNotEmpty())
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
            tvExplore.setOnClickListener {
                store.dispatch(
                    WalletAction.ExploreAddress(
                        state.walletAddresses.selectedAddress.exploreUrl,
                        requireContext(),
                    ),
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
                    buttonTitle = R.string.common_retry,
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
                lBalance.tvAmount.text = data.amountFormatted
                lBalance.tvFiatAmount.text = data.fiatAmountFormatted
                lBalance.tvStatus.setLoadingStatus(R.string.wallet_balance_loading)
            }
            BalanceStatus.VerifiedOnline, BalanceStatus.SameCurrencyTransactionInProgress,
            BalanceStatus.TransactionInProgress,
            -> {
                lBalanceError.root.hide()
                lBalance.root.show()
                lBalance.groupBalance.show()
                lBalance.tvError.hide()
                lBalance.tvAmount.text = data.amountFormatted
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
                lBalance.tvError.setWarningStatus(
                    R.string.wallet_balance_blockchain_unreachable,
                    data.errorMessage,
                )
            }
            BalanceStatus.NoAccount -> {
                lBalance.root.hide()
                lBalanceError.root.show()
                lBalanceError.tvErrorTitle.text = getText(R.string.wallet_error_no_account)
                lBalanceError.tvErrorDescriptions.text =
                    getString(
                        R.string.no_account_generic,
                        data.amountToCreateAccount, data.currencySymbol,
                    )
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_remove -> {
                store.state.walletState.getSelectedWalletData()?.let { walletData ->
                    store.dispatch(WalletAction.MultiWallet.TryToRemoveWallet(walletData.currency))
                    true
                }
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_wallet_details, menu)
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

    private fun TextView.setStatus(
        text: String,
        @ColorRes color: Int,
        @DrawableRes drawable: Int?,
    ) {
        this.text = text
        setTextColor(getColor(color))
        setCompoundDrawablesWithIntrinsicBounds(drawable ?: 0, 0, 0, 0)
    }
}
