package com.tangem.tap.features.wallet.redux.middlewares

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.tangem.blockchain.common.*
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.isZero
import com.tangem.tap.*
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.extensions.shareText
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.extensions.toSendableAmounts
import com.tangem.tap.domain.twins.TwinsHelper
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.twins.CreateTwinWallet
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.network.NetworkStateChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

class WalletMiddleware {
    private val topUpMiddleware = TopUpMiddleware()
    private val twinsMiddleware = TwinsMiddleware()
    private val warningsMiddleware = WarningsMiddleware()
    private val multiWalletMiddleware = MultiWalletMiddleware()

    val walletMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                val globalState = state()?.globalState
                val walletState = state()?.walletState
                when (action) {
                    is WalletAction.TopUpAction -> topUpMiddleware.handle(action)
                    is WalletAction.TwinsAction -> twinsMiddleware.handle(action)
                    is WalletAction.Warnings -> warningsMiddleware.handle(action, globalState)
                    is WalletAction.MultiWallet ->
                        multiWalletMiddleware.handle(action, walletState, globalState)

                    is WalletAction.LoadWallet -> {
                        scope.launch {
                            if (action.currency == null) {
                                walletState?.walletManagers?.map { walletManager ->
                                    globalState?.tapWalletManager?.loadWalletData(walletManager)
                                }
                            } else {
                                val walletManager = walletState?.getWalletManager(action.currency)
                                walletManager?.let { globalState?.tapWalletManager?.loadWalletData(it) }
                            }
                        }
                    }
                    is WalletAction.LoadWallet.Success -> {
                        val coinAmount = action.wallet.amounts[AmountType.Coin]?.value
                        if (coinAmount != null && !coinAmount.isZero()) {
                            if (walletState?.getWalletData(action.wallet.blockchain.currency) == null) {
                                store.dispatch(WalletAction.MultiWallet.AddBlockchain(action.wallet.blockchain))
                                store.dispatch(WalletAction.LoadWallet.Success(action.wallet))
                            }
                        }
                        store.dispatch(WalletAction.Warnings.CheckHashesCount.CheckHashesCountOnline)
                        warningsMiddleware.tryToShowAppRatingWarning(action.wallet)
                    }
                    is WalletAction.LoadFiatRate -> {
                        scope.launch {
                            if (action.wallet != null) {
                                globalState?.tapWalletManager?.loadFiatRate(
                                        globalState.appCurrency, action.wallet
                                )
                            } else if (action.currency != null) {
                                globalState?.tapWalletManager?.loadFiatRate(
                                        globalState.appCurrency, action.currency
                                )
                            } else {
                                walletState?.wallets?.filter { it.blockchain != null }
                                        ?.map { walletState.getWalletManager(it.blockchain?.currency) }
                                        ?.mapNotNull { walletManager ->
                                            walletManager?.let {
                                                globalState?.tapWalletManager?.loadFiatRate(
                                                        globalState.appCurrency, walletManager.wallet
                                                )
                                            }
                                        }
                            }
                        }
                    }
                    is WalletAction.CreateWallet -> {
                        if (walletState?.twinCardsState != null) {
                            store.dispatch(DetailsAction.CreateTwinWalletAction.ShowWarning(
                                    globalState?.scanNoteResponse?.card?.cardId?.let {
                                        TwinsHelper.getTwinCardNumber(it)
                                    },
                                    CreateTwinWallet.CreateWallet
                            ))
                        } else {
                            scope.launch {
                                val result = tangemSdkManager.createWallet(
                                        globalState?.scanNoteResponse?.card?.cardId
                                )
                                when (result) {
                                    is CompletionResult.Success -> {
                                        globalState?.tapWalletManager?.onCardScanned(result.data)
                                    }
                                }
                            }
                        }
                    }
                    is WalletAction.UpdateWallet -> {
                        if (action.currency != null) {
                            scope.launch {
                                val walletManager = walletState?.getWalletManager(action.currency)
                                walletManager?.let { globalState?.tapWalletManager?.updateWallet(it) }
                            }
                        } else {
                            scope.launch {
                                if (walletState?.state == ProgressState.Done) {
                                    walletState.walletManagers.map { walletManager ->
                                        globalState?.tapWalletManager?.updateWallet(walletManager)
                                    }
                                }
                            }
                        }

                    }
                    is WalletAction.Scan -> {
                        scope.launch {
                            val result = tangemSdkManager.scanNote(FirebaseAnalyticsHandler)
                            scope.launch(Dispatchers.Main) {
                                store.dispatch(GlobalAction.ScanFailsCounter.ChooseBehavior(result))
                                when (result) {
                                    is CompletionResult.Success -> {
                                        tangemSdkManager.changeDisplayedCardIdNumbersCount(result.data.card)
                                        globalState?.tapWalletManager?.onCardScanned(result.data, true)
                                    }
                                }
                            }
                        }
                    }
                    is WalletAction.LoadData -> {
                        scope.launch {
                            val scanNoteResponse = globalState?.scanNoteResponse
                                    ?: return@launch
                            if (!walletState?.wallets.isNullOrEmpty()) {
                                globalState.tapWalletManager.reloadData(scanNoteResponse)
                            } else {
                                globalState.tapWalletManager.loadData(scanNoteResponse)
                            }
                        }
                    }
                    is NetworkStateChanged -> {
                        globalState?.scanNoteResponse?.let { scanNoteResponse ->
                            store.dispatch(WalletAction.Warnings.CheckHashesCount.CheckHashesCountOnline)
                            scope.launch {
                                globalState.tapWalletManager.loadData(scanNoteResponse)
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
                        val uri = Uri.parse(action.exploreUrl)
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        ContextCompat.startActivity(action.context, intent, null)
                    }
                    is WalletAction.Send -> {
                        val newAction = prepareSendAction(action.amount, state()?.walletState)
                        store.dispatch(newAction)
                        if (newAction is PrepareSendScreen) {
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.Send))
                        }
                    }
                }
                next(action)
            }
        }
    }

    private fun prepareSendAction(amount: Amount?, state: WalletState?): Action {
        val selectedWalletData = state?.getSelectedWalletData()
        val currency = selectedWalletData?.currencyData?.currencySymbol
        val walletManager = state?.getWalletManager(currency)
        val wallet = walletManager?.wallet

        return if (amount != null) {
            if (amount.type is AmountType.Token) {
                prepareSendActionForToken(amount, state, selectedWalletData, wallet, walletManager)
            } else {
                PrepareSendScreen(amount, selectedWalletData?.fiatRate, walletManager)
            }
        } else {
            val amounts = wallet?.amounts?.toSendableAmounts()
            if (currency != null && state.isMultiwalletAllowed) {
                val amountToSend = amounts?.find { it.currencySymbol == currency }
                        ?: return WalletAction.Send.ChooseCurrency(amounts)
                if (amountToSend.type is AmountType.Token) {
                    prepareSendActionForToken(amount, state, selectedWalletData, wallet, walletManager)
                } else {
                    PrepareSendScreen(amountToSend, selectedWalletData.fiatRate, walletManager)
                }
            } else {
                if (amounts?.size ?: 0 > 1) {
                    WalletAction.Send.ChooseCurrency(amounts)
                } else {
                    val amountToSend = amounts?.first()
                    PrepareSendScreen(amountToSend, selectedWalletData?.fiatRate, walletManager)
                }
            }
        }
    }

    private fun prepareSendActionForToken(
            amount: Amount?, state: WalletState?, selectedWalletData: WalletData?, wallet: Wallet?,
            walletManager: WalletManager?
    ): PrepareSendScreen {
        val coinRate = state?.getWalletData(wallet?.blockchain?.currency)?.fiatRate
        val tokenRate = if (state?.isMultiwalletAllowed == true) {
            selectedWalletData?.fiatRate
        } else {
            selectedWalletData?.currencyData?.token?.fiatRate
        }
        return PrepareSendScreen(
                wallet?.amounts?.get(AmountType.Coin), coinRate, walletManager,
                amount, tokenRate)
    }
}

