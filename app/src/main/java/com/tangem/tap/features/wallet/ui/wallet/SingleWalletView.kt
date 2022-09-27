package com.tangem.tap.features.wallet.ui.wallet

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangem.domain.common.TwinCardNumber
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.extensions.fitChipsByGroupWidth
import com.tangem.tap.common.extensions.getQuantityString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsState
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletMainButton
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.BalanceWidget
import com.tangem.tap.features.wallet.ui.MultipleAddressUiHelper
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.features.wallet.ui.adapters.PendingTransactionsAdapter
import com.tangem.tap.features.wallet.ui.view.WalletDetailsButtonsRow
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletBinding

class SingleWalletView : WalletView() {
    private lateinit var pendingTransactionAdapter: PendingTransactionsAdapter
    override fun changeWalletView(fragment: WalletFragment, binding: FragmentWalletBinding) {
        setFragment(fragment, binding)
        onViewCreated()
        showSingleWalletView(binding)
    }

    private fun showSingleWalletView(binding: FragmentWalletBinding) = with(binding) {
        rvMultiwallet.hide()
        btnAddToken.hide()
        rvPendingTransaction.hide()
        lCardBalance.root.show()
        lAddress.root.show()
        lSingleWalletBalance.root.hide()
    }

    override fun onViewCreated() {
        setupTransactionsRecyclerView()
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
        state.primaryWallet ?: return

        setupTwinCards(state.twinCardsState, binding)
        setupButtons(state.primaryWallet, binding, state.isExchangeServiceFeatureOn)
        setupAddressCard(state.primaryWallet, binding)
        showPendingTransactionsIfPresent(state.primaryWallet.pendingTransactions)
        setupBalance(state, state.primaryWallet)
    }

    private fun showPendingTransactionsIfPresent(pendingTransactions: List<PendingTransaction>) {
        pendingTransactionAdapter.submitList(pendingTransactions)
        binding?.rvPendingTransaction?.show(pendingTransactions.isNotEmpty())
    }

    private fun setupBalance(state: WalletState, primaryWallet: WalletData) {
        val fragment = fragment ?: return
        binding?.apply {
            lCardBalance.lBalance.root.show()
            BalanceWidget(
                binding = this.lCardBalance,
                fragment = fragment,
                data = primaryWallet.currencyData,
                isTwinCard = state.isTangemTwins,
            ).setup()
        }
    }

    private fun setupTwinCards(twinCardsState: TwinCardsState?, binding: FragmentWalletBinding) = with(binding) {
        twinCardsState?.cardNumber?.let { cardNumber ->
            tvTwinCardNumber.show()
            tvTwinCardNumber.text = tvTwinCardNumber.getQuantityString(R.plurals.card_label_card_count, 2)
        }
        if (twinCardsState?.cardNumber == null) {
            tvTwinCardNumber.hide()
        }
    }

    private fun setupButtons(
        walletData: WalletData,
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
        walletData: WalletData,
        rowButtons: WalletDetailsButtonsRow,
        isExchangeServiceFeatureEnabled: Boolean,
    ) {
        rowButtons.updateButtonsVisibility(
            exchangeServiceFeatureOn = isExchangeServiceFeatureEnabled,
            buyAllowed = walletData.isAvailableToBuy,
            sellAllowed = walletData.isAvailableToSell,
            sendAllowed = walletData.mainButton.enabled,
        )

        rowButtons.onBuyClick = { store.dispatch(WalletAction.TradeCryptoAction.Buy()) }
        rowButtons.onSendClick = { store.dispatch(WalletAction.TradeCryptoAction.Sell) }
        rowButtons.onTradeClick = { store.dispatch(WalletAction.DialogAction.ChooseTradeActionDialog) }

        rowButtons.onSendClick = {
            when (walletData.mainButton) {
                is WalletMainButton.SendButton -> store.dispatch(WalletAction.Send())
                is WalletMainButton.CreateWalletButton -> store.dispatch(WalletAction.CreateWallet)
            }
        }
    }

    private fun setupAddressCard(state: WalletData, binding: FragmentWalletBinding) = with(binding.lAddress) {
        if (state.walletAddresses != null && state.currency is Currency.Blockchain) {
            binding.lAddress.root.show()
            if (state.shouldShowMultipleAddress()) {
                (binding.lAddress.root as? ViewGroup)?.beginDelayedTransition()
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
                        fragment!!.requireContext(),
                    ),
                )
            }
        } else {
            binding.lAddress.root.hide()
        }
    }
}
