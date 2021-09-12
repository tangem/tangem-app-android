package com.tangem.tap.domain

import com.tangem.blockchain.common.*
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.CardStatus
import com.tangem.commands.common.network.Result
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.TapWorkarounds.isStart2Coin
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.extensions.*
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.tokens.CardCurrencies
import com.tangem.tap.domain.twins.TwinsHelper
import com.tangem.tap.domain.twins.isTwinCard
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.network.coinmarketcap.CoinMarketCapService
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal


class TapWalletManager {
    private val coinMarketCapService = CoinMarketCapService()

    private val blockchainSdkConfig by lazy {
        store.state.globalState.configManager?.config?.blockchainSdkConfig ?: BlockchainSdkConfig()
    }
    val walletManagerFactory: WalletManagerFactory
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
        Timber.d(wallet.getTokens().toString())
        val currencies = wallet.getTokens()
            .map { Currency.Token(it, wallet.blockchain) }
            .plus(Currency.Blockchain(wallet.blockchain))
        loadFiatRate(fiatCurrency, currencies)
    }

    suspend fun loadFiatRate(fiatCurrency: FiatCurrencyName, currency: Currency) {
        val currencies = listOf(currency)
        loadFiatRate(fiatCurrency, currencies)
    }

    suspend fun loadFiatRate(fiatCurrency: FiatCurrencyName, currencies: List<Currency>) {
        val results = mutableListOf<Pair<Currency, Result<BigDecimal>?>>()
        currencies.forEach {
            results.add(it to coinMarketCapService.getRate(it.currencySymbol, fiatCurrency))
        }
        handleFiatRatesResult(results)
    }

    suspend fun onCardScanned(data: ScanNoteResponse, addAnalyticsEvent: Boolean = false) {
        if (addAnalyticsEvent) {
            FirebaseAnalyticsHandler.triggerEvent(AnalyticsEvent.CARD_IS_SCANNED, data.card)
        }
        store.state.globalState.feedbackManager?.infoHolder?.setCardInfo(data.card)
        updateConfigManager(data)

        withContext(Dispatchers.Main) {
            store.dispatch(WalletAction.ResetState)
            store.dispatch(GlobalAction.SaveScanNoteResponse(data))
            store.dispatch(WalletAction.MultiWallet.SetIsMultiwalletAllowed(data.card.isMultiwalletAllowed))
            if (data.card.isTwinCard()) {
                val cardNumber = TwinsHelper.getTwinCardNumber(data.card.cardId)
                if (cardNumber != null) {
                    store.dispatch(WalletAction.TwinsAction.SetTwinCard(
                            cardNumber, isCreatingTwinCardsAllowed = true
                    ))
                }
            }

            val blockchain = data.card.getBlockchain()
            if (blockchain == Blockchain.Ethereum ||
                    blockchain == Blockchain.EthereumTestnet) {
                store.dispatch(TokensAction.LoadCardTokens)
            }
            loadData(data)
        }
    }

    private fun updateConfigManager(data: ScanNoteResponse) {
        val configManager = store.state.globalState.configManager
        val blockchain = data.card.getBlockchain()
        if (data.card.isStart2Coin) {
            configManager?.turnOff(ConfigManager.isSendingToPayIdEnabled)
            configManager?.turnOff(ConfigManager.isTopUpEnabled)
        } else if (blockchain == Blockchain.Bitcoin
                || data.card.cardData?.blockchainName == Blockchain.Bitcoin.id) {
            configManager?.resetToDefault(ConfigManager.isSendingToPayIdEnabled)
            configManager?.resetToDefault(ConfigManager.isTopUpEnabled)
        } else {
            configManager?.resetToDefault(ConfigManager.isSendingToPayIdEnabled)
            configManager?.resetToDefault(ConfigManager.isTopUpEnabled)
        }
    }

    suspend fun loadData(data: ScanNoteResponse) {
        withContext(Dispatchers.Main) {
            val artworkId = data.verifyResponse?.artworkInfo?.id
            when {
                data.card.getBlockchain() == Blockchain.Unknown && !data.card.isMultiwalletAllowed -> {
                    store.dispatch(WalletAction.LoadData.Failure(TapError.UnknownBlockchain))
                    store.dispatch(WalletAction.LoadArtwork(data.card, artworkId))
                }
                data.card.getStatus() == CardStatus.Empty ||
                        (data.card.isTwinCard() && data.secondTwinPublicKey == null) -> {
                    store.dispatch(WalletAction.EmptyWallet)
                    store.dispatch(WalletAction.LoadArtwork(data.card, artworkId))
                }
                else -> {
                    val config = store.state.globalState.configManager?.config ?: return@withContext

                    val blockchain = data.card.getBlockchain()
                    val primaryWalletManager = walletManagerFactory.makePrimaryWalletManager(data)

                    if (blockchain != null && primaryWalletManager != null) {
                        val primaryToken = data.card.getToken()

                        store.dispatch(WalletAction.MultiWallet.SetPrimaryBlockchain(blockchain))
                        if (primaryToken != null) {
                            primaryWalletManager.addToken(primaryToken)
                            store.dispatch(WalletAction.MultiWallet.SetPrimaryToken(primaryToken))
                        }
                        if (data.card.isMultiwalletAllowed) {
                            loadMultiWalletData(data.card, blockchain, primaryWalletManager)
                        } else {
                            store.dispatch(WalletAction.MultiWallet.AddWalletManagers(primaryWalletManager))
                            store.dispatch(WalletAction.MultiWallet.AddBlockchains(listOf(blockchain)))
                        }

                    } else {
                        if (data.card.isMultiwalletAllowed) {
                            loadMultiWalletData(data.card, blockchain, null)
                        }
                    }
                    store.dispatch(WalletAction.SetArtworkId(data.verifyResponse?.artworkInfo?.id))
                    store.dispatch(WalletAction.LoadWallet(config.isTopUpEnabled))
                    store.dispatch(WalletAction.LoadArtwork(data.card, artworkId))
                    store.dispatch(WalletAction.LoadFiatRate())
                }
            }
            store.dispatch(WalletAction.Warnings.CheckIfNeeded)
        }
    }

    private fun loadMultiWalletData(
            card: Card, primaryBlockchain: Blockchain?, primaryWalletManager: WalletManager?
    ) {
        val presetTokens = primaryWalletManager?.presetTokens ?: emptySet()
        val savedCurrencies = currenciesRepository.loadCardCurrencies(card.cardId)

        if (savedCurrencies == null) {
            if (primaryBlockchain != null && primaryWalletManager != null) {
                store.dispatch(WalletAction.MultiWallet.SaveCurrencies(
                        CardCurrencies(
                                blockchains = setOf(primaryBlockchain), tokens = presetTokens
                        )))
                store.dispatch(WalletAction.MultiWallet.AddWalletManagers(primaryWalletManager))
                store.dispatch(WalletAction.MultiWallet.AddBlockchains(listOf(primaryBlockchain)))
                store.dispatch(WalletAction.MultiWallet.AddTokens(presetTokens.toList()))
            } else {
                val blockchains = setOf(Blockchain.Bitcoin, Blockchain.Ethereum)
                store.dispatch(WalletAction.MultiWallet.SaveCurrencies(
                    CardCurrencies(blockchains = blockchains, tokens = emptySet())
                ))
                val walletManagers =
                    walletManagerFactory.makeWalletManagersForApp(card, blockchains.toList())
                store.dispatch(WalletAction.MultiWallet.AddWalletManagers(walletManagers))
                store.dispatch(WalletAction.MultiWallet.AddBlockchains(blockchains.toList()))
            }
            store.dispatch(WalletAction.MultiWallet.FindBlockchainsInUse(card, walletManagerFactory))
            store.dispatch(WalletAction.MultiWallet.FindTokensInUse)
        } else {
            val blockchains = savedCurrencies.blockchains.toList()
            val walletManagers = if (
                presetTokens.isNotEmpty() &&
                primaryWalletManager != null && primaryBlockchain != null
            ) {
                val blockchainsWithoutPrimary = blockchains.filterNot { it == primaryBlockchain }
                walletManagerFactory.makeWalletManagersForApp(card, blockchainsWithoutPrimary)
                    .plus(primaryWalletManager)
            } else {
                walletManagerFactory.makeWalletManagersForApp(card, blockchains)
            }

            store.dispatch(WalletAction.MultiWallet.AddWalletManagers(walletManagers))
            store.dispatch(WalletAction.MultiWallet.AddBlockchains(blockchains))
            store.dispatch(WalletAction.MultiWallet.AddTokens(savedCurrencies.tokens.toList()))
        }
    }

    suspend fun reloadData(data: ScanNoteResponse) {
        withContext(Dispatchers.Main) {
            when {
                data.card.getBlockchain() == Blockchain.Unknown && !data.card.isMultiwalletAllowed -> {
                    store.dispatch(WalletAction.LoadData.Failure(TapError.UnknownBlockchain))
                }
                data.card.getStatus() == CardStatus.Empty ||
                        (data.card.isTwinCard() && data.secondTwinPublicKey == null) -> {
                    store.dispatch(WalletAction.EmptyWallet)
                }
                else -> {
                    if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
                        store.dispatch(WalletAction.LoadData.Failure(TapError.NoInternetConnection))
                        return@withContext
                    }
                    val config = store.state.globalState.configManager?.config ?: return@withContext
                    store.dispatch(WalletAction.LoadWallet(config.isTopUpEnabled))
                    store.dispatch(WalletAction.LoadFiatRate())
                }
            }
        }
    }

    private suspend fun handleUpdateWalletResult(result: Result<Wallet>, walletManager: WalletManager) {
        withContext(Dispatchers.Main) {
            when (result) {
                is Result.Success -> store.dispatch(WalletAction.LoadWallet.Success(result.data))
                is Result.Failure -> {
                    if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
                        store.dispatch(WalletAction.LoadData.Failure(TapError.NoInternetConnection))
                        return@withContext
                    }
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

    private suspend fun handleFiatRatesResult(results: List<Pair<Currency, Result<BigDecimal>?>>) {
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