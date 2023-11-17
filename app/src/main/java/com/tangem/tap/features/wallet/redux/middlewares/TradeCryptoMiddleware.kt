package com.tangem.tap.features.wallet.redux.middlewares

import androidx.core.os.bundleOf
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.swap.presentation.SwapFragment
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.buyErc20TestnetTokens
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Suppress("LargeClass")
class TradeCryptoMiddleware {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun handle(state: () -> AppState?, action: TradeCryptoAction) {
        if (DemoHelper.tryHandle(state, action)) return

        when (action) {
            is TradeCryptoAction.Buy -> proceedBuyAction(state, action)
            is TradeCryptoAction.Sell -> proceedSellAction()
            is TradeCryptoAction.SendCrypto -> preconfigureAndOpenSendScreen(action)
            is TradeCryptoAction.FinishSelling -> openReceiptUrl(action.transactionId)
            is TradeCryptoAction.Swap -> {
// [REDACTED_TODO_COMMENT]
            }
            is TradeCryptoAction.New.Buy -> proceedNewBuyAction(state, action)
            is TradeCryptoAction.New.Sell -> proceedNewSellAction(action)
            is TradeCryptoAction.New.Swap -> openSwap(
                currency = action.cryptoCurrency,
            )
            is TradeCryptoAction.New.SendToken -> handleNewSendToken(action = action)
            is TradeCryptoAction.New.SendCoin -> handleNewSendCoin(action = action)
        }
    }

    @Deprecated("Use proceedNewBuyAction instead")
    private fun proceedBuyAction(state: () -> AppState?, action: TradeCryptoAction.Buy) {
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
                    destinationAddress = currency.token.contractAddress,
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
            isDarkTheme = MutableAppThemeModeHolder.isDarkThemeActive,
        )?.let {
            store.dispatchOpenUrl(it)
            Analytics.send(Token.Topup.ScreenOpened())
        }
    }

    private fun proceedNewBuyAction(state: () -> AppState?, action: TradeCryptoAction.New.Buy) {
        val networkAddress = action.cryptoCurrencyStatus.value.networkAddress?.defaultAddress ?: return

        val status = action.cryptoCurrencyStatus
        val currency = status.currency
        val blockchain = Blockchain.fromId(currency.network.id.value)
        val exchangeManager = store.state.globalState.exchangeManager
        val topUrl = exchangeManager.getUrl(
            action = CurrencyExchangeManager.Action.Buy,
            blockchain = blockchain,
            cryptoCurrencyName = currency.symbol,
            fiatCurrencyName = action.appCurrencyCode,
            walletAddress = networkAddress,
            isDarkTheme = MutableAppThemeModeHolder.isDarkThemeActive,
        )

        if (action.checkUserLocation && state()?.globalState?.userCountryCode == RUSSIA_COUNTRY_CODE) {
            val dialogData = topUrl?.let {
                WalletDialog.RussianCardholdersWarningDialog.Data(
                    topUpUrl = it,
                )
            }
            store.dispatchOnMain(
                WalletAction.DialogAction.RussianCardholdersWarningDialog(
                    dialogData = dialogData,
                ),
            )
            return
        }

        if (currency is CryptoCurrency.Token && currency.network.isTestnet) {
            scope.launch {
                val walletManager = store.state.daggerGraphState
                    .get(DaggerGraphState::walletManagersFacade)
                    .getOrCreateWalletManager(
                        userWalletId = action.userWallet.walletId,
                        blockchain = blockchain,
                        derivationPath = currency.network.derivationPath.value,
                    )

                if (walletManager !is EthereumWalletManager) {
                    store.dispatchDebugErrorNotification("Testnet tokens available only for the Ethereum")
                    return@launch
                }

                buyErc20TestnetTokens(
                    card = action.userWallet.scanResponse.card,
                    walletManager = walletManager,
                    destinationAddress = currency.contractAddress,
                )
            }
            return
        }

        topUrl?.let {
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
            isDarkTheme = MutableAppThemeModeHolder.isDarkThemeActive,
        )?.let {
            store.dispatchOpenUrl(it)
            Analytics.send(Token.Withdraw.ScreenOpened())
        }
    }

    private fun proceedNewSellAction(action: TradeCryptoAction.New.Sell) {
        val networkAddress = action.cryptoCurrencyStatus.value.networkAddress?.defaultAddress ?: return
        val currency = action.cryptoCurrencyStatus.currency

        store.state.globalState.exchangeManager.getUrl(
            action = CurrencyExchangeManager.Action.Sell,
            blockchain = Blockchain.fromId(currency.network.id.value),
            cryptoCurrencyName = currency.symbol,
            fiatCurrencyName = action.appCurrencyCode,
            walletAddress = networkAddress,
            isDarkTheme = MutableAppThemeModeHolder.isDarkThemeActive,
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

    private fun preconfigureAndOpenSendScreen(action: TradeCryptoAction.SendCrypto) {
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

    private fun openSwap(currency: CryptoCurrency) {
        val bundle = bundleOf(
            SwapFragment.CURRENCY_BUNDLE_KEY to currency
        )

        store.dispatchOnMain(NavigationAction.NavigateTo(screen = AppScreen.Swap, bundle = bundle))
    }

    private fun handleNewSendToken(action: TradeCryptoAction.New.SendToken) {
        val currency = action.tokenCurrency
        val blockchain = Blockchain.fromId(currency.network.id.value)

        scope.launch {
            val walletManager = store.state.daggerGraphState
                .get(DaggerGraphState::walletManagersFacade)
                .getOrCreateWalletManager(
                    userWalletId = action.userWallet.walletId,
                    blockchain = blockchain,
                    derivationPath = currency.network.derivationPath.value,
                )

            if (walletManager == null) {
                val error = TapError.UnsupportedState(stateError = "WalletManager is null")
                FirebaseCrashlytics.getInstance().recordException(IllegalStateException(error.stateError))
                store.dispatchErrorNotification(error)
                return@launch
            }

            val sendableAmount = walletManager.wallet.amounts.values.firstOrNull {
                val amountType = it.type
                amountType is AmountType.Token && amountType.token.contractAddress == currency.contractAddress
            }

            store.dispatchOnMain(
                action = PrepareSendScreen(
                    walletManager = walletManager,
                    coinAmount = walletManager.wallet.amounts[AmountType.Coin],
                    coinRate = action.coinFiatRate,
                    tokenAmount = sendableAmount,
                    tokenRate = action.tokenFiatRate,
                ),
            )

            val bundle = bundleOf(
                SendRouter.CRYPTO_CURRENCY_KEY to currency,
                SendRouter.USER_WALLET_ID_KEY to action.userWallet.walletId.stringValue,
            )
            store.dispatchOnMain(NavigationAction.NavigateTo(screen = AppScreen.Send, bundle = bundle))
        }
    }

    private fun handleNewSendCoin(action: TradeCryptoAction.New.SendCoin) {
        val cryptoStatus = action.coinStatus
        val currency = cryptoStatus.currency
        val blockchain = Blockchain.fromId(currency.network.id.value)

        scope.launch {
            val walletManager = store.state.daggerGraphState
                .get(DaggerGraphState::walletManagersFacade)
                .getOrCreateWalletManager(
                    userWalletId = action.userWallet.walletId,
                    blockchain = blockchain,
                    derivationPath = currency.network.derivationPath.value,
                )

            if (walletManager == null) {
                val error = TapError.UnsupportedState(stateError = "WalletManager is null")
                FirebaseCrashlytics.getInstance().recordException(IllegalStateException(error.stateError))
                store.dispatchErrorNotification(error)
                return@launch
            }

            val sendableAmounts = walletManager.wallet.amounts.values.filter { it.type == AmountType.Coin }
            when (currency) {
                is CryptoCurrency.Coin -> {
                    val amountToSend = sendableAmounts.find { it.currencySymbol == currency.symbol }

                    if (amountToSend == null) {
                        val error = TapError.UnsupportedState(stateError = "Amount to send is null")
                        FirebaseCrashlytics.getInstance()
                            .recordException(IllegalStateException(error.stateError))
                        store.dispatchErrorNotification(error)
                        return@launch
                    }

                    store.dispatchOnMain(
                        action = PrepareSendScreen(
                            walletManager = walletManager,
                            coinAmount = amountToSend,
                            coinRate = cryptoStatus.value.fiatRate,
                        ),
                    )
                }
                is CryptoCurrency.Token -> error("Action.tokenStatus.currency is Token")
            }

            val bundle = bundleOf(
                SendRouter.CRYPTO_CURRENCY_KEY to currency,
                SendRouter.USER_WALLET_ID_KEY to action.userWallet.walletId.stringValue,
            )
            store.dispatchOnMain(NavigationAction.NavigateTo(screen = AppScreen.Send, bundle = bundle))
        }
    }
}
