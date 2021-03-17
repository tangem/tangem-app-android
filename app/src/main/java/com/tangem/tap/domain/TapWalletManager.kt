package com.tangem.tap.domain

import com.tangem.blockchain.common.*
import com.tangem.commands.common.card.CardStatus
import com.tangem.commands.common.network.Result
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.extensions.amountToCreateAccount
import com.tangem.tap.domain.extensions.isNoAccountError
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.tasks.isMultiwalletAllowed
import com.tangem.tap.domain.twins.TwinsHelper
import com.tangem.tap.domain.twins.isTwinCard
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.network.coinmarketcap.CoinMarketCapService
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal


class TapWalletManager {
    private val coinMarketCapService = CoinMarketCapService()

    private val blockchainSdkConfig by lazy {
        store.state.globalState.configManager?.config?.blockchainSdkConfig ?: BlockchainSdkConfig()
    }
    private val walletManagerFactory: WalletManagerFactory
            by lazy { WalletManagerFactory(blockchainSdkConfig) }

    suspend fun loadWalletData(walletManager: WalletManager) {
        val result = try {
            walletManager.update()
            Result.Success(walletManager.wallet)
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
        handleUpdateWalletResult(result, walletManager)
    }

    suspend fun updateWallet(walletManager: WalletManager) {
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

    suspend fun loadFiatRate(fiatCurrency: FiatCurrencyName, wallet: Wallet) {
        val currencyList = wallet.getTokens().map { it.symbol }.toMutableList()
        currencyList.add(wallet.blockchain.currency)
        loadFiatRate(fiatCurrency, currencyList)
    }

    suspend fun loadFiatRate(fiatCurrency: FiatCurrencyName, cryptoCurrencyName: CryptoCurrencyName) {
        val currencyList = listOf(cryptoCurrencyName)
        loadFiatRate(fiatCurrency, currencyList)
    }

    private suspend fun loadFiatRate(fiatCurrency: FiatCurrencyName, currencyList: List<CryptoCurrencyName>) {
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
            store.dispatch(WalletAction.MultiWallet.SetIsMultiwalletAllowed(data.card.isMultiwalletAllowed))
            if (data.card.isTwinCard()) {
                val secondCardId = TwinsHelper.getTwinsCardId(data.card.cardId)
                val cardNumber = TwinsHelper.getTwinCardNumber(data.card.cardId)
                if (secondCardId != null && cardNumber != null) {
                    store.dispatch(WalletAction.TwinsAction.SetTwinCard(
                            secondCardId, cardNumber, isCreatingTwinCardsAllowed = true
                    ))
                }
            }
            if (data.walletManager?.wallet?.blockchain == Blockchain.Ethereum ||
                    data.walletManager?.wallet?.blockchain == Blockchain.EthereumTestnet) {
                store.dispatch(TokensAction.LoadCardTokens)
            }
            loadData(data)
        }
    }

    private fun updateConfigManager(data: ScanNoteResponse) {
        val configManager = store.state.globalState.configManager
        if (TapWorkarounds.isStart2Coin) {
            configManager?.turnOff(ConfigManager.isSendingToPayIdEnabled)
            configManager?.turnOff(ConfigManager.isTopUpEnabled)
        } else if (data.walletManager?.wallet?.blockchain == Blockchain.Bitcoin
                || data.card.cardData?.blockchainName == Blockchain.Bitcoin.id) {
            configManager?.resetToDefault(ConfigManager.isSendingToPayIdEnabled)
            configManager?.resetToDefault(ConfigManager.isTopUpEnabled)
        } else {
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

                val primaryWalletManager = data.walletManager
                val primaryBlockchain = listOf(data.walletManager.wallet.blockchain)
                val primaryToken = primaryWalletManager.presetTokens.toList()

                store.dispatch(WalletAction.MultiWallet.SetPrimaryBlockchain(primaryBlockchain[0]))
                if (primaryWalletManager.presetTokens.isNotEmpty()) {
                    store.dispatch(WalletAction.MultiWallet.SetPrimaryToken(primaryToken.first()))
                }

                if (data.card.isMultiwalletAllowed) {
                    val savedCurrencies = currenciesRepository.loadCardCurrencies(data.card.cardId)
                    val tokens = if (savedCurrencies.tokens.isNotEmpty()) {
                        primaryToken + savedCurrencies.tokens
                    } else {
                        primaryToken
                    }

                    val walletManagers = listOf(primaryWalletManager) +
                            currenciesRepository.getBlockchains()
                                    .filterNot { it == primaryWalletManager.wallet.blockchain }
                                    .mapNotNull { walletManagerFactory.makeWalletManager(data.card, it) }
                    val otherBlockhains = savedCurrencies.blockchains

                    store.dispatch(WalletAction.MultiWallet.AddWalletManagers(walletManagers))
                    store.dispatch(WalletAction.MultiWallet.AddBlockchains(primaryBlockchain + otherBlockhains))
                    store.dispatch(WalletAction.MultiWallet.AddTokens(tokens))
                } else {
                    store.dispatch(WalletAction.MultiWallet.AddWalletManagers(listOf(primaryWalletManager)))
                    store.dispatch(WalletAction.MultiWallet.AddBlockchains(primaryBlockchain))
                }
                store.dispatch(WalletAction.SetArtworkId(data.verifyResponse?.artworkInfo?.id))
                store.dispatch(WalletAction.LoadWallet(config.isTopUpEnabled))
                store.dispatch(WalletAction.LoadArtwork(data.card, artworkId))
                store.dispatch(WalletAction.LoadFiatRate())
            } else if (data.card.status == CardStatus.Empty || data.card.isTwinCard()) {
                store.dispatch(WalletAction.EmptyWallet)
                store.dispatch(WalletAction.LoadArtwork(data.card, artworkId))
            } else {
                store.dispatch(WalletAction.LoadData.Failure(TapError.UnknownBlockchain))
                store.dispatch(WalletAction.LoadArtwork(data.card, artworkId))
            }
        }
    }

    suspend fun reloadData(data: ScanNoteResponse) {
        withContext(Dispatchers.Main) {
            if (data.walletManager != null) {
                if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
                    store.dispatch(WalletAction.LoadData.Failure(TapError.NoInternetConnection))
                    return@withContext
                }
                val config = store.state.globalState.configManager?.config ?: return@withContext
                store.dispatch(WalletAction.LoadWallet(config.isTopUpEnabled))
                store.dispatch(WalletAction.LoadFiatRate())
            } else if (data.card.status == CardStatus.Empty || data.card.isTwinCard()) {
                store.dispatch(WalletAction.EmptyWallet)
            } else {
                store.dispatch(WalletAction.LoadData.Failure(TapError.UnknownBlockchain))
            }
        }
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
                            store.dispatch(WalletAction.LoadWallet.NoAccount(
                                    walletManager.wallet,
                                    amountToCreateAccount.toString()))
                            return@withContext
                        }
                    }
                    store.dispatch(WalletAction.LoadWallet.Failure(walletManager.wallet, result.error?.localizedMessage))
                }
            }
        }
    }

    private suspend fun handleFiatRatesResult(results: List<Pair<CryptoCurrencyName, Result<BigDecimal>?>>) {
        withContext(Dispatchers.Main) {
            results.map {
                when (it.second) {
                    is Result.Success -> {
                        val rate = it.first to (it.second as Result.Success<BigDecimal>).data
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