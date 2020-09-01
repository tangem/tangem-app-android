package com.tangem.tap.features.wallet.redux

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.tangem.commands.common.network.Result
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import com.tangem.tap.TapConfig
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.isPayIdSupported
import com.tangem.tap.network.coinmarketcap.CoinMarketCapService
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
                        val walletManager = store.state.globalState.walletManager
                        if (walletManager == null) {
                            store.dispatch(WalletAction.LoadWallet.Failure)
                            return@launch
                        }
                        try {
                            walletManager.update()
                        } catch (ex: Exception) {
                            withContext(Dispatchers.Main) {
                                store.dispatch(WalletAction.LoadWallet.Failure)
//                            callback(CompletionResult.Failure(BlockchainInternalErrorConverter.convert(ex)))
                                next(action)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            store.dispatch(WalletAction.LoadWallet.Success(walletManager.wallet))
                        }
                    }
                }
                is WalletAction.LoadFiatRate -> {
                    scope.launch {
                        val blockchainCurrency = store.state.globalState.walletManager?.wallet?.blockchain?.currency
                        val tokenCurrency = store.state.globalState.walletManager?.wallet?.token?.symbol

                        val blockchainRate = blockchainCurrency?.let { CoinMarketCapService().getRate(it) }
                        val tokenRate = tokenCurrency?.let { CoinMarketCapService().getRate(it) }

                        withContext(Dispatchers.Main) {
                            when (blockchainRate) {
                                is Result.Success -> {
                                    store.dispatch(GlobalAction.SetFiatRate(blockchainCurrency to blockchainRate.data)
                                    )
                                    store.dispatch(WalletAction.LoadFiatRate.Success(blockchainCurrency to blockchainRate.data)
                                    )
                                }
                                is Result.Failure, null -> store.dispatch(WalletAction.LoadFiatRate.Failure)
                            }
                            when (tokenRate) {
                                is Result.Success -> {
                                    store.dispatch(GlobalAction.SetFiatRate(tokenCurrency to tokenRate.data)
                                    )
                                    store.dispatch(WalletAction.LoadFiatRate.Success(tokenCurrency to tokenRate.data)
                                    )
                                }
                                is Result.Failure, null -> store.dispatch(WalletAction.LoadFiatRate.Failure)
                            }
                        }
                    }
                }
                is WalletAction.LoadPayId -> {
                    if (!TapConfig.usePayId ||
                            store.state.walletState.payIdData.payIdState == PayIdState.Disabled ||
                            store.state.globalState.walletManager?.wallet?.blockchain?.isPayIdSupported() == false) {
                        next(action)
                    }
                    scope.launch {
                        val cardId = store.state.globalState.card?.cardId
                        val publicKey = store.state.globalState.card?.cardPublicKey
                        if (cardId != null && publicKey != null) {
                            val result = PayIdManager().getPayId(cardId, publicKey.toHexString())
                            withContext(Dispatchers.Main) {
                                when (result) {
                                    is Result.Success -> {
                                        val payId = result.data
                                        if (payId == null) {
                                            store.dispatch(WalletAction.LoadPayId.NotCreated)
                                        } else {
                                            store.dispatch(WalletAction.LoadPayId.Success(payId))
                                        }
                                    }
                                    is Result.Failure -> store.dispatch(WalletAction.LoadPayId.Failure)
                                }
                            }
                        }
                    }
                }
                is WalletAction.CreatePayId.CompleteCreatingPayId -> {
                    scope.launch {
                        val cardId = store.state.globalState.card?.cardId
                        val wallet = store.state.walletState.wallet
                        val publicKey = store.state.globalState.card?.cardPublicKey
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
                                withContext(Dispatchers.Main) {
                                    store.dispatch(GlobalAction.LoadCard(result.data.card))
                                    store.dispatch(GlobalAction.LoadWalletManager(result.data.walletManager))
                                    store.dispatch(WalletAction.LoadWallet)
                                    store.dispatch(WalletAction.LoadFiatRate)
                                    store.dispatch(WalletAction.LoadPayId)
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