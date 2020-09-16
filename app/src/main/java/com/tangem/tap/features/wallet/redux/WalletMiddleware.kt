package com.tangem.tap.features.wallet.redux

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.commands.common.network.Result
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.send.redux.PrepareSendScreen
import com.tangem.tap.network.NetworkStateChanged
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.Dispatchers
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
                        store.state.globalState.tapWalletManager.loadFiatRate()
                    }
                }
                is WalletAction.LoadArtwork -> {
                    scope.launch {
                        store.state.globalState.tapWalletManager.loadArtwork(
                                action.card, action.artworkId
                        )
                    }
                }
                is WalletAction.CreateWallet -> {
                    scope.launch {
                        val result = tangemSdkManager.createWallet()
                        when (result) {
                            is CompletionResult.Success -> {
                                store.state.globalState.tapWalletManager.onCardScanned(result.data)
                            }

                        }
                    }
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
                is WalletAction.LoadData, is NetworkStateChanged -> {
                    scope.launch {
                        store.state.globalState.scanNoteResponse?.let {
                            store.state.globalState.tapWalletManager.onCardScanned(it)
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
            }
            next(action)
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
        if (store.state.walletState.wallet?.amounts?.size ?: 0 > 1) {
            WalletAction.Send.ChooseCurrency
        } else {
            val amountToSend = store.state.walletState.wallet?.amounts?.toList()?.get(0)?.second
            PrepareSendScreen(amountToSend)
        }
    }
}