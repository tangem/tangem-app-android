package com.tangem.tap.domain

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.commands.Card
import com.tangem.commands.common.network.Result
import com.tangem.commands.common.network.TangemService
import com.tangem.common.extensions.toHexString
import com.tangem.tap.TapConfig
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.features.wallet.redux.PayIdState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.coinmarketcap.CoinMarketCapService
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal


class TapWalletManager {
    private val payIdManager = PayIdManager()
    private val coinMarketCapService = CoinMarketCapService()
    private val tangemService = TangemService()

    suspend fun loadWalletData() {
        val walletManager = store.state.globalState.walletManager
        if (walletManager == null) {
            store.dispatch(WalletAction.LoadWallet.Failure())
            return
        }
        updateWallet(walletManager)
    }

    suspend fun loadArtwork(card: Card, artworkId: String) {
        val result =
                tangemService.getArtwork(card.cardId, card.cardPublicKey!!.toHexString(), artworkId)
        withContext(Dispatchers.Main) {
            when (result) {
                is Result.Success -> {
                    store.dispatch(WalletAction.LoadArtwork.Success(result.data.byteStream().readBytes()))
                }
                is Result.Failure -> store.dispatch(WalletAction.LoadArtwork.Failure)
            }
        }
    }

    suspend fun loadPayId() {
        val result = loadPayIdIfNeeded()
        result?.let { handlePayIdResult(it) }
    }

    suspend fun loadFiatRate() {
        val blockchainCurrency = store.state.globalState.walletManager?.wallet?.blockchain?.currency
        val tokenCurrency = store.state.globalState.walletManager?.wallet?.token?.symbol

        val blockchainRate = blockchainCurrency?.let { coinMarketCapService.getRate(it) }
        val tokenRate = tokenCurrency?.let { coinMarketCapService.getRate(it) }

        val results = mutableListOf<Pair<String, Result<BigDecimal>?>>()
        if (blockchainCurrency != null) results.add(blockchainCurrency to blockchainRate)
        if (tokenCurrency != null) results.add(tokenCurrency to tokenRate)

        handleFiatRatesResult(results)
    }

    suspend fun onCardScanned(data: ScanNoteResponse) {
        withContext(Dispatchers.Main) {
            store.dispatch(GlobalAction.LoadCard(data.card))
            if (data.walletManager != null) {
                store.dispatch(GlobalAction.LoadWalletManager(data.walletManager))
                data.verifyResponse?.artworkInfo?.id?.let {
                    store.dispatch(WalletAction.LoadArtwork(data.card, it))
                }
                store.dispatch(WalletAction.LoadWallet)
                store.dispatch(WalletAction.LoadFiatRate)
                store.dispatch(WalletAction.LoadPayId)
            } else {
                store.dispatch(WalletAction.EmptyWallet)
            }
        }
    }

    private suspend fun updateWallet(walletManager: WalletManager) {
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
                        val amountToCreateAccount = blockchain.amountToCreateAccount()
                        if (amountToCreateAccount != null) {
                            store.dispatch(WalletAction.LoadWallet.NoAccount(amountToCreateAccount))
                            return@withContext
                        }
                    }
                    store.dispatch(WalletAction.LoadWallet.Failure(result.error?.localizedMessage))
                }
            }
        }
    }


    private suspend fun loadPayIdIfNeeded(): Result<String?>? {
        if (!TapConfig.usePayId ||
                store.state.walletState.payIdData.payIdState == PayIdState.Disabled ||
                store.state.globalState.walletManager?.wallet?.blockchain?.isPayIdSupported() == false) {
            return null
        }
        val cardId = store.state.globalState.card?.cardId
        val publicKey = store.state.globalState.card?.cardPublicKey
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

    private suspend fun handleFiatRatesResult(results: List<Pair<String, Result<BigDecimal>?>>) {
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
