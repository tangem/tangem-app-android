package com.tangem.tap.features.wallet.ui.wallet

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangem.domain.common.TwinCardNumber
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.extensions.fitChipsByGroupWidth
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsState
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.BalanceWidget
import com.tangem.tap.features.wallet.ui.MultipleAddressUiHelper
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.features.wallet.ui.adapters.PendingTransactionsAdapter
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
        setupButtons(state.primaryWallet, state.isTangemTwins, binding)
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

    private fun setupTwinCards(
        twinCardsState: TwinCardsState?, binding: FragmentWalletBinding,
    ) = with(binding) {
        twinCardsState?.cardNumber?.let { cardNumber ->
            tvTwinCardNumber.show()
            val number = when (cardNumber) {
                TwinCardNumber.First -> 1
                TwinCardNumber.Second -> 2
            }
            tvTwinCardNumber.text =
                fragment?.getString(R.string.wallet_twins_chip_format, number, 2)
        }
        if (twinCardsState?.cardNumber == null) {
            tvTwinCardNumber.hide()
        }
    }

    private fun setupButtons(
        state: WalletData, isTwinsWallet: Boolean, binding: FragmentWalletBinding,
    ) = with(binding) {
        setupButtonsType(state, binding)
        val tradeState = state.tradeCryptoState
        val btnConfirm = if (tradeState.isAvailableToSell() || tradeState.isAvailableToBuy()) {
            lButtonsShort.btnConfirm
        } else {
            lButtonsLong.btnConfirmLong
        }

        setupConfirmButton(state, btnConfirm, isTwinsWallet)

        lAddress.btnCopy.setOnClickListener {
            state.walletAddresses?.selectedAddress?.address?.let { addressString ->
                store.dispatch(WalletAction.CopyAddress(addressString, fragment!!.requireContext()))
            }
        }
        lAddress.btnShowQr.setOnClickListener {
            state.walletAddresses?.selectedAddress?.let { selectedAddress ->
                store.dispatch(
                    WalletAction.DialogAction.QrCode(
                        currency = state.currency,
                        selectedAddress = selectedAddress,
                    ),
                )
            }
        }

        setupTradeButton(binding, state.tradeCryptoState)
    }

    private fun setupTradeButton(binding: FragmentWalletBinding, tradeCryptoState: TradeCryptoState) {
        val allowedToBuy = tradeCryptoState.isAvailableToBuy()
        val allowedToSell = tradeCryptoState.isAvailableToSell()
        val action = when {
            allowedToBuy && !allowedToSell -> WalletAction.TradeCryptoAction.Buy()
            !allowedToBuy && allowedToSell -> WalletAction.TradeCryptoAction.Sell
            allowedToBuy && allowedToSell -> WalletAction.DialogAction.ChooseTradeActionDialog
            else -> null
        }
        val text = when {
            allowedToBuy && !allowedToSell -> R.string.wallet_button_buy
            !allowedToBuy && allowedToSell -> R.string.wallet_button_sell
            allowedToBuy && allowedToSell -> R.string.wallet_button_trade
            else -> R.string.wallet_button_trade
        }
        val icon = when {
            allowedToBuy && !allowedToSell -> R.drawable.ic_arrow_up
            !allowedToBuy && allowedToSell -> R.drawable.ic_arrow_down
            allowedToBuy && allowedToSell -> R.drawable.ic_arrows_up_down
            else -> null
        }
        with(binding) {
            lButtonsShort.btnTrade.text = fragment?.getText(text)
            icon?.let { lButtonsShort.btnTrade.setIconResource(it) }
            lButtonsShort.btnTrade.setOnClickListener { if (action != null) store.dispatch(action) }
        }
    }

    private fun setupButtonsType(state: WalletData, binding: FragmentWalletBinding) = with(binding) {
        if (state.tradeCryptoState.isAvailableToSell() || state.tradeCryptoState.isAvailableToBuy()) {
            lButtonsLong.root.hide()
            lButtonsShort.root.show()
        } else {
            lButtonsLong.root.show()
            lButtonsShort.root.hide()
        }
    }

    private fun setupConfirmButton(
        state: WalletData, btnConfirm: Button, isTwinsWallet: Boolean,
    ) {
        val buttonTitle = when (state.mainButton) {
            is WalletMainButton.SendButton -> R.string.wallet_button_send
            is WalletMainButton.CreateWalletButton -> {
                if (!isTwinsWallet) {
                    R.string.wallet_button_create_wallet
                } else {
                    R.string.wallet_button_create_twin_wallet
                }
            }
        }
        btnConfirm.text = fragment?.getString(buttonTitle)
        btnConfirm.isEnabled = state.mainButton.enabled
        btnConfirm.setOnClickListener {
            when (state.mainButton) {
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
