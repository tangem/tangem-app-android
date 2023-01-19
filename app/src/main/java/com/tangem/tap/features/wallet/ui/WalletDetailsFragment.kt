package com.tangem.tap.features.wallet.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
import com.badoo.mvicore.modelWatcher
import com.tangem.common.doOnResult
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.tangem_sdk_new.extensions.dpToPx
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.common.TestActions
import com.tangem.tap.common.analytics.events.DetailsScreen
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.appendIfNotNull
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.fitChipsByGroupWidth
import com.tangem.tap.common.extensions.getColor
import com.tangem.tap.common.extensions.getString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.extensions.toQrCode
import com.tangem.tap.common.recyclerView.SpaceItemDecoration
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.redux.ErrorType
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.WalletState.Companion.UNKNOWN_AMOUNT_SIGN
import com.tangem.tap.features.wallet.ui.adapters.PendingTransactionsAdapter
import com.tangem.tap.features.wallet.ui.adapters.WalletDetailWarningMessagesAdapter
import com.tangem.tap.features.wallet.ui.images.load
import com.tangem.tap.features.wallet.ui.test.TestWallet
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManagerSafe
import com.tangem.tap.walletCurrenciesManager
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.StoreSubscriber

@Suppress("LargeClass", "MagicNumber")
class WalletDetailsFragment : Fragment(R.layout.fragment_wallet_details),
    StoreSubscriber<WalletState> {

    private lateinit var pendingTransactionAdapter: PendingTransactionsAdapter
    private lateinit var warningMessagesAdapter: WalletDetailWarningMessagesAdapter

    private val binding: FragmentWalletDetailsBinding by viewBinding(FragmentWalletDetailsBinding::bind)

    private val walletDataWatcher = modelWatcher<WalletData> {
        WalletData::pendingTransactions {
            showPendingTransactionsIfPresent(it)
        }
        WalletData::currency {
            handleCurrencyIcon(it)
        }
        WalletData::currencyData {
            setupBalanceData(it)
        }
        (WalletData::currencyData or WalletData::currency) { walletData ->
            setupCurrency(walletData.currencyData, walletData.currency)
            setupSwipeRefresh(walletData.currencyData, walletData.currency)
        }
    }

    private val walletStateWatcher = modelWatcher<WalletState> {
        (WalletState::selectedCurrency or WalletState::selectedWalletData) { state ->
            val selectedWalletData = state.selectedWalletData
            if (selectedWalletData != null) {
                walletDataWatcher.invoke(selectedWalletData)
                setupButtons(selectedWalletData, state.isExchangeServiceFeatureOn)
                setupAddressCard(selectedWalletData)
                handleWarnings(selectedWalletData)
            }
        }
        (WalletState::selectedCurrency or WalletState::isExchangeServiceFeatureOn) { state ->
            if (state.selectedWalletData != null) {
                setupButtons(state.selectedWalletData!!, state.isExchangeServiceFeatureOn)
            }
        }
        (WalletState::state or WalletState::error) { state ->
            setupNoInternetHandling(state.state, state.error)
        }
    }

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
        walletDataWatcher.clear()
        walletStateWatcher.clear()
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
        if (state.selectedWalletData == null) return
        walletStateWatcher.invoke(state)

        updateViewMeasurements()
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

    private fun setupSwipeRefresh(currencyData: BalanceWidgetData, currency: Currency) {
        binding.srlWalletDetails.setOnRefreshListener {
            if (currencyData.status != BalanceStatus.Loading && currencyData.status != BalanceStatus.Refreshing) {
                Analytics.send(Token.Refreshed())
                lifecycleScope.launch(Dispatchers.Default) {
                    val selectedUserWallet = userWalletsListManagerSafe?.selectedUserWalletSync
                    if (selectedUserWallet != null) {
                        walletCurrenciesManager.update(selectedUserWallet, currency)
                            .doOnResult {
                                withContext(Dispatchers.Main) {
                                    binding.srlWalletDetails.isRefreshing = false
                                }
                            }
                    } else {
                        val blockchainNetwork = BlockchainNetwork(
                            blockchain = currency.blockchain,
                            derivationPath = currency.derivationPath,
                            tokens = emptyList(),
                        )

                        store.dispatchOnMain(WalletAction.LoadWallet(blockchainNetwork))
                        store.dispatchOnMain(WalletAction.LoadFiatRate(coinsList = listOf(currency)))
                    }
                }
            }
        }

        binding.srlWalletDetails.isRefreshing = currencyData.status == BalanceStatus.Loading ||
            currencyData.status == BalanceStatus.Refreshing
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
            sendAllowed = selectedWallet.mainButton.enabled,
        )
        rowButtons.onTradeClick = {
            store.dispatch(
                WalletAction.DialogAction.ChooseTradeActionDialog(
                    buyAllowed = selectedWallet.isAvailableToBuy,
                    sellAllowed = selectedWallet.isAvailableToSell,
                    swapAllowed = selectedWallet.isAvailableToSwap,
                ),
            )
        }
    }

    private fun handleWarnings(selectedWallet: WalletData) = with(binding) {
        val converter = WalletWarningConverter(requireContext())
        val warningDetails = selectedWallet.assembleWarnings().map { converter.convert(it) }

        warningMessagesAdapter.submitList(warningDetails)
        rvWarningMessages.show(warningDetails.isNotEmpty())
    }

    private fun handleCurrencyIcon(currency: Currency) = with(binding.lWalletDetails.lBalance) {
        ivCurrency.load(
            currency = currency,
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

            tvReceiveMessage.text = when (val currency = state.currency) {
                is Currency.Blockchain -> tvReceiveMessage.getString(
                    id = R.string.address_qr_code_message_format,
                    currency.blockchain.fullName,
                    currency.currencySymbol,
                    currency.blockchain.fullName,
                )
                is Currency.Token -> tvReceiveMessage.getString(
                    id = R.string.address_qr_code_message_format,
                    currency.token.name,
                    currency.currencySymbol,
                    currency.blockchain.fullName,
                )
            }
        }
    }

    private fun setupNoInternetHandling(progressState: ProgressState, errorType: ErrorType?) {
        if (progressState == ProgressState.Error) {
            if (errorType == ErrorType.NoInternetConnection) {
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
                        data.amountToCreateAccount,
                        data.currencySymbol,
                    )
            }
            else -> {}
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_remove -> {
                store.state.walletState.selectedWalletData?.let { walletData ->
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
