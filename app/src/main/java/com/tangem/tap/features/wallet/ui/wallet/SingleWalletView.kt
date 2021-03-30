package com.tangem.tap.features.wallet.ui.wallet

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.BalanceWidget
import com.tangem.tap.features.wallet.ui.MultipleAddressUiHelper
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.features.wallet.ui.adapters.PendingTransactionsAdapter
import com.tangem.tap.features.wallet.ui.dialogs.AmountToSendDialog
import com.tangem.tap.features.wallet.ui.dialogs.QrDialog
import com.tangem.tap.features.wallet.ui.dialogs.ScanFailsDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.card_balance.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.layout_address.*
import kotlinx.android.synthetic.main.layout_wallet_long_buttons.*
import kotlinx.android.synthetic.main.layout_wallet_short_buttons.*

class SingleWalletView : WalletView {

    private lateinit var pendingTransactionAdapter: PendingTransactionsAdapter
    private var fragment: WalletFragment? = null
    private var dialog: Dialog? = null

    override fun setFragment(fragment: WalletFragment) {
        this.fragment = fragment
    }

    override fun removeFragment() {
        fragment = null
    }

    override fun changeWalletView(fragment: WalletFragment) {
        setFragment(fragment)
        onViewCreated()
        showSingleWalletView()
    }

    private fun showSingleWalletView() {
        val fragment = fragment ?: return
        fragment.rv_multiwallet.hide()
        fragment.btn_add_token.hide()
        fragment.btn_scan_multiwallet?.hide()
        fragment.rv_pending_transaction.hide()
        fragment.l_card_balance.show()
        fragment.l_address.show()
    }

    override fun onViewCreated() {
        setupTransactionsRecyclerView()
    }


    private fun setupTransactionsRecyclerView() {
        val fragment = fragment ?: return
        pendingTransactionAdapter = PendingTransactionsAdapter()
        fragment.rv_pending_transaction.layoutManager = LinearLayoutManager(fragment.requireContext())
        fragment.rv_pending_transaction.adapter = pendingTransactionAdapter
    }

    override fun onNewState(state: WalletState) {
        setupTwinCards(state.twinCardsState)
        state.primaryWallet ?: return
        setupButtons(state.primaryWallet, state.twinCardsState != null)
        setupAddressCard(state.primaryWallet)
        showPendingTransactionsIfPresent(state.primaryWallet.pendingTransactions)
        setupBalance(state, state.primaryWallet)
        handleDialogs(state.walletDialog)
    }

    private fun showPendingTransactionsIfPresent(pendingTransactions: List<PendingTransaction>) {
        pendingTransactionAdapter.submitList(pendingTransactions)
        fragment?.rv_pending_transaction?.show(pendingTransactions.isNotEmpty())
    }


    private fun setupBalance(state: WalletState, primaryWallet: WalletData) {
        fragment?.apply {
            this.l_balance.show()
            BalanceWidget(this, primaryWallet.currencyData, state.twinCardsState != null).setup()
        }
    }

    private fun setupTwinCards(twinCardsState: TwinCardsState?) {
        fragment?.apply {
            twinCardsState?.cardNumber?.let { cardNumber ->
                this.tv_twin_card_number.show()
                this.iv_twin_card.show()
                val number = when (cardNumber) {
                    TwinCardNumber.First -> "1"
                    TwinCardNumber.Second -> "2"
                }
                this.tv_twin_card_number.text =
                        this.getString(R.string.wallet_twins_chip_format, number)
            }
            if (twinCardsState?.cardNumber == null) {
                this.tv_twin_card_number.hide()
                this.iv_twin_card.hide()
            }
            if (twinCardsState?.showTwinOnboarding == true) {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.TwinsOnboarding))
            }
        }

    }

    private fun setupButtons(state: WalletData, isTwinsWallet: Boolean) {
        val fragment = fragment ?: return

        setupButtonsType(state, fragment)

        val btnConfirm = if (state.topUpState.allowed) {
            fragment.btn_confirm_short
        } else {
            fragment.btn_confirm_long
        }
        val btnScan = if (state.topUpState.allowed) {
            fragment.btn_scan_short
        } else {
            fragment.btn_scan_long
        }

        setupConfirmButton(state, btnConfirm, fragment, isTwinsWallet)

        btnScan.setOnClickListener {
            store.dispatch(WalletAction.Scan)
        }

        fragment.btn_copy.setOnClickListener {
            state.walletAddresses?.selectedAddress?.address?.let { addressString ->
                store.dispatch(WalletAction.CopyAddress(addressString, fragment.requireContext()))
            }
        }
        fragment.btn_show_qr.setOnClickListener { store.dispatch(WalletAction.ShowDialog.QrCode) }

        fragment.btn_top_up.setOnClickListener {
            store.dispatch(
                    WalletAction.TopUpAction.TopUp(fragment.requireContext(), R.color.backgroundLightGray)
            )
        }
    }

    private fun setupButtonsType(state: WalletData, fragment: WalletFragment) {
        if (state.topUpState.allowed) {
            fragment.l_buttons_long.hide()
            fragment.l_buttons_short.show()
        } else {
            fragment.l_buttons_long.show()
            fragment.l_buttons_short.hide()
        }
    }

    private fun setupConfirmButton(
            state: WalletData, btnConfirm: Button, fragment: WalletFragment, isTwinsWallet: Boolean
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
        btnConfirm.text = fragment.getString(buttonTitle)
        btnConfirm.isEnabled = state.mainButton.enabled
        btnConfirm.setOnClickListener {
            when (state.mainButton) {
                is WalletMainButton.SendButton -> store.dispatch(WalletAction.Send())
                is WalletMainButton.CreateWalletButton -> store.dispatch(WalletAction.CreateWallet)
            }
        }
    }


    private fun setupAddressCard(state: WalletData) {
        val fragment = fragment ?: return
        if (state.walletAddresses != null) {
            fragment.l_address?.show()
            if (state.shouldShowMultipleAddress()) {
                (fragment.l_address as? ViewGroup)?.beginDelayedTransition()
                fragment.chip_group_address_type.show()
                fragment.chip_group_address_type.fitChipsByGroupWidth()

                val checkedId = MultipleAddressUiHelper.typeToId(state.walletAddresses.selectedAddress.type)
                if (checkedId != View.NO_ID) fragment.chip_group_address_type.check(checkedId)

                fragment.chip_group_address_type.setOnCheckedChangeListener { group, checkedId ->
                    if (checkedId == -1) return@setOnCheckedChangeListener
                    val type = MultipleAddressUiHelper.idToType(checkedId, state.currencyData.currencySymbol)
                    type?.let { store.dispatch(WalletAction.ChangeSelectedAddress(type)) }
                }
            } else {
                fragment.chip_group_address_type.hide()
            }
            fragment.tv_address.text = state.walletAddresses.selectedAddress.address
            fragment.tv_explore?.setOnClickListener {
                store.dispatch(WalletAction.ExploreAddress(
                        state.walletAddresses.selectedAddress.exploreUrl,
                        fragment.requireContext()))
            }
        } else {
            fragment.l_address?.hide()
        }
    }

    private fun handleDialogs(walletDialog: WalletDialog?) {
        val fragment = fragment ?: return
        val context = fragment.context ?: return
        when (walletDialog) {
            is WalletDialog.QrDialog -> {
                if (walletDialog.qrCode != null && walletDialog.shareUrl != null) {
                    if (dialog == null) dialog = QrDialog(context).apply {
                        this.showQr(
                                walletDialog.qrCode, walletDialog.shareUrl, walletDialog.currencyName
                        )
                    }
                }
            }
            is WalletDialog.SelectAmountToSendDialog -> {
                if (dialog == null) dialog = AmountToSendDialog(context).apply {
                    this.show(walletDialog.amounts)
                }
            }
            is WalletDialog.ScanFailsDialog -> {
                if (dialog == null) dialog = ScanFailsDialog.create(context).apply {
                    this.show()
                }
            }
            null -> {
                dialog?.dismiss()
                dialog = null
            }
        }
    }

}