package com.tangem.tap.domain

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.commands.CardStatus
import com.tangem.commands.common.network.Result
import com.tangem.common.extensions.toHexString
import com.tangem.tap.TapConfig
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.extensions.amountToCreateAccount
import com.tangem.tap.domain.extensions.isNoAccountError
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.features.wallet.redux.PayIdState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.network.coinmarketcap.CoinMarketCapService
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal


class TapWalletManager {
    private val payIdManager = PayIdManager()
    private val coinMarketCapService = CoinMarketCapService()

    suspend fun loadWalletData() {
        val walletManager = store.state.globalState.scanNoteResponse?.walletManager
        if (walletManager == null) {
            store.dispatch(WalletAction.LoadWallet.Failure())
            return
        }
        loadWallet(walletManager)
    }

    suspend fun loadPayId() {
        val result = loadPayIdIfNeeded()
        result?.let { handlePayIdResult(it) }
    }

    suspend fun updateWallet() {
        val walletManager = store.state.globalState.scanNoteResponse?.walletManager
        if (walletManager == null) {
            store.dispatch(WalletAction.UpdateWallet.Failure())
            return
        }
        val result = try {
            walletManager.update()
            Result.Success(walletManager.wallet)
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
        withContext(Dispatchers.Main) {
            when (result) {
                is Result.Success -> store.dispatch(WalletAction.UpdateWallet.Success(result.data))
                is Result.Failure ->
                    store.dispatch(WalletAction.UpdateWallet.Failure(result.error?.localizedMessage))
            }
        }

    }

    suspend fun loadFiatRate(fiatCurrency: FiatCurrencyName) {
        val wallet = store.state.globalState.scanNoteResponse?.walletManager?.wallet
        val blockchainCurrency = wallet?.blockchain?.currency
        val tokenCurrency = wallet?.token?.symbol

        val blockchainRate = blockchainCurrency?.let { coinMarketCapService.getRate(it, fiatCurrency) }
        val tokenRate = tokenCurrency?.let { coinMarketCapService.getRate(it, fiatCurrency) }

        val results = mutableListOf<Pair<CryptoCurrencyName, Result<BigDecimal>?>>()
        if (blockchainCurrency != null) results.add(blockchainCurrency to blockchainRate)
        if (tokenCurrency != null) results.add(tokenCurrency to tokenRate)

        handleFiatRatesResult(results)
    }

    suspend fun onCardScanned(data: ScanNoteResponse) {
        withContext(Dispatchers.Main) {
            store.dispatch(WalletAction.ResetState)
            store.dispatch(GlobalAction.SaveScanNoteResponse(data))
            loadData(data)
        }
    }

    suspend fun loadData(data: ScanNoteResponse) {
        withContext(Dispatchers.Main) {
            store.dispatch(WalletAction.CheckIfWarningNeeded)
            val artworkId = data.verifyResponse?.artworkInfo?.id
            if (data.walletManager != null) {
                if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
                    store.dispatch(WalletAction.LoadData.Failure(TapError.NoInternetConnection))
                    return@withContext
                }
                store.dispatch(WalletAction.LoadWallet)
                store.dispatch(WalletAction.LoadArtwork(data.card, artworkId))
                store.dispatch(WalletAction.LoadFiatRate)
                store.dispatch(WalletAction.LoadPayId)
            } else if (data.card.status == CardStatus.Empty) {
                store.dispatch(WalletAction.EmptyWallet)
                store.dispatch(WalletAction.LoadArtwork(data.card, artworkId))
            } else {
                store.dispatch(WalletAction.LoadData.Failure(TapError.UnknownBlockchain))
                store.dispatch(WalletAction.LoadArtwork(data.card, artworkId))
            }
        }
    }

    private suspend fun loadWallet(walletManager: WalletManager) {
        val result = try {
            walletManager.update()
            Result.Success(walletManager.wallet)
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
        handleUpdateWalletResult(result, walletManager)
    }

    private suspend fun handleUpdateWalletResult(result: Result<Wallet>, walletManager: WalletManager) {
        withContext(Dispatchers.Main) {
            when (result) {
                is Result.Success -> store.dispatch(WalletAction.LoadWallet.Success(result.data))
                is Result.Failure -> {
                    val error = result.error
                    val blockchain = walletManager.wallet.blockchain
                    if (error != null && blockchain.isNoAccountError(error)) {
                        val amountToCreateAccount = blockchain.amountToCreateAccount(walletManager.wallet.token)
                        if (amountToCreateAccount != null) {
                            store.dispatch(WalletAction.LoadWallet.NoAccount(amountToCreateAccount.toString()))
                            return@withContext
                        }
                    }
                    store.dispatch(WalletAction.LoadWallet.Failure(result.error?.localizedMessage))
                }
            }
        }
    }


    private suspend fun loadPayIdIfNeeded(): Result<String?>? {
        val scanNoteResponse = store.state.globalState.scanNoteResponse
        if (!TapConfig.usePayId ||
                store.state.walletState.payIdData.payIdState == PayIdState.Disabled ||
                scanNoteResponse?.walletManager?.wallet?.blockchain?.isPayIdSupported() == false) {
            return null
        }
        val cardId = scanNoteResponse?.card?.cardId
        val publicKey = scanNoteResponse?.card?.cardPublicKey
        if (cardId == null || publicKey == null) {
            return null
        }
        return payIdManager.getPayId(cardId, publicKey.toHexString())
    }

    private suspend fun handlePayIdResult(result: Result<String?>) {
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

    private suspend fun handleFiatRatesResult(results: List<Pair<CryptoCurrencyName, Result<BigDecimal>?>>) {
        withContext(Dispatchers.Main) {
            results.map {
                when (it.second) {
                    is Result.Success -> {
                        val rate = it.first to (it.second as Result.Success<BigDecimal>).data
                        store.dispatch(GlobalAction.SetFiatRate(rate))
                        store.dispatch(WalletAction.LoadFiatRate.Success(rate))
                    }
                    is Result.Failure -> store.dispatch(WalletAction.LoadFiatRate.Failure)
                    null -> {
                    }
                }
            }
        }
    }
}
