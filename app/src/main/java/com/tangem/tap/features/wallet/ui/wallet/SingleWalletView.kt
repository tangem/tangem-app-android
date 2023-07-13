package com.tangem.tap.features.wallet.ui.wallet

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.*
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsState
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.BalanceWidget
import com.tangem.tap.features.wallet.ui.MultipleAddressUiHelper
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.features.wallet.ui.adapters.PendingTransactionsAdapter
import com.tangem.tap.features.wallet.ui.utils.*
import com.tangem.tap.features.wallet.ui.view.WalletDetailsButtonsRow
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletBinding

class SingleWalletView : WalletView() {
    private lateinit var pendingTransactionAdapter: PendingTransactionsAdapter
// [REDACTED_TODO_COMMENT]
    private var watchedPrimaryWalletForAddressCard: WalletDataModel? = null
    private var watchedPrimaryWalletForBalance: WalletDataModel? = null

    override fun changeWalletView(fragment: WalletFragment, binding: FragmentWalletBinding) {
        setFragment(fragment, binding)
        onViewCreated()
        showSingleWalletView(binding)
    }

    private fun showSingleWalletView(binding: FragmentWalletBinding) = with(binding) {
        watchedPrimaryWalletForAddressCard = null
        watchedPrimaryWalletForBalance = null
        tvTwinCardNumber.hide()
        rvMultiwallet.hide()
        btnAddToken.hide()
        rvPendingTransaction.hide()
        pbLoadingUserTokens.hide()
        lCardTotalBalance.hide()
        lSingleWalletBalance.root.hide()
        lWalletRescanWarning.root.hide()
        lWalletBackupWarning.root.hide()
        lCardBalance.root.show()
        lAddress.root.show()
        rowButtons.show()
    }

    override fun onViewCreated() {
        setupTransactionsRecyclerView()
    }

    override fun onDestroyFragment() {
        super.onDestroyFragment()
        watchedPrimaryWalletForAddressCard = null
        watchedPrimaryWalletForBalance = null
    }

    private fun setupTransactionsRecyclerView() {
        val fragment = fragment ?: return
        pendingTransactionAdapter = PendingTransactionsAdapter()
        binding?.rvPendingTransaction?.layoutManager =
            LinearLayoutManager(fragment.requireContext())
        binding?.rvPendingTransaction?.adapter = pendingTransactionAdapter
    }

    override fun onNewState(state: WalletState) {
        val binding = binding ?: return
        val primaryWalletData = state.primaryWalletData ?: return

        setupTwinCards(state.twinCardsState, binding)
        setupButtons(primaryWalletData, binding, state.isExchangeServiceFeatureOn)
        setupAddressCard(state, binding)
        showPendingTransactionsIfPresent(primaryWalletData.status.pendingTransactions)
        setupBalance(state, primaryWalletData)
    }

    private fun showPendingTransactionsIfPresent(pendingTransactions: List<PendingTransaction>) {
        val knownTransactions = pendingTransactions.filterNot {
            it.type == PendingTransactionType.Unknown
        }
        pendingTransactionAdapter.submitList(knownTransactions)
        binding?.rvPendingTransaction?.show(knownTransactions.isNotEmpty())
    }

    private fun setupBalance(state: WalletState, primaryWallet: WalletDataModel) {
        if (watchedPrimaryWalletForBalance == primaryWallet) return
        watchedPrimaryWalletForBalance = primaryWallet

        val fragment = fragment ?: return
        binding?.apply {
            lCardBalance.lBalance.root.show()
            BalanceWidget(
                binding = this.lCardBalance,
                fragment = fragment,
                blockchainWalletData = primaryWallet,
                tokenWalletData = state.primaryTokenData,
            ).setup()
        }
    }

    private fun setupTwinCards(twinCardsState: TwinCardsState?, binding: FragmentWalletBinding) = with(binding) {
        if (twinCardsState?.cardNumber == null) {
            tvTwinCardNumber.hide()
        } else {
            tvTwinCardNumber.show()
            tvTwinCardNumber.text = tvTwinCardNumber.getQuantityString(R.plurals.card_label_card_count, 2)
        }
    }

    private fun setupButtons(
        walletData: WalletDataModel,
        binding: FragmentWalletBinding,
        isExchangeServiceFeatureEnabled: Boolean,
    ) = with(binding) {
        setupRowButtons(walletData, rowButtons, isExchangeServiceFeatureEnabled)

        lAddress.btnCopy.setOnClickListener {
            walletData.walletAddresses?.selectedAddress?.address?.let { addressString ->
                store.dispatch(WalletAction.CopyAddress(addressString, fragment!!.requireContext()))
            }
        }
        lAddress.btnShowQr.setOnClickListener {
            Analytics.send(Token.ShowWalletAddress)

            walletData.walletAddresses?.selectedAddress?.let { selectedAddress ->
                store.dispatch(
                    WalletAction.DialogAction.QrCode(
                        currency = walletData.currency,
                        selectedAddress = selectedAddress,
                    ),
                )
            }
        }
    }

    private fun setupRowButtons(
        walletData: WalletDataModel,
        rowButtons: WalletDetailsButtonsRow,
        isExchangeServiceFeatureEnabled: Boolean,
    ) {
        val swapInteractor = this.swapInteractor ?: return
        val swapFeatureToggleManager = this.swapFeatureToggleManager ?: return

        val exchangeManager = store.state.globalState.exchangeManager
        binding?.rowButtons?.apply {
            onBuyClick = { store.dispatch(WalletAction.TradeCryptoAction.Buy()) }
            onSellClick = { store.dispatch(WalletAction.TradeCryptoAction.Sell) }
            onSwapClick = { store.dispatch(WalletAction.TradeCryptoAction.Swap) }
            onTradeClick = {
                store.dispatch(
                    WalletAction.DialogAction.ChooseTradeActionDialog(
                        buyAllowed = walletData.isAvailableToBuy(exchangeManager),
                        sellAllowed = walletData.isAvailableToSell(exchangeManager),
                        swapAllowed = false, // always disable for single wallet
                    ),
                )
            }
        }
        val actions = walletData.getAvailableActions(
            swapInteractor = swapInteractor,
            exchangeManager = exchangeManager,
            swapFeatureToggleManager = swapFeatureToggleManager,
            isSingleWallet = true,
        )
        binding?.rowButtons?.updateButtonsVisibility(
            actions = actions,
            exchangeServiceFeatureOn = isExchangeServiceFeatureEnabled,
            sendAllowed = walletData.mainButton(walletData.status.amount).enabled,
        )

        rowButtons.onSendClick = { store.dispatch(WalletAction.Send()) }
    }

    private fun setupAddressCard(state: WalletState, binding: FragmentWalletBinding) = with(binding.lAddress) {
        val primaryWallet = state.primaryWalletData
        if (primaryWallet == watchedPrimaryWalletForAddressCard) return@with
        watchedPrimaryWalletForAddressCard = primaryWallet

        if (primaryWallet?.walletAddresses != null && primaryWallet.currency is Currency.Blockchain) {
            binding.lAddress.root.show()
            if (primaryWallet.shouldShowMultipleAddress()) {
                (binding.lAddress.root as? ViewGroup)?.beginDelayedTransition()
                chipGroupAddressType.show()
                chipGroupAddressType.fitChipsByGroupWidth()
                val checkedId = MultipleAddressUiHelper.typeToId(
                    primaryWallet.walletAddresses.selectedAddress.type,
                    primaryWallet.currency.blockchain,
                )
                if (checkedId != View.NO_ID) chipGroupAddressType.check(checkedId)

                chipGroupAddressType.setOnCheckedChangeListener { group, checkedId ->
                    if (checkedId == -1) return@setOnCheckedChangeListener
                    val type = MultipleAddressUiHelper.idToType(checkedId, primaryWallet.currency.blockchain)
                    type?.let { store.dispatch(WalletAction.ChangeSelectedAddress(type)) }
                }
            } else {
                chipGroupAddressType.hide()
            }
            tvAddress.text = primaryWallet.walletAddresses.selectedAddress.address
            tvExplore.setOnClickListener {
                store.dispatch(
                    WalletAction.ExploreAddress(
                        primaryWallet.walletAddresses.selectedAddress.exploreUrl,
                        fragment!!.requireContext(),
                    ),
                )
            }
            setupCardInfo(primaryWallet)
        } else {
            binding.lAddress.root.hide()
        }
    }

    private fun setupCardInfo(walletData: WalletDataModel) {
        val textView = binding?.lAddress?.tvInfo
        val blockchain = walletData.currency.blockchain
        if (textView != null) {
            textView.text = textView.getString(
                id = R.string.address_qr_code_message_format,
                blockchain.fullName,
                blockchain.currency,
                blockchain.fullName,
            )
        }
    }
}
