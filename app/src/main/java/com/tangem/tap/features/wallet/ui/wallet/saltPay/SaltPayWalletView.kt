package com.tangem.tap.features.wallet.ui.wallet.saltPay

import android.view.LayoutInflater
import android.view.ViewGroup
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.common.extensions.debounce
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.common.ShimmerData
import com.tangem.tap.common.ShimmerRecyclerAdapter
import com.tangem.tap.common.extensions.animateVisibility
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.extensions.formatAmountAsSpannedString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.recyclerView.SpaceItemDecoration
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.features.wallet.ui.wallet.WalletView
import com.tangem.tap.features.wallet.ui.wallet.saltPay.rv.HistoryItemData
import com.tangem.tap.features.wallet.ui.wallet.saltPay.rv.HistoryTransactionData
import com.tangem.tap.features.wallet.ui.wallet.saltPay.rv.TxHistoryAdapter
import com.tangem.tap.mainScope
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.wallet.databinding.FragmentWalletBinding
import com.tangem.wallet.databinding.ItemSaltPayTxHistoryShimmerBinding
import com.tangem.wallet.databinding.LayoutSaltPayBalanceBinding
import com.tangem.wallet.databinding.LayoutSaltPayTxHistoryBinding
import com.tangem.wallet.databinding.LayoutSaltPayWalletBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import java.util.concurrent.atomic.AtomicBoolean

/**
[REDACTED_AUTHOR]
 */
class SaltPayWalletView : WalletView() {

    private lateinit var saltPayBinding: LayoutSaltPayWalletBinding
    private val actionDebouncer = debounce<Action>(500, mainScope) { store.dispatch(it) }

    private val balanceWidget: LayoutSaltPayBalanceBinding
        get() = saltPayBinding.lSaltPayBalance

    private val txWidget: LayoutSaltPayTxHistoryBinding
        get() = saltPayBinding.lSaltPayTxHistory

    private var initialized = AtomicBoolean(false)

    override fun changeWalletView(fragment: WalletFragment, binding: FragmentWalletBinding) {
        saltPayBinding = binding.lSaltPayWallet
        setFragment(fragment, binding)
        onViewCreated()
        showSaltPayView(binding)
    }

    override fun onViewCreated() {
        this.pullToRefreshListener = this::handlePullToRefresh
        prepareTxRecyclers()
    }

    private fun showSaltPayView(binding: FragmentWalletBinding) = with(binding) {
        rvWarningMessages.hide()
        rvPendingTransaction.hide()
        rvMultiwallet.hide()
        tvTwinCardNumber.hide()
        lCardBalance.root.hide()
        lSingleWalletBalance.root.hide()
        lAddress.root.hide()
        rowButtons.hide()
        btnAddToken.hide()
        pbLoadingUserTokens.hide()
        lSaltPayWallet.root.show()
    }

    private fun prepareTxRecyclers() {
        val vhViewFactory: (ViewGroup) -> ViewGroup = {
            val inflater = LayoutInflater.from(it.context)
            ItemSaltPayTxHistoryShimmerBinding.inflate(inflater, it, false).root
        }
        val adapter = ShimmerRecyclerAdapter(vhViewFactory)
        txWidget.rvTxHistoryShimmer.adapter = adapter
        txWidget.rvTxHistoryShimmer.addItemDecoration(SpaceItemDecoration.vertical(10F))

        txWidget.rvTxHistory.adapter = TxHistoryAdapter()
        txWidget.rvTxHistory.addItemDecoration(SpaceItemDecoration.vertical(10F))

        handleTxInit()
    }

    private fun handlePullToRefresh() {
        requestTxHistory(store.state.walletState)
    }

    override fun onNewState(state: WalletState) {
        if (!initialized.getAndSet(true)) requestTxHistory(state)
        setupBalanceWidget(state)
    }

    private fun setupBalanceWidget(state: WalletState) = with(balanceWidget) {
        val tokenData = state.primaryTokenData ?: return@with

        val appCurrency = store.state.globalState.appCurrency
        val mainProgressState = state.state

        if (mainProgressState == ProgressState.Loading) {
            veilBalance.veil()
            veilBalanceCrypto.veil()
        } else {
            veilBalanceCrypto.unVeil()
        }

        tvUnreachable.animateVisibility(show = mainProgressState == ProgressState.Error)
        veilBalanceCrypto.animateVisibility(show = mainProgressState != ProgressState.Error)

        if (tokenData.currencyData.fiatAmount == null) {
            actionDebouncer(WalletAction.LoadFiatRate())
        } else {
            veilBalance.unVeil()
            tvBalance.text = tokenData.currencyData.fiatAmount.formatAmountAsSpannedString(
                currencySymbol = appCurrency.symbol,
            )
        }

        tvBalanceCrypto.text = tokenData.currencyData.amountFormatted

        tvCurrencyName.text = appCurrency.code
        tvCurrencyName.setOnClickListener {
            store.dispatch(WalletAction.AppCurrencyAction.ChooseAppCurrency)
        }

        btnBuy.show(tokenData.isAvailableToBuy)
        btnBuy.setOnClickListener {
            store.dispatch(WalletAction.TradeCryptoAction.Buy(false))
        }
    }

    private fun requestTxHistory(state: WalletState) {
        scope.launch {
            val walletManager = state.primaryWalletManager as? EthereumWalletManager ?: return@launch
            val wallet = walletManager.wallet
            val token = wallet.getFirstToken() ?: return@launch
            val walletAddress = wallet.address
            // val walletAddress = "0xDA94Aae02a4Db0e09E1Cf240E3a0973ba89052cf"
            when (val result = walletManager.getTransactionHistory(walletAddress, wallet.blockchain, setOf(token))) {
                is Result.Success -> {
                    val dateAssociatedHistory = mutableMapOf<String, MutableList<HistoryTransactionData>>()

                    result.data
                        .filter { it.contractAddress == token.contractAddress }
                        .sortedByDescending { it.date?.timeInMillis ?: 0 }
                        .forEach {
                            val txData = HistoryTransactionData(it, walletAddress)
                            val list = dateAssociatedHistory[txData.date] ?: mutableListOf()
                            list.add(txData)
                            dateAssociatedHistory[txData.date] = list
                        }

                    val dataList = mutableListOf<HistoryItemData>()
                    dateAssociatedHistory.forEach { entry ->
                        dataList.add(HistoryItemData.Date(entry.key))
                        entry.value.forEach { dataList.add(HistoryItemData.TransactionData(it)) }
                    }
                    // .toMutableList().apply { clear() }

                    delay(300)
                    withMainContext {
                        if (dataList.isEmpty()) {
                            handleTxEmpty()
                        } else {
                            handleTxSuccess(dataList)
                        }
                    }
                }
                is Result.Failure -> {
                    delay(300)
                    withMainContext { handleTxError() }
                }
            }
        }
    }

    private fun handleTxInit() = with(txWidget) {
        (rvTxHistoryShimmer.adapter as? ShimmerRecyclerAdapter)?.submitList(
            listOf(
                ShimmerData(),
                ShimmerData(),
                ShimmerData(),
            ),
        )
        groupEmpty.hide()
        groupError.hide()
        groupSuccess.hide()
        root.beginDelayedTransition()
        txWidget.groupShimmer.show()
    }

    private fun handleTxSuccess(dataList: List<HistoryItemData>) = with(txWidget) {
        groupShimmer.hide()
        groupEmpty.hide()
        groupError.hide()
        root.beginDelayedTransition()
        updateTxHistoryWidget(dataList)
        groupSuccess.show()
    }

    private fun handleTxEmpty() = with(txWidget) {
        groupShimmer.hide()
        groupError.hide()
        groupSuccess.hide()
        root.beginDelayedTransition()
        updateTxHistoryWidget(emptyList())
        groupEmpty.show()
    }

    private fun handleTxError() = with(txWidget) {
        groupShimmer.hide()
        groupEmpty.hide()
        groupSuccess.hide()
        root.beginDelayedTransition()
        updateTxHistoryWidget(emptyList())
        groupError.show()
    }

    private fun updateTxHistoryWidget(dataList: List<HistoryItemData>) = with(txWidget) {
        (rvTxHistory.adapter as TxHistoryAdapter).submitList(dataList)
    }
}