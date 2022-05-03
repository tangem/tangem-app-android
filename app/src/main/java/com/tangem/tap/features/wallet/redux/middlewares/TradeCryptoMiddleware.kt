package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.AmountType
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.buyErc20Tokens
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch


class TradeCryptoMiddleware {
    fun handle(state: () -> AppState?, action: WalletAction.TradeCryptoAction) {
        if (DemoHelper.tryHandle(state, action)) return

        when (action) {
            is WalletAction.TradeCryptoAction.Buy -> startExchange(action)
            is WalletAction.TradeCryptoAction.Sell -> startExchange(action)
            is WalletAction.TradeCryptoAction.SendCrypto -> preconfigureAndOpenSendScreen(action)
            is WalletAction.TradeCryptoAction.FinishSelling -> openReceiptUrl(action.transactionId)
        }
    }

    private fun startExchange(action: WalletAction.TradeCryptoAction) {
        val selectedWalletData = store.state.walletState.getSelectedWalletData()
        val exchangeManager = store.state.globalState.currencyExchangeManager ?: return
        val addresses = selectedWalletData?.walletAddresses ?: return
        if (addresses.list.isEmpty()) return
        val appCurrency = store.state.globalState.appCurrency

        val defaultAddress = addresses.list[0].address
        val currency = selectedWalletData.currency
        val currencySymbol = selectedWalletData.currency.currencySymbol

        val exchangeAction = if (action is WalletAction.TradeCryptoAction.Buy) {
            CurrencyExchangeManager.Action.Buy
        } else {
            CurrencyExchangeManager.Action.Sell
        }

        if (exchangeAction == CurrencyExchangeManager.Action.Buy &&
            currency is Currency.Token && currency.blockchain.isTestnet()
        ) {
            val walletManager = store.state.walletState.getWalletManager(currency)
            if (walletManager !is EthereumWalletManager) return

            scope.launch { exchangeManager.buyErc20Tokens(walletManager, currency.token) }
            return
        }

        exchangeManager.getUrl(
            action = exchangeAction,
            blockchain = currency.blockchain,
            cryptoCurrencyName = currencySymbol,
            fiatCurrency = appCurrency,
            walletAddress = defaultAddress)?.let { store.dispatchOnMain(NavigationAction.OpenUrl(it)) }

    }

    private fun preconfigureAndOpenSendScreen(action: WalletAction.TradeCryptoAction.SendCrypto) {
        val selectedWalletData = store.state.walletState.getSelectedWalletData() ?: return
        val walletManager =
            store.state.walletState.getWalletManager(selectedWalletData.currency)
        store.dispatchOnMain(PrepareSendScreen(
            coinAmount = walletManager?.wallet?.amounts?.get(AmountType.Coin),
            coinRate = selectedWalletData.fiatRate,
            walletManager = walletManager
        ))
        store.dispatchOnMain(SendAction.SendSpecificTransaction(
            sendAmount = action.amount,
            destinationAddress = action.destinationAddress,
            transactionId = action.transactionId
        ))
        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Send))
    }

    private fun openReceiptUrl(transactionId: String) {
        val exchangeManager = store.state.globalState.currencyExchangeManager ?: return

        store.dispatchOnMain(NavigationAction.PopBackTo())
        exchangeManager.getSellCryptoReceiptUrl(CurrencyExchangeManager.Action.Sell, transactionId)?.let {
            store.dispatchOnMain(NavigationAction.OpenUrl(it))
        }
    }
}