package com.tangem.tap.features.wallet.redux

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.tangem.common.CompletionResult
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Middleware


val walletMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is WalletAction.LoadWallet -> {
                    scope.launch {
                        try {
                            action.walletManager.update()
                        } catch (ex: Exception) {
                            store.dispatch(WalletAction.LoadWallet.Failure)
//                            callback(CompletionResult.Failure(BlockchainInternalErrorConverter.convert(ex)))
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            store.dispatch(WalletAction.LoadWallet.Success(action.walletManager.wallet))
                        }
                    }
                }
                is WalletAction.Scan -> {
                    scope.launch {
                        val result = tangemSdkManager.scanNote()
                        when (result) {
                            is CompletionResult.Success -> {
                                withContext(Dispatchers.Main) {
                                    store.dispatch(WalletAction.LoadWallet(result.data.walletManager))
                                }
                            }
                        }
                    }
                }
                is WalletAction.CopyAddress -> {
                    store.state.walletState.wallet?.address?.let {
                        action.context.copyToClipboard(it)
                        store.dispatch(WalletAction.CopyAddress.Success)
                    }
                }
                is WalletAction.ExploreAddress -> {
                    val uri = Uri.parse(store.state.walletState.wallet?.exploreUrl)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    ContextCompat.startActivity(action.context, intent, null)
                }

            }
            next(action)
        }
    }
}