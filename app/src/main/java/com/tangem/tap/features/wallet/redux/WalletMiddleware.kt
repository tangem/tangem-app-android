package com.tangem.tap.features.wallet.redux

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.Card
import com.tangem.commands.common.network.Result
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.CardType
import com.tangem.common.extensions.getType
import com.tangem.common.extensions.toHexString
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.toSendableAmounts
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


val walletMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
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
                    scope.launch {
                        val result = tangemSdkManager.createWallet(
                                store.state.globalState.scanNoteResponse?.card?.cardId
                        )
                        when (result) {
                            is CompletionResult.Success -> {
                                store.state.globalState.tapWalletManager.onCardScanned(result.data)
                            }

                        }
                    }
                }
                is WalletAction.UpdateWallet -> {
                    scope.launch { store.state.globalState.tapWalletManager.updateWallet() }
                }
                is WalletAction.UpdateWallet.Success -> setupWalletUpdate(action.wallet)
                is WalletAction.LoadWallet.Success -> {
                    store.dispatch(WalletAction.CheckHashesCountOnline)
                    if (!store.state.walletState.updatingWallet) setupWalletUpdate(action.wallet)
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
                        val result = tangemSdkManager.scanNote()
                        when (result) {
                            is CompletionResult.Success -> {
                                store.state.globalState.tapWalletManager.onCardScanned(result.data)
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
                    store.state.walletState.addressData?.address?.let {
                        action.context.copyToClipboard(it)
                        store.dispatch(WalletAction.CopyAddress.Success)
                    }
                }
                is WalletAction.ExploreAddress -> {
                    val uri = Uri.parse(store.state.walletState.addressData?.exploreUrl)
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
                is WalletAction.CheckIfWarningNeeded -> {
                    val card = store.state.globalState.scanNoteResponse?.card
                    val validator = store.state.globalState.scanNoteResponse?.walletManager
                            as? SignatureCountValidator
                    if (card != null && !preferencesStorage.wasCardScannedBefore(card.cardId)) {
                        val result = checkIfWarningNeeded(card, validator)
                        if (result != null) store.dispatch(WalletAction.ShowWarning(result))
                    }
                }
                is WalletAction.CheckHashesCountOnline -> checkHashesCountOnline()
                is WalletAction.SaveCardId -> {
                    val cardId = store.state.globalState.scanNoteResponse?.card?.cardId
                    cardId?.let { preferencesStorage.saveScannedCardId(it) }
                }
            }
            next(action)
        }
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
        if (amount.type == AmountType.Token) {
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
        card: Card, signatureCountValidator: SignatureCountValidator? = null
): WarningType? {

    if (card.getType() != CardType.Release) {
        return WarningType.DevCard
    }

    return if (signatureCountValidator == null) {
        if (card.walletSignedHashes ?: 0 > 0) {
            WarningType.CardSignedHashesBefore
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
                        store.dispatch(WalletAction.ShowWarning(WarningType.CardSignedHashesBefore))
                    } else if (card.walletSignedHashes ?: 0 > 0) {
                        store.dispatch(WalletAction.ShowWarning(WarningType.CardSignedHashesBefore))
                    }
            }
        }
    }
}
