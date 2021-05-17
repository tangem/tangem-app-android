package com.tangem.tap.features.wallet.ui.wallet

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.global.StateDialog
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
import com.tangem.tap.features.wallet.ui.dialogs.SignedHashesWarningDialog
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
        showSingleWalletView(fragment)
    }

    private fun showSingleWalletView(fragment: WalletFragment) = with(fragment) {
        rv_multiwallet.hide()
        btn_add_token.hide()
        btn_scan_multiwallet?.hide()
        rv_pending_transaction.hide()
        l_card_balance.show()
        l_address.show()
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
        val fragment = fragment ?: return
        state.primaryWallet ?: return

        setupTwinCards(state.twinCardsState, fragment)
        setupButtons(state.primaryWallet, state.twinCardsState != null, fragment)
        setupAddressCard(state.primaryWallet, fragment)
        showPendingTransactionsIfPresent(state.primaryWallet.pendingTransactions)
        setupBalance(state, state.primaryWallet)
        handleDialogs(state.walletDialog, fragment)
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

    private fun setupTwinCards(
            twinCardsState: TwinCardsState?, fragment: WalletFragment
    ) = with(fragment) {
        twinCardsState?.cardNumber?.let { cardNumber ->
            tv_twin_card_number.show()
            iv_twin_card.show()
            val number = when (cardNumber) {
                TwinCardNumber.First -> "1"
                TwinCardNumber.Second -> "2"
            }
            tv_twin_card_number.text =
                    this.getString(R.string.wallet_twins_chip_format, number)
        }
        if (twinCardsState?.cardNumber == null) {
            tv_twin_card_number.hide()
            iv_twin_card.hide()
        }
        if (twinCardsState?.showTwinOnboarding == true) {
            store.dispatch(NavigationAction.NavigateTo(AppScreen.TwinsOnboarding))
        }


    }

    private fun setupButtons(
            state: WalletData, isTwinsWallet: Boolean, fragment: WalletFragment
    ) = with(fragment){

        setupButtonsType(state, fragment)

        val btnConfirm = if (state.topUpState.allowed) {
            btn_confirm_short
        } else {
            btn_confirm_long
        }
        val btnScan = if (state.topUpState.allowed) {
            btn_scan_short
        } else {
            btn_scan_long
        }

        setupConfirmButton(state, btnConfirm, fragment, isTwinsWallet)

        btnScan.setOnClickListener {
            store.dispatch(WalletAction.Scan)
        }

        btn_copy.setOnClickListener {
            state.walletAddresses?.selectedAddress?.address?.let { addressString ->
                store.dispatch(WalletAction.CopyAddress(addressString, fragment.requireContext()))
            }
        }
        btn_show_qr.setOnClickListener { store.dispatch(WalletAction.ShowDialog.QrCode) }

        btn_top_up.setOnClickListener {
            store.dispatch(
                    WalletAction.TopUpAction.TopUp(fragment.requireContext(), R.color.backgroundLightGray)
            )
        }
    }

    private fun setupButtonsType(state: WalletData, fragment: WalletFragment) = with(fragment) {
        if (state.topUpState.allowed) {
            l_buttons_long.hide()
            l_buttons_short.show()
        } else {
            l_buttons_long.show()
            l_buttons_short.hide()
        }
    }

    private fun setupConfirmButton(
            state: WalletData, btnConfirm: Button, fragment: WalletFragment, isTwinsWallet: Boolean,
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


    private fun setupAddressCard(state: WalletData, fragment: WalletFragment) = with(fragment) {
        if (state.walletAddresses != null && state.blockchain != null) {
            l_address?.show()
            if (state.shouldShowMultipleAddress()) {
                (l_address as? ViewGroup)?.beginDelayedTransition()
                chip_group_address_type.show()
                chip_group_address_type.fitChipsByGroupWidth()

                val checkedId = MultipleAddressUiHelper.typeToId(state.walletAddresses.selectedAddress.type)
                if (checkedId != View.NO_ID) chip_group_address_type.check(checkedId)

                chip_group_address_type.setOnCheckedChangeListener { group, checkedId ->
                    if (checkedId == -1) return@setOnCheckedChangeListener
                    val type = MultipleAddressUiHelper.idToType(checkedId, state.blockchain)
                    type?.let { store.dispatch(WalletAction.ChangeSelectedAddress(type)) }
                }
            } else {
                chip_group_address_type.hide()
            }
            tv_address.text = state.walletAddresses.selectedAddress.address
            tv_explore?.setOnClickListener {
                store.dispatch(WalletAction.ExploreAddress(
                        state.walletAddresses.selectedAddress.exploreUrl,
                        fragment.requireContext()))
            }
        } else {
            l_address?.hide()
        }
    }

    private fun handleDialogs(walletDialog: StateDialog?, fragment: WalletFragment) {
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
                if (dialog == null) dialog = ScanFailsDialog.create(context).apply { show() }
            }
            is WalletDialog.SignedHashesMultiWalletDialog -> {
                if (dialog == null) {
                    dialog = SignedHashesWarningDialog.create(context).apply { show() }
                }
            }
            null -> {
                dialog?.dismiss()
                dialog = null
            }
        }
    }

}