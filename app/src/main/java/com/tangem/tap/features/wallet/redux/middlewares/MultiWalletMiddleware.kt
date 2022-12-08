package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.analytics.events.Token.ButtonRemoveToken
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchErrorNotification
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.scanCard.ScanCardProcessor
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.toCurrencies
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.features.wallet.redux.reducers.toWallet
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.userTokensRepository
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletCurrenciesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class MultiWalletMiddleware {
    fun handle(
        action: WalletAction.MultiWallet, walletState: WalletState?, globalState: GlobalState?,
    ) {
        val globalState = globalState ?: return

        when (action) {
            is WalletAction.MultiWallet.AddBlockchains -> {
                handleAddingWalletManagers(globalState, action.walletManagers)
            }
            is WalletAction.MultiWallet.SelectWallet -> {
                if (action.walletData != null) {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.WalletDetails))
                }
            }
            is WalletAction.MultiWallet.AddToken -> {
                addTokens(listOf(action.token), action.blockchain, walletState, globalState, action.save)
            }
            is WalletAction.MultiWallet.AddTokens -> {
                addTokens(action.tokens, action.blockchain, walletState, globalState, save = false)
            }
            is WalletAction.MultiWallet.AddBlockchain -> {
                action.walletManager?.let {
                    handleAddingWalletManagers(globalState, listOf(action.walletManager))
                }
                val currencies: List<Currency> =
                    (walletState?.currencies ?: emptyList()) + action.blockchain.toCurrencies()


                if (action.save && globalState.scanResponse != null) {
                    scope.launch {
                        userTokensRepository.saveUserTokens(
                            card = globalState.scanResponse.card,
                            tokens = currencies,
                        )
                    }
                }

                store.dispatch(
                    WalletAction.LoadFiatRate(
                        coinsList = listOf(
                            Currency.Blockchain(
                                action.blockchain.blockchain,
                                action.blockchain.derivationPath,
                            ),
                        ),
                    ),
                )
                store.dispatch(
                    WalletAction.LoadWallet(
                        action.blockchain, action.walletManager,
                    ),
                )
            }
            is WalletAction.MultiWallet.SaveCurrencies -> {
                val card = action.card ?: globalState.scanResponse?.card ?: return
                scope.launch { userTokensRepository.saveUserTokens(card, action.blockchainNetworks.toCurrencies()) }
            }
            is WalletAction.MultiWallet.TryToRemoveWallet -> {
                val currency = action.currency
                val walletManager = walletState?.getWalletManager(currency).guard {
                    store.dispatchErrorNotification(TapError.UnsupportedState("walletManager is NULL"))
                    store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                    return
                }

                if (currency.isBlockchain() && walletManager.cardTokens.isNotEmpty()) {
                    store.dispatchDialogShow(
                        WalletDialog.TokensAreLinkedDialog(
                            currencyTitle = currency.currencyName,
                            currencySymbol = currency.currencySymbol,
                        ),
                    )
                } else {
                    store.dispatchDialogShow(
                        WalletDialog.RemoveWalletDialog(
                            currencyTitle = currency.currencyName,
                            onOk = {
                                Analytics.send(ButtonRemoveToken(AnalyticsParam.CurrencyType.Currency(currency)))
                                store.dispatch(WalletAction.MultiWallet.RemoveWallet(currency))
                                store.dispatch(NavigationAction.PopBackTo())
                            },
                        ),
                    )
                }
            }
            is WalletAction.MultiWallet.RemoveWallet -> {
                val selectedUserWallet = userWalletsListManager.selectedUserWalletSync
                if (selectedUserWallet != null) scope.launch {
                    walletCurrenciesManager.removeCurrency(
                        userWallet = selectedUserWallet,
                        currencyToRemove = action.currency,
                    )
                } else {
                    val currency = action.currency
                    val card = globalState.scanResponse?.card.guard {
                        store.dispatchErrorNotification(TapError.UnsupportedState("card is NULL"))
                        store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                        return
                    }
                    var currencies = walletState?.currencies ?: emptyList()
                    currencies = currencies.filterNot { it == currency }
                    if (currency.isBlockchain()) {
                        currencies
                            .filter { it.blockchain == currency.blockchain && it.derivationPath == currency.derivationPath }
                    }
                    scope.launch { userTokensRepository.saveUserTokens(card, currencies) }
                }
            }
            is WalletAction.MultiWallet.RemoveWallets -> {
                val card = globalState.scanResponse?.card.guard {
                    store.dispatchErrorNotification(TapError.UnsupportedState("card is NULL"))
                    store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                    return
                }
                var currencies = walletState?.currencies ?: emptyList()
                currencies = currencies.filterNot { action.currencies.contains(it) }
                scope.launch { userTokensRepository.saveUserTokens(card, currencies) }
                store.dispatch(WalletAction.MultiWallet.SelectWallet(null))
            }
            is WalletAction.MultiWallet.ShowWalletBackupWarning -> Unit
            is WalletAction.MultiWallet.BackupWallet -> {
                store.state.globalState.scanResponse?.let {
                    store.dispatch(GlobalAction.Onboarding.Start(it, canSkipBackup = false))
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
                }
            }
            is WalletAction.MultiWallet.ScanToGetDerivations -> {
                val selectedWallet = userWalletsListManager.selectedUserWalletSync
                if (selectedWallet != null) {
                    scanAndUpdateCard(selectedWallet, walletState)
                } else {
                    store.dispatch(WalletAction.Scan)
                }
            }
        }
    }

    private fun scanAndUpdateCard(
        selectedWallet: UserWallet,
        state: WalletState?,
    ) = scope.launch {
        Analytics.send(MainScreen.CardWasScanned())
        ScanCardProcessor.scan(
            cardId = selectedWallet.cardId,
            additionalBlockchainsToDerive = state?.missingDerivations?.map { it.blockchain },
        ) { scanResponse ->
            val userWallet = selectedWallet.copy(
                scanResponse = scanResponse,
            )

            userWalletsListManager.save(userWallet, canOverride = true)
                .doOnSuccess {
                    store.state.globalState.tapWalletManager.loadData(userWallet, refresh = true)
                }
        }
    }

    private fun addDummyBalances(walletManagers: List<WalletManager>) {
        walletManagers.forEach {
            if (it.wallet.fundsAvailable(AmountType.Coin) == BigDecimal.ZERO) {
                DemoHelper.injectDemoBalance(it)
            }
        }
    }

    private fun handleAddingWalletManagers(
        globalState: GlobalState,
        walletManagers: List<WalletManager>,
    ) {
        globalState.feedbackManager?.infoHolder?.setWalletsInfo(walletManagers)
        if (globalState.scanResponse?.isDemoCard() == true) {
            addDummyBalances(walletManagers)
        }
    }

    private fun addTokens(
        tokens: List<Token>, blockchainNetwork: BlockchainNetwork,
        walletState: WalletState?, globalState: GlobalState?,
        save: Boolean,
    ) {
        if (tokens.isEmpty()) return
        val scanResponse = globalState?.scanResponse ?: return
        val wmFactory = globalState.tapWalletManager.walletManagerFactory
        val walletState = walletState ?: return
        val walletManager = walletState.getWalletManager(blockchainNetwork)?.also {
            if (save) {
                val wallets = tokens.mapNotNull { token -> token.toWallet(walletState, blockchainNetwork) }
                val currencies = walletState.updateWalletsData(wallets).currencies
                scope.launch { userTokensRepository.saveUserTokens(scanResponse.card, currencies) }
            }
        } ?: wmFactory.makeWalletManagerForApp(scanResponse, blockchainNetwork)?.also {
            store.dispatch(WalletAction.MultiWallet.AddBlockchain(blockchainNetwork.updateTokens(tokens), it, save))
        }

        store.dispatch(
            WalletAction.LoadFiatRate(
                coinsList = tokens.map { token ->
                    Currency.Token(
                        token, blockchainNetwork.blockchain, blockchainNetwork.derivationPath,
                    )
                },
            ),
        )
        if (tokens.isNotEmpty()) walletManager?.addTokens(tokens)

        scope.launch {
            val result = walletManager?.safeUpdate()
            withMainContext {
                when (result) {
                    is com.tangem.common.services.Result.Success -> {
                        val wallet = result.data
                        wallet.getTokens()
                            .filter { tokens.contains(it) }
                            .mapNotNull { token ->
                                wallet.getTokenAmount(token)?.let { Pair(token, it) }
                            }
                            .forEach {
                                withContext(Dispatchers.Main) {
                                    store.dispatch(
                                        WalletAction.MultiWallet.TokenLoaded(
                                            it.second,
                                            it.first,
                                            blockchainNetwork,
                                        ),
                                    )
                                }
                            }
                    }
                    else -> {}
                }
            }
        }
    }
}
