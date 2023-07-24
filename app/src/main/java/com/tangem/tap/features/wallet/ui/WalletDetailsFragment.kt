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
import com.badoo.mvicore.DiffStrategy
import com.badoo.mvicore.ModelWatcher
import com.badoo.mvicore.modelWatcher
import com.tangem.common.doOnResult
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.feature.swap.api.SwapFeatureToggleManager
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.sdk.extensions.dpToPx
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.common.TestActions
import com.tangem.tap.common.analytics.events.DetailsScreen
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.appendIfNotNull
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.extensions.fitChipsByGroupWidth
import com.tangem.tap.common.extensions.getColor
import com.tangem.tap.common.extensions.getString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.extensions.toQrCode
import com.tangem.tap.common.recyclerView.SpaceItemDecoration
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.utils.SafeStoreSubscriber
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.tap.features.wallet.models.WalletWarning
import com.tangem.tap.features.wallet.redux.ErrorType
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.utils.UNKNOWN_AMOUNT_SIGN
import com.tangem.tap.features.wallet.ui.adapters.PendingTransactionsAdapter
import com.tangem.tap.features.wallet.ui.adapters.WalletDetailWarningMessagesAdapter
import com.tangem.tap.features.wallet.ui.images.load
import com.tangem.tap.features.wallet.ui.test.TestWallet
import com.tangem.tap.features.wallet.ui.utils.assembleWarnings
import com.tangem.tap.features.wallet.ui.utils.getAvailableActions
import com.tangem.tap.features.wallet.ui.utils.getFormattedCryptoAmount
import com.tangem.tap.features.wallet.ui.utils.getFormattedFiatAmount
import com.tangem.tap.features.wallet.ui.utils.isAvailableToBuy
import com.tangem.tap.features.wallet.ui.utils.isAvailableToSell
import com.tangem.tap.features.wallet.ui.utils.isAvailableToSwap
import com.tangem.tap.features.wallet.ui.utils.mainButton
import com.tangem.tap.features.wallet.ui.utils.shouldShowMultipleAddress
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManagerSafe
import com.tangem.tap.walletCurrenciesManager
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Wallet details fragment - use only for MultiWallet
 */
@Suppress("LargeClass", "MagicNumber")
@AndroidEntryPoint
class WalletDetailsFragment : Fragment(R.layout.fragment_wallet_details), SafeStoreSubscriber<WalletState> {

    @Inject
    lateinit var swapInteractor: SwapInteractor

    @Inject
    lateinit var swapFeatureToggleManager: SwapFeatureToggleManager

    private lateinit var pendingTransactionAdapter: PendingTransactionsAdapter
    private lateinit var warningMessagesAdapter: WalletDetailWarningMessagesAdapter

    private val binding: FragmentWalletDetailsBinding by viewBinding(FragmentWalletDetailsBinding::bind)

    private val walletDataWatcher: ModelWatcher<WalletDataModel> = modelWatcher {
        val addressCardStrategy: DiffStrategy<WalletDataModel> = { old, new ->
            old.currency != new.currency || old.walletAddresses != new.walletAddresses
        }

        WalletDataModel::currency {
            handleCurrencyIcon(it)
        }
        WalletDataModel::walletAddresses { walletAddresses ->
            setupCopyAndShareButtons(walletAddresses?.selectedAddress?.address)
        }
        WalletDataModel::currency { currency ->
            setupCurrency(currency)
        }
        watch({ it }, addressCardStrategy) { walletData ->
            setupAddressCard(
                shouldShowMultipleAddress = walletData.shouldShowMultipleAddress(),
                selectedAddress = walletData.walletAddresses?.selectedAddress,
                currency = walletData.currency,
            )
        }
    }

    private val walletStateWatcher: ModelWatcher<WalletState> = modelWatcher {
        val walletDataStrategy: DiffStrategy<WalletState> = { old, new ->
            new.walletsStores.isNotEmpty() &&
                new.selectedCurrency != null &&
                (old.selectedCurrency != new.selectedCurrency || old.walletsStores != new.walletsStores)
        }

        watch({ it }, walletDataStrategy) { state ->
            val selectedWallet = state.selectedWalletData
            if (selectedWallet != null) {
                setupBalanceData(selectedWallet)
                setupSwipeRefresh(selectedWallet)
                walletDataWatcher.invoke(selectedWallet)

                val walletStore = state.getWalletStore(state.selectedCurrency)
                if (walletStore != null) {
                    handleWarnings(
                        selectedWallet.assembleWarnings(
                            blockchainAmount = walletStore.blockchainWalletData.status.amount,
                            blockchainWalletRent = walletStore.walletRent,
                        ),
                    )
                }
            }
        }
        (WalletState::selectedWalletData or WalletState::isExchangeServiceFeatureOn) { state ->
            val selectedWallet = state.selectedWalletData
            if (selectedWallet != null) {
                val blockchainAmount: BigDecimal = state.getBlockchainAmount(selectedWallet.currency)
                setupButtonsRow(selectedWallet, state.isExchangeServiceFeatureOn, blockchainAmount)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

        setupTransactionsRecyclerView()
        setupButtons()
        setupWarningsRecyclerView()
        setupTestActionButton()
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state -> state.select(AppState::walletState) }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearWatchers()
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

    private fun setupButtons() {
        binding.rowButtons.onSendClick = { store.dispatch(WalletAction.Send()) }
    }

    private fun setupTestActionButton() {
        view?.findViewById<View>(R.id.l_balance)?.let { view ->
            TestActions.initFor(view = view, actions = TestWallet.solanaRentExemptWarning())
        }
    }

    override fun newStateOnMain(state: WalletState) {
        if (activity == null || view == null) return
        if (state.selectedWalletData == null) return
        walletStateWatcher.invoke(state)

        updateViewMeasurements()
    }

    private fun updateViewMeasurements() {
        val tvFiatAmount = binding.lWalletDetails.lBalance.tvFiatAmount
        val paddingStart = if (tvFiatAmount.text == UNKNOWN_AMOUNT_SIGN) 16f else 12f

        tvFiatAmount.setPadding(
            tvFiatAmount.dpToPx(paddingStart).toInt(),
            tvFiatAmount.paddingTop,
            tvFiatAmount.paddingEnd,
            tvFiatAmount.paddingBottom,
        )
    }

    private fun setupCurrency(currency: Currency) = with(binding) {
        tvCurrencyTitle.text = currency.currencyName

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

    private fun setupSwipeRefresh(walletData: WalletDataModel) {
        binding.srlWalletDetails.setOnRefreshListener {
            if (walletData.status !is WalletDataModel.Loading) {
                Analytics.send(Token.Refreshed())
                val selectedUserWallet = userWalletsListManagerSafe?.selectedUserWalletSync.guard {
                    Timber.e("Unable to refresh wallet details screen, no user wallet selected")
                    return@setOnRefreshListener
                }
                binding.srlWalletDetails.isRefreshing = true
                lifecycleScope.launch(Dispatchers.Default) {
                    walletCurrenciesManager.update(selectedUserWallet, walletData.currency).doOnResult {
                        withMainContext {
                            binding.srlWalletDetails.isRefreshing = false
                        }
                    }
                }
            }
        }
    }

    private fun setupCopyAndShareButtons(walletAddress: String?) {
        binding.lWalletDetails.btnCopy.setOnClickListener {
            if (walletAddress != null) store.dispatch(WalletAction.CopyAddress(walletAddress, requireContext()))
        }

        binding.lWalletDetails.btnShare.setOnClickListener {
            if (walletAddress != null) store.dispatch(WalletAction.ShareAddress(walletAddress, requireContext()))
        }
    }

    private fun setupButtonsRow(
        selectedWallet: WalletDataModel,
        isExchangeServiceFeatureOn: Boolean,
        blockchainAmount: BigDecimal,
    ) {
        val exchangeManager = store.state.globalState.exchangeManager
        binding.rowButtons.apply {
            onBuyClick = { store.dispatch(WalletAction.TradeCryptoAction.Buy()) }
            onSellClick = { store.dispatch(WalletAction.TradeCryptoAction.Sell) }
            onSwapClick = { store.dispatch(WalletAction.TradeCryptoAction.Swap) }
            onTradeClick = {
                store.dispatch(
                    WalletAction.DialogAction.ChooseTradeActionDialog(
                        buyAllowed = selectedWallet.isAvailableToBuy(exchangeManager),
                        sellAllowed = selectedWallet.isAvailableToSell(exchangeManager),
                        swapAllowed = selectedWallet.isAvailableToSwap(
                            swapFeatureToggleManager = swapFeatureToggleManager,
                            swapInteractor = swapInteractor,
                            isSingleWallet = false,
                        ),
                    ),
                )
            }
        }
        val actions = selectedWallet.getAvailableActions(
            swapInteractor = swapInteractor,
            exchangeManager = exchangeManager,
            swapFeatureToggleManager = swapFeatureToggleManager,
            isSingleWallet = false,
        )
        binding.rowButtons.updateButtonsVisibility(
            actions = actions,
            exchangeServiceFeatureOn = isExchangeServiceFeatureOn,
            sendAllowed = selectedWallet.mainButton(blockchainAmount).enabled,
        )
    }

    private fun handleWarnings(warnings: List<WalletWarning>) = with(binding) {
        val converter = WalletWarningConverter(requireContext())
        val warningDetails = warnings.map { converter.convert(it) }

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
        val knownTransactions = pendingTransactions.filterNot { it.type == PendingTransactionType.Unknown }
        pendingTransactionAdapter.submitList(knownTransactions)
        binding.rvPendingTransaction.show(knownTransactions.isNotEmpty())
    }

    private fun setupAddressCard(
        shouldShowMultipleAddress: Boolean,
        selectedAddress: WalletDataModel.AddressData?,
        currency: Currency,
    ) = with(binding.lWalletDetails) {
        if (selectedAddress == null) return@with

        setupAddressTypeChips(shouldShowMultipleAddress, selectedAddress, currency)

        tvAddress.text = selectedAddress.address
        tvExplore.setOnClickListener {
            store.dispatch(WalletAction.ExploreAddress(selectedAddress.exploreUrl, requireContext()))
        }
        ivQrCode.setImageBitmap(selectedAddress.shareUrl.toQrCode())

        tvReceiveMessage.text = when (currency) {
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

    private fun setupAddressTypeChips(
        shouldShowMultipleAddress: Boolean,
        selectedAddress: WalletDataModel.AddressData,
        currency: Currency,
    ) = with(binding.lWalletDetails) {
        if (shouldShowMultipleAddress && currency is Currency.Blockchain) {
            (cardBalance as? ViewGroup)?.beginDelayedTransition()
            chipGroupAddressType.show()
            chipGroupAddressType.fitChipsByGroupWidth()

            val checkedId = MultipleAddressUiHelper.typeToId(selectedAddress.type, currency.blockchain)
            if (checkedId != View.NO_ID) chipGroupAddressType.check(checkedId)

            chipGroupAddressType.setOnCheckedChangeListener { _, checkedId ->
                if (checkedId == -1) return@setOnCheckedChangeListener
                val type =
                    MultipleAddressUiHelper.idToType(checkedId, currency.blockchain)
                type?.let { store.dispatch(WalletAction.ChangeSelectedAddress(type)) }
            }
        } else {
            chipGroupAddressType.hide()
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

    private fun setupBalanceData(walletData: WalletDataModel) = with(binding.lWalletDetails) {
        when (val status = walletData.status) {
            is WalletDataModel.Loading -> {
                lBalanceError.root.hide()
                lBalance.root.show()
                lBalance.groupBalance.show()
                lBalance.tvError.hide()
                lBalance.tvAmount.text = walletData.getFormattedCryptoAmount()
                lBalance.tvFiatAmount.text = walletData.getFormattedFiatAmount(store.state.globalState.appCurrency)
                lBalance.tvStatus.setLoadingStatus(R.string.wallet_balance_loading)
            }
            is WalletDataModel.VerifiedOnline,
            is WalletDataModel.SameCurrencyTransactionInProgress,
            is WalletDataModel.TransactionInProgress,
            -> {
                lBalanceError.root.hide()
                lBalance.root.show()
                lBalance.groupBalance.show()
                lBalance.tvError.hide()
                lBalance.tvAmount.text = walletData.getFormattedCryptoAmount()
                lBalance.tvFiatAmount.text = walletData.getFormattedFiatAmount(store.state.globalState.appCurrency)
                when (status) {
                    is WalletDataModel.VerifiedOnline,
                    is WalletDataModel.SameCurrencyTransactionInProgress,
                    -> {
                        lBalance.tvStatus.setVerifiedBalanceStatus(R.string.wallet_balance_verified)
                    }

                    is WalletDataModel.TransactionInProgress -> {
                        lBalance.tvStatus.setWarningStatus(R.string.wallet_balance_tx_in_progress)
                        showPendingTransactionsIfPresent(status.pendingTransactions)
                    }

                    else -> Unit
                }
            }
            is WalletDataModel.Unreachable -> {
                lBalanceError.root.hide()
                lBalance.root.show()
                lBalance.groupBalance.hide()
                lBalance.tvError.show()
                lBalance.tvError.setWarningStatus(
                    R.string.wallet_balance_blockchain_unreachable,
                    status.errorMessage,
                )
            }
            is WalletDataModel.NoAccount -> {
                lBalance.root.hide()
                lBalanceError.root.show()
                lBalanceError.tvErrorTitle.text = getText(R.string.wallet_error_no_account)
                lBalanceError.tvErrorDescriptions.text =
                    getString(
                        R.string.no_account_generic,
                        status.amountToCreateAccount,
                        walletData.currency.currencySymbol,
                    )
            }

            else -> Unit
        }
    }

    @Deprecated("Deprecated in Java")
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

    @Deprecated(
        message = "Deprecated in Java",
        replaceWith = ReplaceWith("inflater.inflate(R.menu.menu_wallet_details, menu)", "com.tangem.wallet.R"),
    )
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_wallet_details, menu)
    }

    private fun clearWatchers() {
        walletDataWatcher.clear()
        walletStateWatcher.clear()
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