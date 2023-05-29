package com.tangem.tap.features.wallet.redux.middlewares

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.CompletionResult
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.userWalletList.GetCardImageUseCase
import com.tangem.tap.domain.userWalletList.lockIfLockable
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.getSendableAmounts
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.utils.coroutines.ifActive
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber
import java.math.BigDecimal

@Suppress("LargeClass")
class WalletMiddleware {
    private val tradeCryptoMiddleware = TradeCryptoMiddleware()
    private val warningsMiddleware = WarningsMiddleware()
    private val multiWalletMiddleware = MultiWalletMiddleware()
    private val walletDialogMiddleware = WalletDialogsMiddleware()
    private val appCurrencyMiddleware by lazy(mode = LazyThreadSafetyMode.NONE) {
        AppCurrencyMiddleware(
            // TODO("After adding DI") get dependencies by DI
            walletRepository = store.state.featureRepositoryProvider.walletRepository,
            tapWalletManager = store.state.globalState.tapWalletManager,
            fiatCurrenciesPrefStorage = preferencesStorage.fiatCurrenciesPrefStorage,
            appCurrencyProvider = { store.state.globalState.appCurrency },
        )
    }

    private val networkConnectionManager: NetworkConnectionManager
        get() = store.state.daggerGraphState.get(DaggerGraphState::networkConnectionManager)

    private var updateWalletStoresJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    val walletMiddleware: Middleware<AppState> = { _, state ->
        { next ->
            { action ->
                handleAction(state, action)
                next(action)
            }
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun handleAction(state: () -> AppState?, action: Action) {
        if (DemoHelper.tryHandle(state, action)) return

        val globalState = store.state.globalState
        val walletState = store.state.walletState

        when (action) {
            is WalletAction.TradeCryptoAction -> tradeCryptoMiddleware.handle(state, action)
            is WalletAction.Warnings -> warningsMiddleware.handle(action, globalState)
            is WalletAction.MultiWallet -> multiWalletMiddleware.handle(action, walletState)
            is WalletAction.AppCurrencyAction -> appCurrencyMiddleware.handle(action)
            is WalletAction.DialogAction -> walletDialogMiddleware.handle(action)
            is WalletAction.CreateWallet -> {
                scope.launch {
                    when (val result = tangemSdkManager.createWallet(globalState.scanResponse?.card?.cardId)) {
                        is CompletionResult.Success -> {
                            val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
                                Timber.e("Unable to create wallet, no user wallet selected")
                                return@launch
                            }
                            userWalletsListManager.update(selectedUserWallet.walletId) { userWallet ->
                                userWallet.copy(
                                    scanResponse = userWallet.scanResponse.copy(
                                        card = result.data,
                                    ),
                                )
                            }
                        }
                        is CompletionResult.Failure -> Unit
                    }
                }
            }
            is WalletAction.Scan -> {
                store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                scope.launch {
                    delay(timeMillis = 700)
                    store.dispatchOnMain(HomeAction.ReadCard(action.onScanSuccessEvent))
                }
            }
            is WalletAction.LoadData,
            is WalletAction.LoadData.Refresh,
            -> {
                val selectedWallet = userWalletsListManager.selectedUserWalletSync.guard {
                    Timber.e("Unable to load/refresh wallets data, no user wallet selected")
                    return
                }

                scope.launch {
                    globalState.tapWalletManager.loadData(
                        userWallet = selectedWallet,
                        refresh = action is WalletAction.LoadData.Refresh,
                    )
                }

                store.dispatchOnMain(WalletAction.UpdateUserWalletArtwork(selectedWallet.walletId))
            }
            is WalletAction.CopyAddress -> {
                Analytics.send(Token.Receive.ButtonCopyAddress())
                action.context.copyToClipboard(action.address)
                store.dispatch(WalletAction.CopyAddress.Success)
            }
            is WalletAction.ShareAddress -> {
                Analytics.send(Token.Receive.ButtonShareAddress())
                action.context.shareText(action.address)
            }
            is WalletAction.ExploreAddress -> {
                Analytics.send(Token.ButtonExplore())
                store.dispatchOpenUrl(action.exploreUrl)
            }
            is WalletAction.Send -> {
                if (!networkConnectionManager.isOnline) {
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
                        store.state.walletState.selectedWalletData?.currency?.let { currency ->
                            Analytics.send(Token.ButtonSend(AnalyticsParam.CurrencyType.Currency(currency)))
                        }
                        store.dispatch(NavigationAction.NavigateTo(AppScreen.Send))
                    }
                }
            }
            is WalletAction.ShowSaveWalletIfNeeded -> {
                showSaveWalletIfNeeded()
            }
            is WalletAction.ChangeWallet -> {
                changeWallet(walletState)
            }
            is WalletAction.UserWalletChanged -> Unit
            is WalletAction.WalletStoresChanged -> {
                // Cancel update job when new wallet stores received
                updateWalletStoresJob = scope.launch(Dispatchers.Default) {
                    ifActive { fetchTotalFiatBalance(action.walletStores) }
                    ifActive { findMissedDerivations(action.walletStores) }
                    ifActive { tryToShowAppRatingWarning(action.walletStores) }
                    ifActive { store.state.globalState.topUpController?.walletStoresChanged(action.walletStores) }
                }
            }
            is WalletAction.TotalFiatBalanceChanged -> Unit
            is WalletAction.PopBackToInitialScreen -> {
                userWalletsListManager.lockIfLockable()
                val screen = if (walletState.canSaveUserWallets) {
                    AppScreen.Welcome
                } else {
                    AppScreen.Home
                }

                store.dispatchOnMain(NavigationAction.PopBackTo(screen))
            }
            is WalletAction.ChangeSelectedAddress -> {
                changeSelectedWalletAddress(action.type, walletState)
            }
            is WalletAction.UpdateUserWalletArtwork -> {
                scope.launch {
                    userWalletsListManager
                        .update(
                            userWalletId = action.walletId,
                            update = { userWallet ->
                                userWallet.copy(
                                    artworkUrl = GetCardImageUseCase().invoke(
                                        cardId = userWallet.cardId,
                                        cardPublicKey = userWallet.scanResponse.card.cardPublicKey,
                                    ),
                                )
                            },
                        )
                        .doOnSuccess {
                            store.dispatch(
                                WalletAction.SetArtworkUrl(userWalletId = action.walletId, url = it.artworkUrl),
                            )
                        }
                }
            }
        }
    }

    private fun changeSelectedWalletAddress(type: AddressType, state: WalletState) {
        val selectedUserWalletId = userWalletsListManager.selectedUserWalletSync?.walletId.guard {
            Timber.e("Unable to change selected wallet address, no user wallet selected")
            return
        }
        val selectedCurrency = state.selectedCurrency.guard {
            Timber.e("Unable to change selected wallet address, no currency selected")
            return
        }

        scope.launch(Dispatchers.Default) {
            walletStoresManager.updateSelectedAddress(selectedUserWalletId, selectedCurrency, type)
        }
    }

    private suspend fun fetchTotalFiatBalance(walletStores: List<WalletStoreModel>) {
        val totalFiatBalance = totalFiatBalanceCalculator.calculateOrNull(walletStores)

        if (totalFiatBalance != null) {
            store.dispatchOnMain(WalletAction.TotalFiatBalanceChanged(totalFiatBalance))
        }
    }

    private fun findMissedDerivations(wallStores: List<WalletStoreModel>) {
        val missedDerivations = wallStores
            .filter { store ->
                store.walletsData.any { it.status is WalletDataModel.MissedDerivation }
            }
            .map(WalletStoreModel::blockchainNetwork)

        store.dispatchOnMain(WalletAction.MultiWallet.AddMissingDerivations(missedDerivations))
    }

    private fun tryToShowAppRatingWarning(walletStores: List<WalletStoreModel>) {
        warningsMiddleware.tryToShowAppRatingWarning(
            hasNonZeroWallets = walletStores
                .flatMap { it.walletsData }
                .any { it.status.amount.isGreaterThan(BigDecimal.ZERO) },
        )
    }

    private fun showSaveWalletIfNeeded() {
        if (preferencesStorage.shouldShowSaveUserWalletScreen &&
            tangemSdkManager.canUseBiometry &&
            store.state.navigationState.backStack.lastOrNull() == AppScreen.Wallet
        ) {
            store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.SaveWallet))
        }
    }

    private fun changeWallet(state: WalletState) {
        when {
            state.canSaveUserWallets -> {
                Analytics.send(MainScreen.ButtonMyWallets())
                store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.WalletSelector))
            }
            else -> {
                Analytics.send(MainScreen.ButtonScanCard())
                store.dispatch(WalletAction.Scan(Basic.CardWasScanned(AnalyticsParam.ScannedFrom.Main)))
            }
        }
    }

    private fun prepareSendAction(amount: Amount?, state: WalletState?): Action {
        val selectedWalletData = state?.selectedWalletData
        val currency = selectedWalletData?.currency
        val walletStore = state?.getWalletStore(currency)

        return if (amount != null) {
            if (amount.type is AmountType.Token) {
                prepareSendActionForToken(amount, selectedWalletData, walletStore)
            } else {
                PrepareSendScreen(amount, selectedWalletData?.fiatRate, walletStore?.walletManager)
            }
        } else {
            val amounts = walletStore?.walletManager?.wallet?.getSendableAmounts()
            if (currency != null && state.isMultiwalletAllowed) {
                when (currency) {
                    is Currency.Blockchain -> {
                        val amountToSend = amounts?.find { it.currencySymbol == currency.blockchain.currency }
                            ?: return WalletAction.DialogAction.ChooseCurrency(amounts)
                        PrepareSendScreen(
                            coinAmount = amountToSend,
                            coinRate = selectedWalletData.fiatRate,
                            walletManager = walletStore.walletManager,
                        )
                    }
                    is Currency.Token -> {
                        val amountToSend = amounts?.find { it.currencySymbol == currency.token.symbol }
                            ?: return WalletAction.DialogAction.ChooseCurrency(amounts)
                        prepareSendActionForToken(
                            amount = amountToSend,
                            selectedWalletData = selectedWalletData,
                            walletStore = walletStore,
                        )
                    }
                }
            } else {
                val amountsSize = amounts?.size ?: 0
                if (amountsSize > 1) {
                    WalletAction.DialogAction.ChooseCurrency(amounts)
                } else {
                    val amountToSend = amounts?.first()
                    PrepareSendScreen(
                        coinAmount = amountToSend,
                        coinRate = selectedWalletData?.fiatRate,
                        walletManager = walletStore?.walletManager,
                    )
                }
            }
        }
    }

    private fun prepareSendActionForToken(
        amount: Amount,
        selectedWalletData: WalletDataModel?,
        walletStore: WalletStoreModel?,
    ): PrepareSendScreen {
        val coinRate = walletStore?.blockchainWalletData?.fiatRate
        val tokenRate = selectedWalletData?.fiatRate
        val coinAmount = walletStore?.walletManager?.wallet?.amounts?.get(AmountType.Coin)

        return PrepareSendScreen(
            coinAmount = coinAmount,
            coinRate = coinRate,
            walletManager = walletStore?.walletManager,
            tokenAmount = amount,
            tokenRate = tokenRate,
        )
    }
}