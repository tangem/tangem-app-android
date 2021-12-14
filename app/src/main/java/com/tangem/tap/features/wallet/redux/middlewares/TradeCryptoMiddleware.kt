package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.AmountType
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.topup.TopUpManager
import com.tangem.tap.domain.topup.TradeCryptoHelper
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch
import timber.log.Timber


class TradeCryptoMiddleware {
    fun handle(action: WalletAction.TradeCryptoAction) {
        when (action) {
            is WalletAction.TradeCryptoAction.Buy -> startExchange(action)
            is WalletAction.TradeCryptoAction.Sell -> startExchange(action)
            is WalletAction.TradeCryptoAction.SendCrypto -> preconfigureAndOpenSendScreen(action)
            is WalletAction.TradeCryptoAction.FinishSelling -> openReceiptUrl(action.transactionId)
        }
    }

    private fun startExchange(action: WalletAction.TradeCryptoAction) {
        val selectedWalletData = store.state.walletState.getSelectedWalletData()
        val config = store.state.globalState.configManager?.config ?: return
        val addresses = selectedWalletData?.walletAddresses ?: return
        if (addresses.list.isEmpty()) return

        val defaultAddress = addresses.list[0].address
        val currency = selectedWalletData.currency
        val currencySymbol = selectedWalletData.currency?.currencySymbol ?: return

        val exchangeAction = if (action is WalletAction.TradeCryptoAction.Buy) {
            TradeCryptoHelper.Action.Buy
        } else {
            TradeCryptoHelper.Action.Sell
        }

        if (exchangeAction == TradeCryptoHelper.Action.Buy &&
            currency is Currency.Token && currency.blockchain.isTestnet()
        ) {
            val walletManager = store.state.walletState.getWalletManager(currency.token)
            if (walletManager !is EthereumWalletManager) return

            scope.launch {
                TopUpManager().topUpTestErc20Tokens(
                    walletManager = walletManager, token = currency.token
                )
            }
            return
        }

        val url = TradeCryptoHelper.getUrl(
            action = exchangeAction,
            blockchain = currency?.blockchain,
            cryptoCurrencyName = currencySymbol,
            walletAddress = defaultAddress,
            apiKey = config.moonPayApiKey,
            secretKey = config.moonPayApiSecretKey
        )
        Timber.d("Moonpay $exchangeAction URL: $url")
        store.dispatchOnMain(NavigationAction.OpenUrl(url))
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
        store.dispatchOnMain(NavigationAction.PopBackTo())
        val url = TradeCryptoHelper.getSellCryptoReceiptUrl(transactionId)
        store.dispatchOnMain(NavigationAction.OpenUrl(url))
    }
}