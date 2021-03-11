package com.tangem.tap.features.wallet.redux

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.tangem.TangemSdkError
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.CardType
import com.tangem.commands.common.network.Result
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.getType
import com.tangem.common.extensions.toHexString
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.extensions.isGreaterThan
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.TopUpHelper
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.domain.extensions.toSendableAmounts
import com.tangem.tap.domain.twins.TwinsHelper
import com.tangem.tap.domain.twins.isTwinCard
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.twins.CreateTwinWallet
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.features.wallet.models.toPendingTransactions
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.network.NetworkStateChanged
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.Middleware
import java.math.BigDecimal

class WalletMiddleware {
    private val topUpMiddleware = TopUpMiddleware()

    val walletMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                when (action) {
                    is WalletAction.TopUpAction -> topUpMiddleware.handle(action)
                    is WalletAction.LoadWallet -> {
                        scope.launch {
                            store.state.globalState.tapWalletManager.loadWalletData()
                        }
                    }
                    is WalletAction.LoadPayId -> {
                        scope.launch {
                            store.state.globalState.tapWalletManager.loadPayId()
                        }
                    }
                    is WalletAction.LoadFiatRate -> {
                        scope.launch {
                            store.state.globalState.tapWalletManager.loadFiatRate(store.state.globalState.appCurrency)
                        }
                    }
                    is WalletAction.CreateWallet -> {
                        if (store.state.walletState.twinCardsState != null) {
                            store.dispatch(DetailsAction.CreateTwinWalletAction.ShowWarning(
                                    store.state.globalState.scanNoteResponse?.card?.cardId?.let {
                                        TwinsHelper.getTwinCardNumber(it)
                                    },
                                    CreateTwinWallet.CreateWallet
                            ))
                        } else {
                            scope.launch {
                                val result = tangemSdkManager.createWallet(
                                        store.state.globalState.scanNoteResponse?.card?.cardId
                                )
                                when (result) {
                                    is CompletionResult.Success -> {
                                        store.state.globalState.tapWalletManager
                                                .onCardScanned(result.data)
                                    }

                                }
                            }
                        }
                    }
                    is WalletAction.UpdateWallet -> {
                        if (store.state.walletState.state == ProgressState.Done) {
                            scope.launch { store.state.globalState.tapWalletManager.updateWallet() }
                        }
                    }
                    is WalletAction.UpdateWallet.Success -> setupWalletUpdate(action.wallet)
                    is WalletAction.LoadWallet.Success -> {
                        store.dispatch(WalletAction.CheckHashesCountOnline)
                        if (!store.state.walletState.updatingWallet) setupWalletUpdate(action.wallet)
                        tryToShowAppRatingWarning(action.wallet)
                    }
                    is WalletAction.CreatePayId.CompleteCreatingPayId -> {
                        scope.launch {
                            val cardId = store.state.globalState.scanNoteResponse?.card?.cardId
                            val wallet = store.state.globalState.scanNoteResponse?.walletManager?.wallet
                            val publicKey = store.state.globalState.scanNoteResponse?.card?.cardPublicKey
                            if (cardId != null && wallet != null && publicKey != null) {
                                val result = PayIdManager().setPayId(
                                        cardId, publicKey.toHexString(),
                                        action.payId, wallet.address, wallet.blockchain
                                )
                                withContext(Dispatchers.Main) {
                                    when (result) {
                                        is Result.Success ->
                                            store.dispatch(WalletAction.CreatePayId.Success(action.payId))
                                        is Result.Failure -> {
                                            val error = result.error as? TapError
                                                    ?: TapError.PayIdCreatingError
                                            store.dispatch(WalletAction.CreatePayId.Failure(error))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is WalletAction.Scan -> {
                        scope.launch {
                            val result = tangemSdkManager.scanNote(FirebaseAnalyticsHandler)
                            when (result) {
                                is CompletionResult.Success -> {
                                    tangemSdkManager.changeDisplayedCardIdNumbersCount(result.data.card)
                                    store.state.globalState.tapWalletManager
                                            .onCardScanned(result.data, true)
                                    if (store.state.walletState.twinCardsState != null) {
                                        val showOnboarding = !preferencesStorage.wasTwinsOnboardingShown()
                                        if (showOnboarding) {
                                            store.dispatch(NavigationAction.NavigateTo(AppScreen.TwinsOnboarding))
                                        }
                                    }
                                    store.dispatch(WalletAction.ScanCardFinished())
                                }
                                is CompletionResult.Failure -> {
                                    if (result.error !is TangemSdkError.UserCancelled) {
                                        // Weird things... If you run the code below without coroutines,
                                        // then rescanning will be impossible
                                        scope.launch(Dispatchers.Main) {
                                            store.dispatch(WalletAction.ScanCardFinished(result.error))
                                            if (store.state.walletState.scanCardFailsCounter >= 2) {
                                                store.dispatch(WalletAction.ShowDialog.ScanFails)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is WalletAction.LoadData -> {
                        scope.launch {
                            store.state.globalState.scanNoteResponse?.let {
                                store.state.globalState.tapWalletManager.loadData(it)
                            }
                        }
                    }
                    is NetworkStateChanged -> {
                        store.state.globalState.scanNoteResponse?.let { scanNoteResponse ->
                            store.dispatch(WalletAction.CheckHashesCountOnline)
                            scope.launch {
                                store.state.globalState.tapWalletManager.onCardScanned(scanNoteResponse)
                            }
                        }
                    }
                    is WalletAction.CopyAddress -> {
                        store.state.walletState.walletAddresses?.selectedAddress?.address?.let {
                            action.context.copyToClipboard(it)
                            store.dispatch(WalletAction.CopyAddress.Success)
                        }
                    }
                    is WalletAction.ExploreAddress -> {
                        val uri = Uri.parse(store.state.walletState.walletAddresses?.selectedAddress?.exploreUrl)
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        ContextCompat.startActivity(action.context, intent, null)
                    }
                    is WalletAction.Send -> {
                        val newAction = prepareSendAction(action.amount)
                        store.dispatch(newAction)
                        if (newAction is PrepareSendScreen) {
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.Send))
                        }
                    }
                    is WalletAction.Warnings.CheckIfNeeded -> {
                        val globalState = store.state.globalState
                        val validator = globalState.scanNoteResponse?.walletManager as? SignatureCountValidator
                        globalState.scanNoteResponse?.card?.let { card ->
                            store.state.globalState.warningManager?.removeWarnings(WarningMessage.Origin.Local)
                            if (card.getType() != CardType.Release) addWarningMessage(WarningMessagesManager.devCardWarning())
                            if (!preferencesStorage.wasCardScannedBefore(card.cardId)) {
                                checkIfWarningNeeded(card, validator)?.let { addWarningMessage(it) }
                            }
                            updateWarningMessages()
                        }
                    }
                    is WalletAction.CheckHashesCountOnline -> checkHashesCountOnline()
                    is WalletAction.SaveCardId -> {
                        val cardId = store.state.globalState.scanNoteResponse?.card?.cardId
                        cardId?.let { preferencesStorage.saveScannedCardId(it) }
                    }
                    is WalletAction.TwinsAction.SetTwinCard -> {
                        val showOnboarding = !preferencesStorage.wasTwinsOnboardingShown()
                        if (showOnboarding) store.dispatch(WalletAction.TwinsAction.ShowOnboarding)
                    }
                    is WalletAction.TwinsAction.SetOnboardingShown -> {
                        preferencesStorage.saveTwinsOnboardingShown()
                    }
                    is WalletAction.Warnings.AppRating.RemindLater -> {
                        preferencesStorage.appRatingLaunchObserver.applyDelayedShowing()
                    }
                    is WalletAction.Warnings.AppRating.SetNeverToShow -> {
                        preferencesStorage.appRatingLaunchObserver.setNeverToShow()
                    }
                }
                next(action)
            }
        }
    }

    private fun tryToShowAppRatingWarning(wallet: Wallet) {
        val nonZeroWalletsCount = wallet.amounts.filter {
            it.value.value?.isGreaterThan(BigDecimal.ZERO) ?: false
        }.size
        if (nonZeroWalletsCount > 0) {
            preferencesStorage.appRatingLaunchObserver.foundWalletWithFunds()
        }
        if (preferencesStorage.appRatingLaunchObserver.isReadyToShow()) {
            FirebaseAnalyticsHandler.triggerEvent(AnalyticsEvent.APP_RATING_DISPLAYED)
            addWarningMessage(WarningMessagesManager.appRatingWarning(), true)
        }
    }

    private fun setupWalletUpdate(wallet: Wallet) {
        if (!wallet.recentTransactions.toPendingTransactions(wallet.address).isNullOrEmpty()) {
            store.dispatch(WalletAction.UpdateWallet.ScheduleUpdatingWallet)
            scope.launch(Dispatchers.IO) {
                delay(10000)
                withContext(Dispatchers.Main) {
                    store.dispatch(WalletAction.UpdateWallet)
                }
            }
        }
    }


    private fun prepareSendAction(amount: Amount?): Action {
        return if (amount != null) {
            if (amount.type is AmountType.Token) {
                PrepareSendScreen(store.state.walletState.wallet?.amounts?.get(AmountType.Coin), amount)
            } else {
                PrepareSendScreen(amount)
            }
        } else {
            val amounts = store.state.walletState.wallet?.amounts?.toSendableAmounts()
            if (amounts?.size ?: 0 > 1) {
                WalletAction.Send.ChooseCurrency(amounts)
            } else {
                val amountToSend = amounts?.first()
                PrepareSendScreen(amountToSend)
            }
        }
    }

    private fun checkIfWarningNeeded(
            card: Card, signatureCountValidator: SignatureCountValidator? = null,
    ): WarningMessage? {
        if (card.isTwinCard()) return null

        return if (signatureCountValidator == null) {
            if (card.walletSignedHashes ?: 0 > 0) {
                WarningMessagesManager.alreadySignedHashesWarning()
            } else {
                store.dispatch(WalletAction.SaveCardId)
                null
            }
        } else {
            store.dispatch(WalletAction.NeedToCheckHashesCountOnline)
            null
        }
    }

    private fun checkHashesCountOnline() {
        if (store.state.walletState.hashesCountVerified != false) return
        if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) return

        val card = store.state.globalState.scanNoteResponse?.card
        if (card == null || preferencesStorage.wasCardScannedBefore(card.cardId)) return

        if (card.isTwinCard()) return

        val validator = store.state.globalState.scanNoteResponse?.walletManager
                as? SignatureCountValidator
        scope.launch {
            val result = validator?.validateSignatureCount(card.walletSignedHashes ?: 0)
            withContext(Dispatchers.Main) {
                when (result) {
                    SimpleResult.Success -> {
                        store.dispatch(WalletAction.ConfirmHashesCount)
                        store.dispatch(WalletAction.SaveCardId)
                    }
                    is SimpleResult.Failure ->
                        if (result.error is BlockchainSdkError.SignatureCountNotMatched) {
                            addWarningMessage(WarningMessagesManager.alreadySignedHashesWarning(), true)
                        } else if (card.walletSignedHashes ?: 0 > 0) {
                            addWarningMessage(WarningMessagesManager.alreadySignedHashesWarning(), true)
                        }
                }
            }
        }
    }

    private fun addWarningMessage(warning: WarningMessage, autoUpdate: Boolean = false) {
        store.state.globalState.warningManager?.addWarning(warning)
        if (autoUpdate) updateWarningMessages()
    }

    private fun updateWarningMessages() {
        val warningManager = store.state.globalState.warningManager ?: return
        store.dispatch(WalletAction.Warnings.SetWarnings(
                warningManager.getWarnings(WarningMessage.Location.MainScreen)))
    }
}

private class TopUpMiddleware {
    fun handle(action: WalletAction.TopUpAction) {
        when (action) {
            is WalletAction.TopUpAction.TopUp -> {
                val config = store.state.globalState.configManager?.config ?: return
                val addresses = store.state.walletState.walletAddresses ?: return
                if (addresses.list.isEmpty()) return

                val defaultAddress = addresses.list[0].address
                val url = TopUpHelper.getUrl(
                        store.state.walletState.currencyData.currencySymbol!!,
                        defaultAddress,
                        config.moonPayApiKey,
                        config.moonPayApiSecretKey
                )
                val customTabsIntent = CustomTabsIntent.Builder()
                        .setToolbarColor(action.toolbarColor)
                        .build()
                customTabsIntent.launchUrl(action.context, Uri.parse(url));
            }
        }
    }
}