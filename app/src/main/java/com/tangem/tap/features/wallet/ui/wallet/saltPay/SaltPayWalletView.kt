package com.tangem.tap.features.wallet.ui.wallet.saltPay

import android.view.LayoutInflater
import android.view.ViewGroup
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.common.ShimmerData
import com.tangem.tap.common.ShimmerRecyclerAdapter
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.extensions.animateVisibility
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.recyclerView.SpaceItemDecoration
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.utils.getFormattedAmount
import com.tangem.tap.features.wallet.ui.utils.getFormattedFiatAmount
import com.tangem.tap.features.wallet.ui.utils.isAvailableToBuy
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.features.wallet.ui.wallet.WalletView
import com.tangem.tap.features.wallet.ui.wallet.saltPay.rv.HistoryItemData
import com.tangem.tap.features.wallet.ui.wallet.saltPay.rv.HistoryTransactionData
import com.tangem.tap.features.wallet.ui.wallet.saltPay.rv.TxHistoryAdapter
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.wallet.databinding.FragmentWalletBinding
import com.tangem.wallet.databinding.ItemSaltPayTxHistoryShimmerBinding
import com.tangem.wallet.databinding.LayoutSaltPayBalanceBinding
import com.tangem.wallet.databinding.LayoutSaltPayTxHistoryBinding
import com.tangem.wallet.databinding.LayoutSaltPayWalletBinding
import kotlinx.coroutines.launch
import timber.log.Timber

/**
* [REDACTED_AUTHOR]
 */
class SaltPayWalletView : WalletView() {

    private var saltPayBinding: LayoutSaltPayWalletBinding? = null

    private val balanceWidget: LayoutSaltPayBalanceBinding?
        get() = saltPayBinding?.lSaltPayBalance

    private val txWidget: LayoutSaltPayTxHistoryBinding?
        get() = saltPayBinding?.lSaltPayTxHistory

    private val itemDecorator = SpaceItemDecoration.vertical(10F)

    override fun changeWalletView(fragment: WalletFragment, binding: FragmentWalletBinding) {
        Timber.d("changeWalletView")
        saltPayBinding = binding.lSaltPayWallet
        showSaltPayView(binding)
        onViewCreated()
    }

    override fun onViewCreated() {
        Timber.d("onViewCreated")
        prepareTxRecyclers(txWidget!!)
    }

    private fun showSaltPayView(binding: FragmentWalletBinding) = with(binding) {
        Timber.d("showSaltPayView")
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

    private fun prepareTxRecyclers(txWidget: LayoutSaltPayTxHistoryBinding) = with(txWidget) {
        Timber.d("prepareTxRecyclers")
        val vhViewFactory: (ViewGroup) -> ViewGroup = {
            val inflater = LayoutInflater.from(it.context)
            ItemSaltPayTxHistoryShimmerBinding.inflate(inflater, it, false).root
        }
        val adapter = ShimmerRecyclerAdapter(vhViewFactory)
        rvTxHistoryShimmer.adapter = adapter
        rvTxHistoryShimmer.addItemDecoration(itemDecorator)
        adapter.submitList(
            listOf(
                ShimmerData(),
                ShimmerData(),
                ShimmerData(),
            ),
        )

        rvTxHistory.adapter = TxHistoryAdapter()
        rvTxHistory.addItemDecoration(itemDecorator)
    }

    override fun onNewState(state: WalletState) {
        val balanceWidget = balanceWidget ?: return
        val txWidget = txWidget ?: return

        val exchangeManager = store.state.globalState.exchangeManager
        setupBalanceWidget(balanceWidget, exchangeManager, state)
        setupTxHistoryWidget(txWidget, state)
    }

    private fun setupBalanceWidget(
        balanceWidget: LayoutSaltPayBalanceBinding,
        exchangeManager: CurrencyExchangeManager,
        state: WalletState,
    ) = with(balanceWidget) {
        Timber.d("setupBalanceWidget")
        val tokenData = state.primaryTokenData ?: return

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

        if (tokenData.fiatRate == null) {
            veilBalance.veil()
        } else {
            veilBalance.unVeil()
            tvBalance.text = tokenData.getFormattedFiatAmount(appCurrency)
        }

        tvBalanceCrypto.text = tokenData.getFormattedAmount()

        tvCurrencyName.text = appCurrency.code
        tvCurrencyName.setOnClickListener {
            store.dispatch(WalletAction.AppCurrencyAction.ChooseAppCurrency)
        }

        btnBuy.show(tokenData.isAvailableToBuy(exchangeManager))
        btnBuy.setOnClickListener {
            Analytics.send(MainScreen.ButtonBuy())
            store.dispatch(WalletAction.TradeCryptoAction.Buy(false))
        }
    }

    private fun setupTxHistoryWidget(txWidget: LayoutSaltPayTxHistoryBinding, state: WalletState) {
        Timber.d("setupTxHistoryWidget")
        if (state.state == ProgressState.Loading) {
            handleTxLoading(txWidget)
            return
        }
        if (state.state == ProgressState.Error) {
            handleTxError(txWidget)
            return
        }
        if (state.state != ProgressState.Done) return
        val walletAddress = state.primaryWalletManager?.wallet?.address ?: return
        val tokenData = state.primaryTokenData ?: return
        val historyTransactions = tokenData.historyTransactions ?: return
        if (historyTransactions.isEmpty()) {
            handleTxEmpty(txWidget)
            return
        }

        scope.launch {
            val dateAssociatedHistory = mutableListOf<Pair<String, MutableList<HistoryTransactionData>>>()
            historyTransactions.forEach { tx ->
                val txHistoryData = HistoryTransactionData(tx, walletAddress)
                val item = dateAssociatedHistory.firstOrNull { it.first == txHistoryData.date }
                if (item == null) {
                    dateAssociatedHistory.add(Pair(txHistoryData.date, mutableListOf(txHistoryData)))
                } else {
                    item.second.add(txHistoryData)
                }
            }

            val rvDataList = mutableListOf<HistoryItemData>()
            dateAssociatedHistory.forEach { (txData, txHistoryData) ->
                rvDataList.add(HistoryItemData.Date(txData))
                rvDataList.addAll(txHistoryData.map { HistoryItemData.TransactionData(it) })
            }

            withMainContext { handleTxSuccess(txWidget, rvDataList) }
        }
    }

    private fun handleTxLoading(txWidget: LayoutSaltPayTxHistoryBinding) = with(txWidget) {
        Timber.d("handleTxLoading")
        groupEmpty.hide()
        groupError.hide()
        groupSuccess.hide()
        // root.beginDelayedTransition()
        groupShimmer.show()
    }

    private fun handleTxSuccess(txWidget: LayoutSaltPayTxHistoryBinding, dataList: List<HistoryItemData>) =
        with(txWidget) {
            Timber.d("handleTxSuccess")
            groupShimmer.hide()
            groupEmpty.hide()
            groupError.hide()
            // root.beginDelayedTransition()
            updateTxHistoryWidget(txWidget, dataList)
            groupSuccess.show()
        }

    private fun handleTxEmpty(txWidget: LayoutSaltPayTxHistoryBinding) = with(txWidget) {
        Timber.d("handleTxEmpty")
        groupShimmer.hide()
        groupError.hide()
        groupSuccess.hide()
        // root.beginDelayedTransition()
        updateTxHistoryWidget(txWidget, emptyList())
        groupEmpty.show()
    }

    private fun handleTxError(txWidget: LayoutSaltPayTxHistoryBinding) = with(txWidget) {
        Timber.d("handleTxError")
        groupShimmer.hide()
        groupEmpty.hide()
        groupSuccess.hide()
        // root.beginDelayedTransition()
        updateTxHistoryWidget(txWidget, emptyList())
        groupError.show()
    }

    private fun updateTxHistoryWidget(txWidget: LayoutSaltPayTxHistoryBinding, dataList: List<HistoryItemData>) =
        with(txWidget) {
            (rvTxHistory.adapter as TxHistoryAdapter).submitList(dataList)
        }

    override fun onViewDestroy() {
        Timber.d("onViewDestroy")
        txWidget?.apply {
            rvTxHistoryShimmer.removeItemDecoration(itemDecorator)
            rvTxHistory.removeItemDecoration(itemDecorator)
        }
        super.onViewDestroy()
    }
}
