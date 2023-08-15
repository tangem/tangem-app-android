package com.tangem.tap.features.wallet.ui.wallet

import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.badoo.mvicore.modelWatcher
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.analytics.events.Portfolio
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.getQuantityString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.features.tokens.legacy.redux.TokensAction
import com.tangem.tap.features.wallet.redux.ErrorType
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.features.wallet.ui.adapters.WalletAdapter
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentWalletBinding

class MultiWalletView : WalletView() {

    private lateinit var walletsAdapter: WalletAdapter

    private val watcher = modelWatcher<WalletState> {
        // !!! Workaround !!!
        // Checking state properties instead of state params can reduce application performance,
        // but here it is necessary because the WalletStore has an unsuitable equals method
        WalletState::walletsDataFromStores {
            walletsAdapter.submitList(it)
        }
        WalletState::loadingUserTokens {
            binding?.pbLoadingUserTokens?.show(it)
        }
        WalletState::walletCardsCount { walletCardsCount ->
            binding?.let {
                setupWalletCardNumber(it, walletCardsCount)
            }
        }
        WalletState::missingDerivations { missingDerivations ->
            binding?.let {
                handleRescanWarning(it, missingDerivations.isNotEmpty())
            }
        }
        WalletState::showBackupWarning { showBackupWarnings ->
            binding?.let {
                handleBackupWarning(it, showBackupWarnings)
            }
        }
        (WalletState::totalBalance or WalletState::walletsDataFromStores) { walletState ->
            binding?.let {
                handleTotalBalance(
                    binding = it,
                    totalBalance = walletState.totalBalance,
                    walletsCount = walletState.walletsDataFromStores.size,
                    appFiatCurrency = store.state.globalState.appCurrency,
                )
            }
        }
    }

    override fun changeWalletView(fragment: WalletFragment, binding: FragmentWalletBinding) {
        setFragment(fragment, binding)
        onViewCreated()
        showMultiWalletView(binding)
    }

    private fun showMultiWalletView(binding: FragmentWalletBinding) = with(binding) {
        watcher.clear()
        tvTwinCardNumber.hide()
        rvPendingTransaction.hide()
        lCardBalance.root.hide()
        lAddress.root.hide()
        rowButtons.hide()
        lSingleWalletBalance.root.hide()
        lCardTotalBalance.show()
        rvMultiwallet.show()
        btnAddToken.show()
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
        binding?.rvMultiwallet?.itemAnimator = null
    }

    override fun onNewState(state: WalletState) {
        val fragment = fragment ?: return
        val binding = binding ?: return

        watcher.invoke(state)

        binding.btnAddToken.setOnClickListener {
            Analytics.send(Portfolio.ButtonManageTokens())

            store.dispatch(
                TokensAction.SetArgs.ManageAccess(
                    wallets = state.walletsDataFromStores,
                    derivationStyle = store.state.globalState.scanResponse
                        ?.derivationStyleProvider?.getDerivationStyle(),
                ),
            )
            store.dispatch(NavigationAction.NavigateTo(AppScreen.AddTokens))
        }
        handleErrorStates(state = state, binding = binding, fragment = fragment)
    }

    private fun setupWalletCardNumber(binding: FragmentWalletBinding, walletCardsCount: Int?) = with(binding) {
        if (walletCardsCount != null) {
            tvTwinCardNumber.show()
            tvTwinCardNumber.text =
                tvTwinCardNumber.getQuantityString(R.plurals.card_label_card_count, walletCardsCount)
        } else {
            tvTwinCardNumber.hide()
        }
    }

    private fun handleBackupWarning(binding: FragmentWalletBinding, showBackupWarning: Boolean) =
        with(binding.lWalletBackupWarning) {
            root.isVisible = showBackupWarning
            root.setOnClickListener {
                Analytics.send(MainScreen.NoticeBackupYourWalletTapped())
                store.dispatch(WalletAction.MultiWallet.BackupWallet)
            }
        }

    private fun handleRescanWarning(binding: FragmentWalletBinding, showRescanWarning: Boolean) =
        with(binding.lWalletRescanWarning) {
            root.isVisible = showRescanWarning
            root.setOnClickListener {
                Analytics.send(MainScreen.NoticeScanYourCardTapped())
                store.dispatch(WalletAction.MultiWallet.ScanToGetDerivations)
            }
        }

    private fun handleTotalBalance(
        binding: FragmentWalletBinding,
        totalBalance: TotalFiatBalance?,
        walletsCount: Int,
        appFiatCurrency: FiatCurrency,
    ) = with(binding.lCardTotalBalance) {
        isVisible = walletsCount > 0

        onChangeFiatCurrencyClick = {
            store.dispatch(WalletAction.AppCurrencyAction.ChooseAppCurrency)
        }
        status = totalBalance
        fiatCurrency = appFiatCurrency
    }

    private fun handleErrorStates(state: WalletState, binding: FragmentWalletBinding, fragment: WalletFragment) {
        when (state.error) {
            ErrorType.UnknownBlockchain -> {
                showErrorState(
                    binding,
                    fragment.getText(R.string.wallet_error_unsupported_blockchain),
                    fragment.getString(R.string.wallet_error_unsupported_blockchain_subtitle),
                )
            }

            else -> Unit
        }
    }

    private fun showErrorState(
        binding: FragmentWalletBinding,
        errorTitle: CharSequence,
        errorDescription: CharSequence,
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

    override fun onDestroyFragment() {
        super.onDestroyFragment()
        watcher.clear()
    }
}
