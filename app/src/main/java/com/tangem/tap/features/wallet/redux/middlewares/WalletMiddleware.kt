package com.tangem.tap.features.wallet.redux.middlewares

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.tangem.blockchain.blockchains.solana.RentProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.isZero
import com.tangem.common.services.Result
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.extensions.toSendableAmounts
import com.tangem.tap.domain.failedRates
import com.tangem.tap.domain.loadedRates
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.tap.features.wallet.models.getPendingTransactions
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.network.NetworkStateChanged
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
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
                        store.dispatch(WalletAction.MultiWallet.AddBlockchain(
                            action.blockchain,
                            null
                        ))
                        store.dispatch(WalletAction.LoadWallet.Success(
                            action.wallet,
                            action.blockchain
                        ))
                    }
                }
                store.dispatch(WalletAction.Warnings.CheckHashesCount.CheckHashesCountOnline)
                warningsMiddleware.tryToShowAppRatingWarning(action.wallet)
            }
            is WalletAction.LoadFiatRate -> {
                val appCurrencyId = globalState.appCurrency
                scope.launch {
                    val coinsList = when {
                        action.wallet != null -> {
                            val wallet = action.wallet
                            wallet.getTokens()
                                .map { Currency.Token(it, wallet.blockchain, wallet.publicKey.derivationPath?.rawPath) }
                                .plus(Currency.Blockchain(wallet.blockchain, wallet.publicKey.derivationPath?.rawPath))
                        }
                        action.coinsList != null -> action.coinsList
                        else -> walletState.walletsData.map { it.currency }
                    }
                    val ratesResult = globalState.tapWalletManager.rates.loadFiatRate(
                        currencyId = appCurrencyId,
                        coinsList = coinsList,
                    )
                    when (ratesResult) {
                        is Result.Success -> {
                            ratesResult.data.loadedRates.forEach {
                                dispatchOnMain(WalletAction.LoadFiatRate.Success(it.toPair()))
                            }
                            ratesResult.data.failedRates.forEach { (currency, throwable) ->
                                Timber.e(throwable, "Loading rates failed for [%s]", currency.currencySymbol)
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
                        is CompletionResult.Failure -> {
                            (result.error as? TangemSdkError)?.let { error ->
                                store.state.globalState.analyticsHandlers?.logCardSdkError(
                                    error,
                                    Analytics.ActionToLog.CreateWallet,
                                    card = store.state.detailsState.scanResponse?.card
                                )
                            }
                        }
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
            is WalletAction.LoadData -> {
                scope.launch {
                    val scanNoteResponse = globalState.scanResponse ?: return@launch
                    if (walletState.walletsData.isNotEmpty()) {
                        globalState.tapWalletManager.reloadData(scanNoteResponse)
                    } else {
                        globalState.tapWalletManager.loadData(scanNoteResponse)
                    }
                }
            }
            is NetworkStateChanged -> {
                globalState.scanResponse?.let { scanNoteResponse ->
                    store.dispatch(WalletAction.Warnings.CheckHashesCount.CheckHashesCountOnline)
                    scope.launch { globalState.tapWalletManager.loadData(scanNoteResponse) }
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
                val newAction = prepareSendAction(action.amount, store.state.walletState)
                store.dispatch(newAction)
                if (newAction is PrepareSendScreen) {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.Send))
                }
            }
            is WalletAction.ShowDialog.QrCode -> {
                val selectedWalletData =
                    walletState.getWalletData(walletState.selectedCurrency) ?: return
                val selectedAddressData =
                    selectedWalletData.walletAddresses?.selectedAddress ?: return

                val currency = selectedWalletData.currency
                store.dispatchDialogShow(AppDialog.AddressInfoDialog(currency, selectedAddressData))
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
            val amounts = walletStore?.walletManager?.wallet?.amounts?.toSendableAmounts()
            if (currency != null && state.isMultiwalletAllowed) {
                when (currency) {
                    is Currency.Blockchain -> {
                        val amountToSend =
                            amounts?.find { it.currencySymbol == currency.blockchain.currency }
                                ?: return WalletAction.Send.ChooseCurrency(amounts)
                        PrepareSendScreen(
                            coinAmount = amountToSend,
                            coinRate = selectedWalletData.fiatRate,
                            walletManager = walletStore.walletManager
                        )
                    }
                    is Currency.Token -> {
                        val amountToSend =
                            amounts?.find { it.currencySymbol == currency.token.symbol }
                                ?: return WalletAction.Send.ChooseCurrency(amounts)
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
                    WalletAction.Send.ChooseCurrency(amounts)
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
                    val outgoingTxs = walletManager.wallet.getPendingTransactions(PendingTransactionType.Outgoing)
                    val rentExempt = result.data
                    val show = if (outgoingTxs.isEmpty()) {
                        isNeedToShowWarning(balance, rentExempt)
                    } else {
                        val outgoingAmount = outgoingTxs.sumOf { it.amount ?: BigDecimal.ZERO }
                        val rest = balance.minus(outgoingAmount)
                        isNeedToShowWarning(rest, rentExempt)
                    }

                    val currency = walletManager.wallet.blockchain.currency
                    if (show) {
                        dispatchOnMain(WalletAction.SetWalletRent(
                            wallet = walletManager.wallet,
                            minRent = ("${rentProvider.rentAmount().stripZeroPlainString()} $currency"),
                            rentExempt = ("${rentExempt.stripZeroPlainString()} $currency")
                        ))
                    } else {
                        dispatchOnMain(WalletAction.RemoveWalletRent(walletManager.wallet))
                    }
                }
                is com.tangem.blockchain.extensions.Result.Failure -> {}
            }
        }
    }
}
