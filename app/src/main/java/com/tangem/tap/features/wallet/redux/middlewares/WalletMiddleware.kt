package com.tangem.tap.features.wallet.redux.middlewares

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchain.blockchains.solana.RentProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.isZero
import com.tangem.common.services.Result
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.extensions.dispatchToastNotification
import com.tangem.tap.common.extensions.isGreaterThan
import com.tangem.tap.common.extensions.onCardScanned
import com.tangem.tap.common.extensions.shareText
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.failedRates
import com.tangem.tap.domain.loadedRates
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.tap.features.wallet.models.filterByCoin
import com.tangem.tap.features.wallet.models.getPendingTransactions
import com.tangem.tap.features.wallet.models.getSendableAmounts
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.WalletStore
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.network.NetworkStateChanged
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.totalFiatBalanceCalculator
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.userWalletsListManagerSafe
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware
import timber.log.Timber
import java.math.BigDecimal

class WalletMiddleware {
    private val tradeCryptoMiddleware = TradeCryptoMiddleware()
    private val warningsMiddleware = WarningsMiddleware()
    private val multiWalletMiddleware = MultiWalletMiddleware()
    private val walletDialogMiddleware = WalletDialogsMiddleware()
    private val appCurrencyMiddleware by lazy(mode = LazyThreadSafetyMode.NONE) {
        AppCurrencyMiddleware(
            tangemTechService = store.state.domainNetworks.tangemTechService,
            tapWalletManager = store.state.globalState.tapWalletManager,
            fiatCurrenciesPrefStorage = preferencesStorage.fiatCurrenciesPrefStorage,
            appCurrencyProvider = { store.state.globalState.appCurrency },
        )
    }

    val walletMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                handleAction(state, action, dispatch)
                next(action)
            }
        }
    }

    private fun handleAction(state: () -> AppState?, action: Action, dispatch: DispatchFunction) {
        if (DemoHelper.tryHandle(state, action)) return

        val globalState = store.state.globalState
        val walletState = store.state.walletState

        when (action) {
            is WalletAction.TradeCryptoAction -> tradeCryptoMiddleware.handle(state, action)
            is WalletAction.Warnings -> warningsMiddleware.handle(action, globalState)
            is WalletAction.MultiWallet -> multiWalletMiddleware.handle(
                action,
                walletState,
                globalState
            )
            is WalletAction.AppCurrencyAction -> appCurrencyMiddleware.handle(action)
            is WalletAction.DialogAction -> walletDialogMiddleware.handle(action)
            is WalletAction.LoadWallet -> {
                scope.launch {
                    if (action.blockchain == null) {
                        walletState.walletManagers.map { walletManager ->
                            async { globalState.tapWalletManager.loadWalletData(walletManager) }
                        }.awaitAll()
                    } else {
                        val walletManager = walletState.getWalletManager(action.blockchain)
                            ?: action.walletManager
                        walletManager?.let { globalState.tapWalletManager.loadWalletData(it) }
                    }
                }
            }
            is WalletAction.LoadWallet.Success -> {
                checkForRentWarning(walletState.getWalletManager(action.blockchain))
                val coinAmount = action.wallet.amounts[AmountType.Coin]?.value
                if (coinAmount != null && !coinAmount.isZero()) {
                    if (walletState.getWalletData(action.blockchain) == null) {
                        store.dispatch(
                            WalletAction.MultiWallet.AddBlockchain(
                                action.blockchain,
                                null,
                                true,
                            ),
                        )
                        store.dispatch(
                            action = WalletAction.LoadWallet.Success(action.wallet, action.blockchain)
                        )
                    }
                }
                store.dispatch(WalletAction.Warnings.CheckHashesCount.CheckHashesCountOnline)
                warningsMiddleware.tryToShowAppRatingWarning(action.wallet)
            }
            is WalletAction.LoadFiatRate -> {
                val appCurrencyId = globalState.appCurrency.code
                scope.launch {
                    val coinsList = when {
                        action.wallet != null -> {
                            val wallet = action.wallet
                            wallet.getTokens()
                                .map { Currency.Token(it, wallet.blockchain, wallet.publicKey.derivationPath?.rawPath) }
                                .plus(Currency.Blockchain(wallet.blockchain, wallet.publicKey.derivationPath?.rawPath))
                        }
                        action.coinsList != null -> action.coinsList
                        else -> {
                            if (walletState.isMultiwalletAllowed) {
                                walletState.walletsData.map { it.currency }
                            } else {
                                val derivationPath = walletState.primaryWallet?.currency?.derivationPath
                                val primaryBlockchain = walletState.primaryBlockchain
                                val primaryToken = walletState.primaryToken
                                listOfNotNull(
                                    primaryBlockchain?.let { Currency.Blockchain(it, derivationPath) },
                                    primaryToken?.let { Currency.Token(it, primaryBlockchain!!, derivationPath) },
                                )
                            }
                        }
                    }
                    val ratesResult = globalState.tapWalletManager.rates.loadFiatRate(
                        currencyId = appCurrencyId,
                        coinsList = coinsList,
                    )
                    when (ratesResult) {
                        is Result.Success -> {
                            ratesResult.data.loadedRates.let {
                                dispatchOnMain(WalletAction.LoadFiatRate.Success(it))
                            }
                            ratesResult.data.failedRates.forEach { (currency, throwable) ->
                                Timber.e(
                                    throwable,
                                    "Loading rates failed for [%s]",
                                    currency.currencySymbol
                                )
                            }
                        }
                        is Result.Failure -> {
                            store.dispatchDebugErrorNotification("LoadFiatRate.Failure")
                            dispatchOnMain(WalletAction.LoadFiatRate.Failure)
                        }
                    }
                }
            }
            is WalletAction.CreateWallet -> {
                scope.launch {
                    val result = tangemSdkManager.createWallet(
                        globalState.scanResponse?.card?.cardId
                    )
                    when (result) {
                        is CompletionResult.Success -> {
                            val scanNoteResponse =
                                globalState.scanResponse?.copy(card = result.data)
                            scanNoteResponse?.let { store.onCardScanned(scanNoteResponse) }
                        }
                        is CompletionResult.Failure -> {}
                    }
                }
            }
            is WalletAction.Scan -> {
                store.dispatch(HomeAction.ShouldScanCardOnResume(true))
                store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
            }
            is WalletAction.LoadCardInfo -> {
                val attestationFailed = action.card.attestation.status == Attestation.Status.Failed
                store.dispatchOnMain(GlobalAction.SetIfCardVerifiedOnline(!attestationFailed))

                scope.launch {
                    val response = OnlineCardVerifier().getCardInfo(
                        action.card.cardId, action.card.cardPublicKey
                    )
                    when (response) {
                        is Result.Success -> {
                            val actionList = listOf(
                                WalletAction.SetArtworkId(response.data.artwork?.id),
                                WalletAction.LoadArtwork(action.card, response.data.artwork?.id),
                            )
                            withMainContext { actionList.forEach { store.dispatch(it) } }
                        }
                        is Result.Failure -> {}
                    }
                    store.dispatchOnMain(WalletAction.Warnings.CheckIfNeeded)
                }
            }
            is WalletAction.LoadData,
            is WalletAction.LoadData.Refresh,
            -> {
                val selectedWallet = userWalletsListManager.selectedUserWalletSync
                scope.launch {
                    if (selectedWallet != null) {
                        globalState.tapWalletManager.loadData(
                            userWallet = selectedWallet,
                            refresh = action is WalletAction.LoadData.Refresh,
                        )
                    } else {
                        val scanNoteResponse = globalState.scanResponse ?: return@launch
                        if (walletState.walletsData.isNotEmpty()) {
                            globalState.tapWalletManager.reloadData(scanNoteResponse)
                        } else {
                            globalState.tapWalletManager.loadData(scanNoteResponse)
                        }
                    }
                }
            }
            is NetworkStateChanged -> {
                store.dispatch(WalletAction.Warnings.CheckHashesCount.CheckHashesCountOnline)
                val selectedUserWallet = userWalletsListManagerSafe?.selectedUserWalletSync
                if (selectedUserWallet != null) scope.launch {
                    globalState.tapWalletManager.loadData(selectedUserWallet)
                } else {
                    globalState.scanResponse?.let { scanNoteResponse ->
                        scope.launch { globalState.tapWalletManager.loadData(scanNoteResponse) }
                    }
                }
            }
            is WalletAction.CopyAddress -> {
                action.context.copyToClipboard(action.address)
                store.dispatch(WalletAction.CopyAddress.Success)
            }
            is WalletAction.ShareAddress -> {
                action.context.shareText(action.address)
            }
            is WalletAction.ExploreAddress -> {
                Analytics.send(Token.ButtonExplore())
                store.dispatchOpenUrl(action.exploreUrl)
            }
            is WalletAction.Send -> {
                if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
                    store.dispatchErrorNotification(TapError.NoInternetConnection)
                    return
                }
                val newAction = prepareSendAction(action.amount, store.state.walletState)
                if (newAction is PrepareSendScreen && newAction.walletManager == null) {
                    store.dispatch(NavigationAction.PopBackTo(screen = AppScreen.Home))
                    FirebaseCrashlytics.getInstance().recordException(
                        IllegalStateException("PrepareSendScreen: walletManager is null"),
                    )
                    store.dispatchToastNotification(R.string.internal_error_wallet_manager_not_found)
                } else {
                    store.dispatch(newAction)
                    if (newAction is PrepareSendScreen) {
                        store.dispatch(NavigationAction.NavigateTo(AppScreen.Send))
                    }
                }
            }
            is WalletAction.ShowSaveWalletIfNeeded -> {
                showSaveWalletIfNeeded()
            }
            is WalletAction.ChangeWallet -> {
                changeWallet()
            }
            is WalletAction.UserWalletChanged -> Unit
            is WalletAction.WalletStoresChanged -> {
                fetchTotalFiatBalance(action.walletStores, walletState)
                findMissedDerivations(action.walletStores)
                tryToShowAppRatingWarning(action.walletStores)
            }
            is WalletAction.TotalFiatBalanceChanged -> Unit
        }
    }

    private fun fetchTotalFiatBalance(walletStores: List<WalletStoreModel>, state: WalletState) {
        scope.launch(Dispatchers.Default) {
            val totalFiatBalance = totalFiatBalanceCalculator.calculateOrNull(
                prevAmount = state.totalBalance?.fiatAmount,
                walletStores = walletStores,
            )

            if (totalFiatBalance != null) {
                store.dispatchOnMain(WalletAction.TotalFiatBalanceChanged(totalFiatBalance))
            }
        }
    }

    private fun findMissedDerivations(wallStores: List<WalletStoreModel>) {
        scope.launch(Dispatchers.Default) {
            val missedDerivations = wallStores
                .filter { store ->
                    store.walletsData.any { it.status is WalletDataModel.MissedDerivation }
                }
                .map { it.blockchainNetwork }

            store.dispatchOnMain(WalletAction.MultiWallet.AddMissingDerivations(missedDerivations))
        }
    }

    private fun tryToShowAppRatingWarning(walletStores: List<WalletStoreModel>) {
        scope.launch(Dispatchers.Default) {
            warningsMiddleware.tryToShowAppRatingWarning(
                hasNonZeroWallets = walletStores
                    .flatMap { it.walletsData }
                    .any { it.status.amount.isGreaterThan(BigDecimal.ZERO) },
            )
        }
    }

    private fun showSaveWalletIfNeeded() {
        if (preferencesStorage.shouldShowSaveUserWalletScreen
            && tangemSdkManager.canUseBiometry
            && store.state.navigationState.backStack.lastOrNull() == AppScreen.Wallet
        ) {
            store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.SaveWallet))
        }
    }

    private fun changeWallet() {
        when {
            userWalletsListManager.hasSavedUserWallets -> {
                Analytics.send(MainScreen.ButtonMyWallets())
                store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.WalletSelector))
            }
            else -> {
                Analytics.send(MainScreen.ButtonScanCard())
                store.dispatch(WalletAction.Scan)
            }
        }
    }

    private fun prepareSendAction(amount: Amount?, state: WalletState?): Action {
        val selectedWalletData = state?.getSelectedWalletData()
        val currency = selectedWalletData?.currency
        val walletStore = state?.getWalletStore(currency)

        return if (amount != null) {
            if (amount.type is AmountType.Token) {
                prepareSendActionForToken(amount, state, selectedWalletData, walletStore)
            } else {
                PrepareSendScreen(amount, selectedWalletData?.fiatRate, walletStore?.walletManager)
            }
        } else {
            val amounts = walletStore?.walletManager?.wallet?.getSendableAmounts()
            if (currency != null && state.isMultiwalletAllowed) {
                when (currency) {
                    is Currency.Blockchain -> {
                        val amountToSend =
                            amounts?.find { it.currencySymbol == currency.blockchain.currency }
                                ?: return WalletAction.DialogAction.ChooseCurrency(amounts)
                        PrepareSendScreen(
                            coinAmount = amountToSend,
                            coinRate = selectedWalletData.fiatRate,
                            walletManager = walletStore.walletManager
                        )
                    }
                    is Currency.Token -> {
                        val amountToSend =
                            amounts?.find { it.currencySymbol == currency.token.symbol }
                                ?: return WalletAction.DialogAction.ChooseCurrency(amounts)
                        prepareSendActionForToken(
                            amount = amountToSend,
                            state = state,
                            selectedWalletData = selectedWalletData,
                            walletStore = walletStore
                        )
                    }
                }
            } else {
                if (amounts?.size ?: 0 > 1) {
                    WalletAction.DialogAction.ChooseCurrency(amounts)
                } else {
                    val amountToSend = amounts?.first()
                    PrepareSendScreen(
                        coinAmount = amountToSend,
                        coinRate = selectedWalletData?.fiatRate,
                        walletManager = walletStore?.walletManager
                    )
                }
            }
        }
    }

    private fun prepareSendActionForToken(
        amount: Amount,
        state: WalletState?,
        selectedWalletData: WalletData?,
        walletStore: WalletStore?
    ): PrepareSendScreen {
        val coinRate = state?.getWalletData(walletStore?.blockchainNetwork)?.fiatRate
        val tokenRate = if (state?.isMultiwalletAllowed == true) {
            selectedWalletData?.fiatRate
        } else {
            selectedWalletData?.currencyData?.token?.fiatRate
        }
        val coinAmount = walletStore?.walletManager?.wallet?.amounts?.get(AmountType.Coin)

        return PrepareSendScreen(
            coinAmount = coinAmount,
            coinRate = coinRate,
            walletManager = walletStore?.walletManager,
            tokenAmount = amount,
            tokenRate = tokenRate
        )
    }

    private fun checkForRentWarning(walletManager: WalletManager?) {
        val rentProvider = walletManager as? RentProvider ?: return

        scope.launch {
            when (val result = rentProvider.minimalBalanceForRentExemption()) {
                is com.tangem.blockchain.extensions.Result.Success -> {
                    fun isNeedToShowWarning(balance: BigDecimal, rentExempt: BigDecimal): Boolean {
                        return balance < rentExempt
                    }

                    val balance = walletManager.wallet.fundsAvailable(AmountType.Coin)
                    val outgoingTxs = walletManager.wallet.getPendingTransactions(
                        PendingTransactionType.Outgoing
                    ).filterByCoin()

                    val rentExempt = result.data
                    val show = if (outgoingTxs.isEmpty()) {
                        isNeedToShowWarning(balance, rentExempt)
                    } else {
                        val outgoingAmount = outgoingTxs.sumOf { it.amountValue ?: BigDecimal.ZERO }
                        val rest = balance.minus(outgoingAmount)
                        isNeedToShowWarning(rest, rentExempt)
                    }

                    val currency = walletManager.wallet.blockchain.currency
                    if (show) {
                        dispatchOnMain(
                            WalletAction.SetWalletRent(
                                wallet = walletManager.wallet,
                                minRent = ("${rentProvider.rentAmount().stripZeroPlainString()} $currency"),
                                rentExempt = ("${rentExempt.stripZeroPlainString()} $currency")
                            )
                        )
                    } else {
                        dispatchOnMain(WalletAction.RemoveWalletRent(walletManager.wallet))
                    }
                }
                is com.tangem.blockchain.extensions.Result.Failure -> {}
            }
        }
    }
}
