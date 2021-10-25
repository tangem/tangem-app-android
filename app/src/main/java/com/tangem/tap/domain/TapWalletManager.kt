package com.tangem.tap.domain

import com.tangem.blockchain.common.*
import com.tangem.common.card.Card
import com.tangem.common.services.Result
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.TapWorkarounds.isStart2Coin
import com.tangem.tap.domain.TapWorkarounds.isTestCard
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.extensions.*
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.tokens.CardCurrencies
import com.tangem.tap.domain.twins.getTwinCardNumber
import com.tangem.tap.domain.twins.isTangemTwin
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
        handleUpdateWalletResult(walletManager.safeUpdate(), walletManager)
    }

    suspend fun updateWallet(walletManager: WalletManager) {
        val result = walletManager.safeUpdate()
        withContext(Dispatchers.Main) {
            when (result) {
                is Result.Success ->
                    store.dispatch(WalletAction.UpdateWallet.Success(result.data))
                is Result.Failure ->
                    store.dispatch(WalletAction.UpdateWallet.Failure(result.error.localizedMessage))
            }
        }

    }

    suspend fun loadFiatRate(fiatCurrency: FiatCurrencyName, wallet: Wallet) {
        Timber.d(wallet.getTokens().toString())
        val currencies = wallet.getTokens()
            .map { Currency.Token(it) }
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

    suspend fun onCardScanned(data: ScanResponse, addAnalyticsEvent: Boolean = false) {
        if (addAnalyticsEvent) {
            FirebaseAnalyticsHandler.triggerEvent(AnalyticsEvent.CARD_IS_SCANNED, data.card, data.walletData?.blockchain)
        }
        store.state.globalState.feedbackManager?.infoHolder?.setCardInfo(data.card)
        updateConfigManager(data)

        withContext(Dispatchers.Main) {
            store.dispatch(WalletAction.ResetState)
            store.dispatch(GlobalAction.SaveScanNoteResponse(data))
            store.dispatch(WalletAction.SetIfTestnetCard(data.card.isTestCard))
            store.dispatch(WalletAction.MultiWallet.SetIsMultiwalletAllowed(data.card.isMultiwalletAllowed))
            if (data.card.isTangemTwin()) {
                data.card.getTwinCardNumber()?.let {
                    store.dispatch(WalletAction.TwinsAction.SetTwinCard(it, true))
                }
            }

            val blockchain = data.getBlockchain()
            if (blockchain == Blockchain.Ethereum ||
                    blockchain == Blockchain.EthereumTestnet) {
                store.dispatch(TokensAction.LoadCardTokens)
            }
            loadData(data)
        }
    }

    private fun updateConfigManager(data: ScanResponse) {
        val configManager = store.state.globalState.configManager
        val blockchain = data.getBlockchain()
        if (data.card.isStart2Coin) {
            configManager?.turnOff(ConfigManager.isSendingToPayIdEnabled)
            configManager?.turnOff(ConfigManager.isTopUpEnabled)
        } else if (blockchain == Blockchain.Bitcoin
                || data.walletData?.blockchain == Blockchain.Bitcoin.id) {
            configManager?.resetToDefault(ConfigManager.isSendingToPayIdEnabled)
            configManager?.resetToDefault(ConfigManager.isTopUpEnabled)
        } else {
            configManager?.resetToDefault(ConfigManager.isSendingToPayIdEnabled)
            configManager?.resetToDefault(ConfigManager.isTopUpEnabled)
        }
    }

    suspend fun loadData(data: ScanResponse) {
        withContext(Dispatchers.Main) {

            store.dispatch(WalletAction.LoadCardInfo(data.card))

            when {
                data.getBlockchain() == Blockchain.Unknown && !data.card.isMultiwalletAllowed -> {
                    store.dispatch(WalletAction.LoadData.Failure(TapError.UnknownBlockchain))
                }
                data.card.wallets.isEmpty() ||
                        (data.card.isTangemTwin() && data.secondTwinPublicKey == null) -> {
                    store.dispatch(WalletAction.EmptyWallet)
                }
                else -> {
                    val config = store.state.globalState.configManager?.config ?: return@withContext

                    val blockchain = data.getBlockchain()
                    val primaryWalletManager = walletManagerFactory.makePrimaryWalletManager(data)

                    if (blockchain != Blockchain.Unknown && primaryWalletManager != null) {
                        val primaryToken = data.getPrimaryToken()

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
                    val moonPayUserStatus = store.state.globalState.moonPayUserStatus
                    store.dispatch(WalletAction.LoadWallet(
                        allowToBuy = config.isTopUpEnabled && moonPayUserStatus?.isBuyAllowed == true,
                        allowToSell = config.isTopUpEnabled && moonPayUserStatus?.isSellAllowed == true,
                    ))
                    store.dispatch(WalletAction.LoadFiatRate())
                }
            }
        }
    }

    private fun loadMultiWalletData(
            card: Card, primaryBlockchain: Blockchain?, primaryWalletManager: WalletManager?
    ) {
        val primaryTokens = primaryWalletManager?.cardTokens ?: emptySet()
        val savedCurrencies = currenciesRepository.loadCardCurrencies(card.cardId)

        if (savedCurrencies == null) {
            if (primaryBlockchain != null && primaryWalletManager != null) {
                store.dispatch(WalletAction.MultiWallet.SaveCurrencies(
                        CardCurrencies(
                                blockchains = setOf(primaryBlockchain), tokens = primaryTokens
                        )))
                store.dispatch(WalletAction.MultiWallet.AddWalletManagers(primaryWalletManager))
                store.dispatch(WalletAction.MultiWallet.AddBlockchains(listOf(primaryBlockchain)))
                store.dispatch(WalletAction.MultiWallet.AddTokens(primaryTokens.toList()))
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
                primaryTokens.isNotEmpty() &&
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

    suspend fun reloadData(data: ScanResponse) {
        withContext(Dispatchers.Main) {
            when {
                data.getBlockchain() == Blockchain.Unknown && !data.card.isMultiwalletAllowed -> {
                    store.dispatch(WalletAction.LoadData.Failure(TapError.UnknownBlockchain))
                }
                data.card.wallets.isEmpty() ||
                        (data.card.isTangemTwin() && data.secondTwinPublicKey == null) -> {
                    store.dispatch(WalletAction.EmptyWallet)
                }
                else -> {
                    if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
                        store.dispatch(WalletAction.LoadData.Failure(TapError.NoInternetConnection))
                        return@withContext
                    }
                    val config = store.state.globalState.configManager?.config ?: return@withContext
                    val moonpayUserStatus = store.state.globalState.moonPayUserStatus
                    store.dispatch(WalletAction.LoadWallet(
                        allowToBuy = config.isTopUpEnabled && moonpayUserStatus?.isBuyAllowed == true,
                        allowToSell = config.isTopUpEnabled && moonpayUserStatus?.isSellAllowed == true,
                    ))
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
                    val error = (result.error as? TapError) ?: TapError.UnknownError
                    when (error) {
                        TapError.NoInternetConnection -> {
                            store.dispatch(WalletAction.LoadData.Failure(error))
                        }
                        is TapError.WalletManagerUpdate.NoAccountError -> {
                            store.dispatch(WalletAction.LoadWallet
                                    .NoAccount(walletManager.wallet, error.customMessage))
                        }
                        is TapError.WalletManagerUpdate.InternalError -> {
                            store.dispatch(WalletAction.LoadWallet
                                    .Failure(walletManager.wallet, error.customMessage))
                        }
                        else -> {
                            store.dispatchDebugErrorNotification(error)
                        }
                    }
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