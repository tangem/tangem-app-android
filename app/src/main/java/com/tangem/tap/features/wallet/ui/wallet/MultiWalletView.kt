package com.tangem.tap.features.wallet.ui.wallet

import android.widget.Button
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangem.common.card.Card
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.tap.common.extensions.animateVisibility
import com.tangem.tap.common.extensions.formatAmountAsSpannedString
import com.tangem.tap.common.extensions.getQuantityString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.tokens.CurrenciesRepository
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.features.wallet.models.TotalBalance
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.features.wallet.ui.adapters.WalletAdapter
import com.tangem.tap.features.wallet.ui.view.WalletDetailsButtonsRow
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletBinding

class MultiWalletView : WalletView() {

    private lateinit var walletsAdapter: WalletAdapter

    override fun changeWalletView(fragment: WalletFragment, binding: FragmentWalletBinding) {
        setFragment(fragment, binding)
        onViewCreated()
        showMultiWalletView(binding)
    }

    private fun showMultiWalletView(binding: FragmentWalletBinding) = with(binding) {
        tvTwinCardNumber.hide()
        rvPendingTransaction.hide()
        lCardBalance.root.hide()
        lAddress.root.hide()
        rowButtons.hide()
        lSingleWalletBalance.root.hide()
        rvMultiwallet.show()
        btnAddToken.show()
        setupWalletCardNumber(binding)
    }

    private fun setupWalletCardNumber(binding: FragmentWalletBinding) = with(binding) {
        val card = store.state.globalState.scanResponse?.card
        if (card?.backupStatus is Card.BackupStatus.Active) {
            val cardCount = (card.backupStatus as Card.BackupStatus.Active).cardCount + 1
            tvTwinCardNumber.show()
            tvTwinCardNumber.text = tvTwinCardNumber.getQuantityString(R.plurals.card_label_card_count, cardCount)
        } else {
            tvTwinCardNumber.hide()
        }
    }

    override fun onViewCreated() {
        setupWalletsRecyclerView()
    }

    private fun setupWalletsRecyclerView() {
        val fragment = fragment ?: return
        walletsAdapter = WalletAdapter()
        walletsAdapter.setHasStableIds(true)
        binding?.rvMultiwallet?.layoutManager = LinearLayoutManager(fragment.requireContext())
        binding?.rvMultiwallet?.adapter = walletsAdapter
    }

    override fun onNewState(state: WalletState) {
        val fragment = fragment ?: return
        val binding = binding ?: return

        handleTotalBalance(binding, state.totalBalance)
        handleBackupWarning(binding, state.showBackupWarning)
        handleRescanWarning(binding, state.missingDerivations.isNotEmpty())
        walletsAdapter.submitList(state.walletsData, state.primaryBlockchain, state.primaryToken)

        binding.btnAddToken.setOnClickListener {
            val card = store.state.globalState.scanResponse!!.card
            store.dispatch(
                TokensAction.LoadCurrencies(
                    supportedBlockchains = CurrenciesRepository.getBlockchains(
                        card.firmwareVersion,
                        card.isTestCard,
                    ),
                    scanResponse = store.state.globalState.scanResponse,
                ),
            )
            store.dispatch(TokensAction.AllowToAddTokens(true))
            store.dispatch(
                TokensAction.SetAddedCurrencies(
                    wallets = state.walletsData,
                    derivationStyle = card.derivationStyle,
                ),
            )
            store.dispatch(NavigationAction.NavigateTo(AppScreen.AddTokens))
        }
        handleErrorStates(state = state, binding = binding, fragment = fragment)
    }

    private fun handleBackupWarning(
        binding: FragmentWalletBinding,
        showBackupWarning: Boolean,
    ) = with(binding.lWalletBackupWarning) {
        root.isVisible = showBackupWarning
        root.setOnClickListener {
            store.dispatch(WalletAction.MultiWallet.BackupWallet)
        }
    }

    private fun handleRescanWarning(
        binding: FragmentWalletBinding,
        showRescanWarning: Boolean,
    ) = with(binding.lWalletRescanWarning) {
        root.isVisible = showRescanWarning
        root.setOnClickListener {
            store.dispatch(WalletAction.MultiWallet.ScanToGetDerivations)
        }
    }

    private fun handleTotalBalance(
        binding: FragmentWalletBinding,
        totalBalance: TotalBalance?,
    ) = with(binding.lCardTotalBalance) {
        root.isVisible = totalBalance != null
        if (totalBalance != null) {
            // Skip changes when on refreshing state
            if (totalBalance.state == ProgressState.Refreshing) return@with

            if (totalBalance.state == ProgressState.Loading) {
                veilBalance.veil()
            } else {
                veilBalance.unVeil()
            }
            tvProcessing.animateVisibility(
                show = totalBalance.state == ProgressState.Error,
            )

            tvBalance.text = totalBalance.fiatAmount.formatAmountAsSpannedString(
                currencySymbol = totalBalance.fiatCurrency.symbol,
            )
            tvCurrencyName.text = totalBalance.fiatCurrency.code

            tvCurrencyName.setOnClickListener {
                store.dispatch(WalletAction.AppCurrencyAction.ChooseAppCurrency)
            }
        }
    }

    private fun handleErrorStates(
        state: WalletState,
        binding: FragmentWalletBinding,
        fragment: WalletFragment,
    ) {
        when (state.primaryWallet?.currencyData?.status) {
            BalanceStatus.EmptyCard -> {
                showErrorState(
                    binding,
                    fragment.getText(R.string.wallet_error_empty_card),
                    fragment.getString(R.string.wallet_error_empty_card_subtitle),
                )
                configureButtonsForEmptyWalletState(binding)
            }
            BalanceStatus.UnknownBlockchain -> {
                showErrorState(
                    binding,
                    fragment.getText(R.string.wallet_error_unsupported_blockchain),
                    fragment.getString(R.string.wallet_error_unsupported_blockchain_subtitle),
                )
            }
            else -> { /* no-op */
            }
        }
    }

    private fun showErrorState(
        binding: FragmentWalletBinding, errorTitle: CharSequence, errorDescription: CharSequence,
    ) = with(binding) {
        lCardBalance.root.show()
        with(lCardBalance) {
            lBalance.root.hide()
            lBalanceError.root.show()
            rvMultiwallet.show()
            btnAddToken.hide()
            lBalanceError.tvErrorTitle.text = errorTitle
            lBalanceError.tvErrorDescriptions.text = errorDescription
        }
    }

    private fun configureButtonsForEmptyWalletState(binding: FragmentWalletBinding) =
        with(binding) {
            rowButtons.btnBuy.hide()
            rowButtons.btnSell.hide()
            rowButtons.btnTrade.hide()
            rowButtons.show()

            rowButtons.btnSend.text = fragment?.getText(R.string.wallet_button_create_wallet)
            rowButtons.onSendClick = { store.dispatch(WalletAction.CreateWallet) }
        }
}

private val WalletDetailsButtonsRow.btnBuy: Button
    get() = this.findViewById(R.id.btn_buy)
private val WalletDetailsButtonsRow.btnSell: Button
    get() = this.findViewById(R.id.btn_sell)
private val WalletDetailsButtonsRow.btnTrade: Button
    get() = this.findViewById(R.id.btn_trade)
private val WalletDetailsButtonsRow.btnSend: Button
    get() = this.findViewById(R.id.btn_send)