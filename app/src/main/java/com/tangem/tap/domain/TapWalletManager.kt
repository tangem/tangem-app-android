package com.tangem.tap.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.commands.common.card.CardStatus
import com.tangem.commands.common.network.Result
import com.tangem.common.extensions.toHexString
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.extensions.amountToCreateAccount
import com.tangem.tap.domain.extensions.isNoAccountError
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.twins.TwinsHelper
import com.tangem.tap.domain.twins.isTwinCard
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
            withContext(Dispatchers.Main) { store.dispatch(WalletAction.UpdateWallet.Failure()) }
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
                is Result.Success ->
                    store.dispatch(WalletAction.UpdateWallet.Success(result.data))
                is Result.Failure ->
                    store.dispatch(WalletAction.UpdateWallet.Failure(result.error?.localizedMessage))
            }
        }

    }

    suspend fun loadFiatRate(fiatCurrency: FiatCurrencyName) {
        val wallet = store.state.globalState.scanNoteResponse?.walletManager?.wallet ?: return

        val currencyList = wallet.getTokens().map { it.symbol }.toMutableList()
        currencyList.add(wallet.blockchain.currency)

        val results = mutableListOf<Pair<CryptoCurrencyName, Result<BigDecimal>?>>()
        currencyList.forEach { results.add(it to coinMarketCapService.getRate(it, fiatCurrency)) }
        handleFiatRatesResult(results)
    }

    suspend fun onCardScanned(data: ScanNoteResponse, addAnalyticsEvent: Boolean = false) {
        if (addAnalyticsEvent) {
            FirebaseAnalyticsHandler.triggerEvent(AnalyticsEvent.CARD_IS_SCANNED, data.card)
        }
        TapWorkarounds.updateCard(data.card)
        store.state.globalState.warningManager?.setBlockchain(data.walletManager?.wallet?.blockchain)
        updateConfigManager(data)
        updateFeedbackManager(data)

        withContext(Dispatchers.Main) {
            store.dispatch(WalletAction.ResetState)
            store.dispatch(GlobalAction.SaveScanNoteResponse(data))
            if (data.card.isTwinCard()) {
                val secondCardId = TwinsHelper.getTwinsCardId(data.card.cardId)
                val cardNumber = TwinsHelper.getTwinCardNumber(data.card.cardId)
                if (secondCardId != null && cardNumber != null) {
                    store.dispatch(WalletAction.TwinsAction.SetTwinCard(
                            secondCardId, cardNumber, isCreatingTwinCardsAllowed = true
                    ))
                }
            }
            loadData(data)
        }
    }

    private fun updateConfigManager(data: ScanNoteResponse) {
        val configManager = store.state.globalState.configManager
        if (TapWorkarounds.isStart2Coin) {
            configManager?.turnOff(ConfigManager.isWalletPayIdEnabled)
            configManager?.turnOff(ConfigManager.isSendingToPayIdEnabled)
            configManager?.turnOff(ConfigManager.isTopUpEnabled)
        } else if (data.walletManager?.wallet?.blockchain == Blockchain.Bitcoin
                || data.card.cardData?.blockchainName == Blockchain.Bitcoin.id) {
            configManager?.turnOff(ConfigManager.isWalletPayIdEnabled)
            configManager?.resetToDefault(ConfigManager.isSendingToPayIdEnabled)
            configManager?.resetToDefault(ConfigManager.isTopUpEnabled)
        } else {
            configManager?.resetToDefault(ConfigManager.isWalletPayIdEnabled)
            configManager?.resetToDefault(ConfigManager.isSendingToPayIdEnabled)
            configManager?.resetToDefault(ConfigManager.isTopUpEnabled)
        }
    }

    private fun updateFeedbackManager(data: ScanNoteResponse) {
        val card = data.card
        val wallet = data.walletManager?.wallet ?: return
        val infoHolder = store.state.globalState.feedbackManager?.infoHolder ?: return

        infoHolder.cardId = card.cardId
        infoHolder.cardFirmwareVersion = card.firmwareVersion.version
        infoHolder.signedHashesCount = card.walletSignedHashes?.toString() ?: "0"
        infoHolder.sourceAddress = wallet.address
        infoHolder.explorerLink = wallet.getExploreUrl(wallet.address)
        infoHolder.blockchain = wallet.blockchain
    }

    suspend fun loadData(data: ScanNoteResponse) {
        withContext(Dispatchers.Main) {
            store.dispatch(WalletAction.Warnings.CheckIfNeeded)
            val artworkId = data.verifyResponse?.artworkInfo?.id
            if (data.walletManager != null) {
                if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
                    store.dispatch(WalletAction.LoadData.Failure(TapError.NoInternetConnection))
                    return@withContext
                }
                val config = store.state.globalState.configManager?.config ?: return@withContext

                store.dispatch(WalletAction.LoadWallet(
                        data.walletManager.wallet, data.verifyResponse?.artworkInfo?.id, config.isTopUpEnabled))
                store.dispatch(WalletAction.LoadArtwork(data.card, artworkId))
                store.dispatch(WalletAction.LoadFiatRate)
                store.dispatch(WalletAction.LoadPayId)
            } else if (data.card.status == CardStatus.Empty || data.card.isTwinCard()) {
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
                        val token = walletManager.wallet.getFirstToken()
                        val amountToCreateAccount = blockchain.amountToCreateAccount(token)
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
        if (store.state.globalState.configManager?.config?.isWalletPayIdEnabled == false
                || scanNoteResponse?.walletManager?.wallet?.blockchain?.isPayIdSupported() == false) {
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
                    val config = store.state.globalState.configManager?.config
                    if (config?.isWalletPayIdEnabled == false) {
                        store.dispatch(WalletAction.DisablePayId)
                        return@withContext
                    }
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

fun Wallet.getFirstToken(): Token? {
    return getTokens().toList().getOrNull(0)
}