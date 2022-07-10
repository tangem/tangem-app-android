package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.AmountType
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
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
import com.tangem.tap.network.exchangeServices.buyErc20TestnetTokens
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch


class TradeCryptoMiddleware {
    fun handle(state: () -> AppState?, action: WalletAction.TradeCryptoAction) {
        if (DemoHelper.tryHandle(state, action)) return

        when (action) {
            is WalletAction.TradeCryptoAction.Buy -> proceedBuyAction(state, action)
            is WalletAction.TradeCryptoAction.Sell -> proceedSellAction()
            is WalletAction.TradeCryptoAction.SendCrypto -> preconfigureAndOpenSendScreen(action)
            is WalletAction.TradeCryptoAction.FinishSelling -> openReceiptUrl(action.transactionId)
        }
    }

    private fun proceedBuyAction(
        state: () -> AppState?,
        action: WalletAction.TradeCryptoAction.Buy,
    ) {
        if (action.checkUserLocation && state()?.globalState?.userCountry == "RU") {
            store.dispatchOnMain(
                WalletAction.DialogAction.RussianCardholdersWarningDialog
            )
            return
        }

        val selectedWalletData = store.state.walletState.getSelectedWalletData() ?: return
        val exchangeManager = store.state.globalState.exchangeManager ?: return
        val appCurrency = store.state.globalState.appCurrency

        val addresses = selectedWalletData.walletAddresses?.list.orEmpty()
        if (addresses.isEmpty()) return

        val currency = selectedWalletData.currency

        if (currency is Currency.Token && currency.blockchain.isTestnet()) {
            val walletManager = store.state.walletState.getWalletManager(currency)
            if (walletManager !is EthereumWalletManager) {
                store.dispatchDebugErrorNotification("Testnet tokens available only for the Ethereum")
                return
            }

            scope.launch { exchangeManager.buyErc20TestnetTokens(walletManager, currency.token) }
            return
        }

        exchangeManager.getUrl(
            action = CurrencyExchangeManager.Action.Buy,
            blockchain = currency.blockchain,
            cryptoCurrencyName = currency.currencySymbol,
            fiatCurrencyName = appCurrency.code,
            walletAddress = addresses[0].address
        )?.let { store.dispatchOnMain(NavigationAction.OpenUrl(it)) }
    }

    private fun proceedSellAction() {
        val selectedWalletData = store.state.walletState.getSelectedWalletData() ?: return
        val exchangeManager = store.state.globalState.exchangeManager ?: return
        val appCurrency = store.state.globalState.appCurrency

        val addresses = selectedWalletData.walletAddresses?.list.orEmpty()
        if (addresses.isEmpty()) return

        val currency = selectedWalletData.currency

        exchangeManager.getUrl(
            action = CurrencyExchangeManager.Action.Sell,
            blockchain = currency.blockchain,
            cryptoCurrencyName = currency.currencySymbol,
            fiatCurrencyName = appCurrency.code,
            walletAddress = addresses[0].address
        )?.let { store.dispatchOnMain(NavigationAction.OpenUrl(it)) }
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
        val exchangeManager = store.state.globalState.exchangeManager ?: return

        store.dispatchOnMain(NavigationAction.PopBackTo())
        exchangeManager.getSellCryptoReceiptUrl(CurrencyExchangeManager.Action.Sell, transactionId)?.let {
            store.dispatchOnMain(NavigationAction.OpenUrl(it))
        }
    }
}
