package com.tangem.tap.features.wallet.redux.middlewares

import androidx.core.os.bundleOf
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.feature.swap.presentation.SwapFragment
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.buyErc20TestnetTokens
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

@Suppress("LargeClass")
@Deprecated("Will be removed soon")
object TradeCryptoMiddleware {

    val middleware: Middleware<AppState> = { _, appState ->
        { nextDispatch ->
            { action ->
                if (action is TradeCryptoAction) {
                    handle(appState, action)
                }
                nextDispatch(action)
            }
        }
    }

    private val isSendRedesignedEnabled: Boolean
        get() = store.state.daggerGraphState.get(
            getDependency = DaggerGraphState::sendFeatureToggles,
        ).isRedesignedSendEnabled

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun handle(state: () -> AppState?, action: TradeCryptoAction) {
        if (DemoHelper.tryHandle(state, action)) return

        when (action) {
            is TradeCryptoAction.FinishSelling -> openReceiptUrl(action.transactionId)
            is TradeCryptoAction.Buy -> proceedBuyAction(state, action)
            is TradeCryptoAction.Sell -> proceedSellAction(action)
            is TradeCryptoAction.Swap -> openSwap(
                currency = action.cryptoCurrency,
            )
            is TradeCryptoAction.SendToken -> {
                if (isSendRedesignedEnabled) {
                    handleNewSendToken(action = action)
                } else {
                    handleSendToken(action = action)
                }
            }
            is TradeCryptoAction.SendCoin -> {
                if (isSendRedesignedEnabled) {
                    handleNewSendCoin(action = action)
                } else {
                    handleSendCoin(action = action)
                }
            }
        }
    }

    private fun proceedBuyAction(state: () -> AppState?, action: TradeCryptoAction.Buy) {
        val networkAddress = action.cryptoCurrencyStatus.value.networkAddress
            ?.defaultAddress
            ?.let(NetworkAddress.Address::value)
            ?: return

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
                AppDialog.RussianCardholdersWarningDialog.Data(topUpUrl = it)
            }
            store.dispatchDialogShow(AppDialog.RussianCardholdersWarningDialog(data = dialogData))
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

    private fun proceedSellAction(action: TradeCryptoAction.Sell) {
        val networkAddress = action.cryptoCurrencyStatus.value.networkAddress
            ?.defaultAddress
            ?.let(NetworkAddress.Address::value)
            ?: return
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

    private fun openReceiptUrl(transactionId: String) {
        store.dispatchOnMain(NavigationAction.PopBackTo())
        store.state.globalState.exchangeManager.getSellCryptoReceiptUrl(
            action = CurrencyExchangeManager.Action.Sell,
            transactionId = transactionId,
        )?.let { store.dispatchOpenUrl(it) }
    }

    private fun openSwap(currency: CryptoCurrency) {
        val bundle = bundleOf(
            SwapFragment.CURRENCY_BUNDLE_KEY to currency,
        )

        store.dispatchOnMain(NavigationAction.NavigateTo(screen = AppScreen.Swap, bundle = bundle))
    }

    private fun handleSendToken(action: TradeCryptoAction.SendToken) {
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
                    feePaidCurrency = walletManager.wallet.blockchain.feePaidCurrency(),
                    currency = currency,
                    coinAmount = walletManager.wallet.amounts[AmountType.Coin],
                    coinRate = action.coinFiatRate,
                    tokenAmount = sendableAmount,
                    tokenRate = action.tokenFiatRate,
                    feeCurrencyRate = action.feeCurrencyStatus?.value?.fiatRate,
                    feeCurrencyDecimals = action.feeCurrencyStatus?.currency?.decimals ?: 0,
                ),
            )

            val txInfo = action.transactionInfo
            if (txInfo != null) {
                store.dispatchOnMain(
                    SendAction.SendSpecificTransaction(
                        sendAmount = txInfo.amount,
                        destinationAddress = txInfo.destinationAddress,
                        transactionId = txInfo.transactionId,
                    ),
                )
            }

            val bundle = bundleOf(
                SendRouter.CRYPTO_CURRENCY_KEY to currency,
                SendRouter.USER_WALLET_ID_KEY to action.userWallet.walletId.stringValue,
            )
            store.dispatchOnMain(NavigationAction.NavigateTo(screen = AppScreen.Send, bundle = bundle))
        }
    }

    private fun handleSendCoin(action: TradeCryptoAction.SendCoin) {
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
                            feePaidCurrency = walletManager.wallet.blockchain.feePaidCurrency(),
                            currency = currency,
                            coinAmount = amountToSend,
                            coinRate = cryptoStatus.value.fiatRate,
                            feeCurrencyRate = action.feeCurrencyStatus?.value?.fiatRate,
                            feeCurrencyDecimals = action.feeCurrencyStatus?.currency?.decimals ?: 0,
                        ),
                    )
                }
                is CryptoCurrency.Token -> error("Action.tokenStatus.currency is Token")
            }

            val txInfo = action.transactionInfo
            if (txInfo != null) {
                store.dispatchOnMain(
                    SendAction.SendSpecificTransaction(
                        sendAmount = txInfo.amount,
                        destinationAddress = txInfo.destinationAddress,
                        transactionId = txInfo.transactionId,
                    ),
                )
            }

            val bundle = bundleOf(
                SendRouter.CRYPTO_CURRENCY_KEY to currency,
                SendRouter.USER_WALLET_ID_KEY to action.userWallet.walletId.stringValue,
            )
            store.dispatchOnMain(NavigationAction.NavigateTo(screen = AppScreen.Send, bundle = bundle))
        }
    }

    private fun handleNewSendToken(action: TradeCryptoAction.SendToken) {
        handleNewSend(
            userWalletId = action.userWallet.walletId.stringValue,
            txInfo = action.transactionInfo,
            currency = action.tokenCurrency,
        )
    }

    private fun handleNewSendCoin(action: TradeCryptoAction.SendCoin) {
        handleNewSend(
            userWalletId = action.userWallet.walletId.stringValue,
            txInfo = action.transactionInfo,
            currency = action.coinStatus.currency,
        )
    }

    private fun handleNewSend(
        userWalletId: String,
        txInfo: TradeCryptoAction.TransactionInfo?,
        currency: CryptoCurrency,
    ) {
        val bundle = bundleOf(
            SendRouter.CRYPTO_CURRENCY_KEY to currency,
            SendRouter.USER_WALLET_ID_KEY to userWalletId,
            SendRouter.TRANSACTION_ID_KEY to txInfo?.transactionId,
            SendRouter.DESTINATION_ADDRESS_KEY to txInfo?.destinationAddress,
            SendRouter.AMOUNT_KEY to txInfo?.amount,
        )
        store.dispatchOnMain(NavigationAction.NavigateTo(screen = AppScreen.Send, bundle = bundle))
    }
}
