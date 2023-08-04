package com.tangem.tap.features.wallet.redux.middlewares

import androidx.core.os.bundleOf
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.AmountType
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.feature.swap.presentation.SwapFragment
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.tokens.getIconUrl
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.buyErc20TestnetTokens
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.tangem.feature.swap.domain.models.domain.Currency as SwapCurrency

class TradeCryptoMiddleware {
    fun handle(state: () -> AppState?, action: WalletAction.TradeCryptoAction) {
        if (DemoHelper.tryHandle(state, action)) return

        when (action) {
            is WalletAction.TradeCryptoAction.Buy -> proceedBuyAction(state, action)
            is WalletAction.TradeCryptoAction.Sell -> proceedSellAction()
            is WalletAction.TradeCryptoAction.SendCrypto -> preconfigureAndOpenSendScreen(action)
            is WalletAction.TradeCryptoAction.FinishSelling -> openReceiptUrl(action.transactionId)
            is WalletAction.TradeCryptoAction.Swap -> openSwap()
        }
    }

    private fun proceedBuyAction(state: () -> AppState?, action: WalletAction.TradeCryptoAction.Buy) {
        val selectedWalletData = store.state.walletState.selectedWalletData ?: return
        val currency = chooseAppropriateCurrency(store.state.walletState) ?: return

        Analytics.send(Token.ButtonBuy(AnalyticsParam.CurrencyType.Currency(currency)))
        if (action.checkUserLocation && state()?.globalState?.userCountryCode == RUSSIA_COUNTRY_CODE) {
            store.dispatchOnMain(WalletAction.DialogAction.RussianCardholdersWarningDialog())
            return
        }

        val card = store.state.globalState.scanResponse?.card ?: return
        val addresses = selectedWalletData.walletAddresses?.list.orEmpty()
        if (addresses.isEmpty()) return

        val exchangeManager = store.state.globalState.exchangeManager
        val appCurrency = store.state.globalState.appCurrency

        if (currency is Currency.Token && currency.blockchain.isTestnet()) {
            val walletManager = store.state.walletState.getWalletManager(currency)
            if (walletManager !is EthereumWalletManager) {
                store.dispatchDebugErrorNotification("Testnet tokens available only for the Ethereum")
                return
            }

            scope.launch {
                buyErc20TestnetTokens(
                    card = card,
                    walletManager = walletManager,
                    token = currency.token,
                )
            }
            return
        }

        exchangeManager.getUrl(
            action = CurrencyExchangeManager.Action.Buy,
            blockchain = currency.blockchain,
            cryptoCurrencyName = currency.currencySymbol,
            fiatCurrencyName = appCurrency.code,
            walletAddress = addresses[0].address,
        )?.let {
            store.dispatchOpenUrl(it)
            Analytics.send(Token.Topup.ScreenOpened())
        }
    }

    private fun proceedSellAction() {
        val selectedWalletData = store.state.walletState.selectedWalletData ?: return
        val currency = chooseAppropriateCurrency(store.state.walletState) ?: return

        val appCurrency = store.state.globalState.appCurrency
        val addresses = selectedWalletData.walletAddresses?.list.orEmpty()
        if (addresses.isEmpty()) return

        Analytics.send(Token.ButtonSell(AnalyticsParam.CurrencyType.Currency(currency)))

        store.state.globalState.exchangeManager.getUrl(
            action = CurrencyExchangeManager.Action.Sell,
            blockchain = currency.blockchain,
            cryptoCurrencyName = currency.currencySymbol,
            fiatCurrencyName = appCurrency.code,
            walletAddress = addresses[0].address,
        )?.let {
            store.dispatchOpenUrl(it)
            Analytics.send(Token.Withdraw.ScreenOpened())
        }
    }

    private fun chooseAppropriateCurrency(walletState: WalletState): Currency? {
        return if (walletState.primaryTokenData == null) {
            walletState.selectedWalletData?.currency
        } else {
            walletState.primaryTokenData?.currency as? Currency.Token
        }.guard {
            store.dispatchDebugErrorNotification("Can't select an appropriate currency for a Trade action")
            return null
        }
    }

    private fun preconfigureAndOpenSendScreen(action: WalletAction.TradeCryptoAction.SendCrypto) {
        val selectedWalletData = store.state.walletState.selectedWalletData ?: return

        Analytics.send(Token.ButtonSend(AnalyticsParam.CurrencyType.Currency(selectedWalletData.currency)))
        val walletManager = store.state.walletState.getWalletManager(selectedWalletData.currency).guard {
            FirebaseCrashlytics.getInstance().recordException(IllegalStateException("WalletManager is null"))
            return
        }

        store.dispatchOnMain(
            PrepareSendScreen(
                walletManager = walletManager,
                coinAmount = walletManager.wallet.amounts[AmountType.Coin],
                coinRate = selectedWalletData.fiatRate,
            ),
        )
        store.dispatchOnMain(
            SendAction.SendSpecificTransaction(
                sendAmount = action.amount,
                destinationAddress = action.destinationAddress,
                transactionId = action.transactionId,
            ),
        )
        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Send))
    }

    private fun openReceiptUrl(transactionId: String) {
        store.dispatchOnMain(NavigationAction.PopBackTo())
        store.state.globalState.exchangeManager.getSellCryptoReceiptUrl(
            action = CurrencyExchangeManager.Action.Sell,
            transactionId = transactionId,
        )?.let { store.dispatchOpenUrl(it) }
    }

    private fun openSwap() {
        val currency = store.state.walletState.selectedWalletData?.currency?.toSwapCurrency()
        val bundle =
            bundleOf(
                SwapFragment.CURRENCY_BUNDLE_KEY to Json.encodeToString(currency),
                SwapFragment.DERIVATION_PATH to store.state.walletState.selectedWalletData?.currency?.derivationPath,
            )
        store.dispatchOnMain(NavigationAction.NavigateTo(screen = AppScreen.Swap, bundle = bundle))
    }

    private fun Currency.toSwapCurrency(): SwapCurrency {
        return when (this) {
            is Currency.Blockchain -> {
                SwapCurrency.NativeToken(
                    id = blockchain.toCoinId(),
                    name = this.currencyName,
                    symbol = this.currencySymbol,
                    networkId = this.blockchain.toNetworkId(),
                    // no need to set logoUrl for blockchain cause
                    // error when form url with coinId, coinId of eth and arbitrum the same
                    logoUrl = "",
                )
            }
            is Currency.Token -> SwapCurrency.NonNativeToken(
                id = this.token.id ?: "",
                name = this.currencyName,
                symbol = this.currencySymbol,
                networkId = this.blockchain.toNetworkId(),
                logoUrl = getIconUrl(this.token.id ?: ""),
                contractAddress = this.token.contractAddress,
                decimalCount = decimals,
            )
        }
    }
}