package com.tangem.tap.domain

import com.tangem.blockchain.common.*
import com.tangem.common.services.Result
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.network.api.tangemTech.TangemTechService
import com.tangem.tap.common.ThrottlerWithValues
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.extensions.isMultiwalletAllowed
import com.tangem.tap.domain.extensions.makePrimaryWalletManager
import com.tangem.tap.domain.extensions.makeWalletManagersForApp
import com.tangem.tap.domain.tokens.BlockchainNetwork
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal


class TapWalletManager {
    val walletManagerFactory: WalletManagerFactory
        by lazy { WalletManagerFactory(blockchainSdkConfig) }

    private val tangemTechService: TangemTechService
        get() = store.state.domainNetworks.tangemTechService

    private val blockchainSdkConfig by lazy {
        store.state.globalState.configManager?.config?.blockchainSdkConfig ?: BlockchainSdkConfig()
    }

    private val walletManagersThrottler = ThrottlerWithValues<Blockchain, Result<Wallet>>(10000)
    private val fiatRatesThrottler = ThrottlerWithValues<Currency, Result<BigDecimal>?>(60000)

    suspend fun loadWalletData(walletManager: WalletManager) {
        val blockchain = walletManager.wallet.blockchain
        val blockchainNetwork = BlockchainNetwork.fromWalletManager(walletManager)
        val result = if (walletManagersThrottler.isStillThrottled(blockchain)) {
            walletManagersThrottler.geValue(blockchain)!!
        } else {
            updateThrottlingForWalletManager(walletManager)
        }
        when (result) {
            is Result.Success -> {
                dispatchOnMain(WalletAction.LoadWallet.Success(result.data, blockchainNetwork))
            }
            is Result.Failure -> {
                when (result.error) {
                    is TapError.WalletManager.NoAccountError -> {
                        dispatchOnMain(
                            WalletAction.LoadWallet.NoAccount(
                                walletManager.wallet,
                                blockchainNetwork,
                                (result.error as TapError.WalletManager.NoAccountError).customMessage
                            )
                        )
                    }
                    else -> {
                        dispatchOnMain(
                            WalletAction.LoadWallet.Failure(
                                walletManager.wallet,
                                result.error.localizedMessage
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun updateThrottlingForWalletManager(walletManager: WalletManager): Result<Wallet> {
        val newResult = walletManager.safeUpdate()
        val blockchain = walletManager.wallet.blockchain
        walletManagersThrottler.updateThrottlingTo(blockchain)
        walletManagersThrottler.setValue(blockchain, newResult)
        return newResult
    }

    suspend fun loadFiatRate(currencyId: FiatCurrencyName, wallet: Wallet) {
        val coinsList = wallet.getTokens()
            .map { Currency.Token(it, wallet.blockchain, wallet.publicKey.derivationPath?.rawPath) }
            .plus(Currency.Blockchain(wallet.blockchain, wallet.publicKey.derivationPath?.rawPath))
        loadFiatRate(currencyId, coinsList)
    }

    suspend fun loadFiatRate(currencyId: FiatCurrencyName, coinsList: List<Currency>) {
        suspend fun handleFiatRatesResult(rates: Map<Currency, Result<BigDecimal>?>) {
            rates.forEach { (currency, priceResult) ->
                when (priceResult) {
                    is Result.Success -> {
                        dispatchOnMain(WalletAction.LoadFiatRate.Success(
                            currency to priceResult.data
                        ))
                    }
                    is Result.Failure -> dispatchOnMain(WalletAction.LoadFiatRate.Failure)
                    null -> {}
                }
            }
        }

        // get and submit previous result of equivalents.
        val throttledResult = coinsList.filter { fiatRatesThrottler.isStillThrottled(it) }.map {
            Pair(it, fiatRatesThrottler.geValue(it))
        }
        if (throttledResult.isNotEmpty()) {
            handleFiatRatesResult(throttledResult.toMap())
        }

        val currenciesToUpdate = coinsList.filter { !fiatRatesThrottler.isStillThrottled(it) }
        val coinIds = currenciesToUpdate.mapNotNull { it.coinId }.distinct()
        if (coinIds.isEmpty()) return

        //TODO: refactoring: move fiatRatesThrottler to the TangemTechRepository
        when (val result = tangemTechService.rates(currencyId, coinIds)) {
            is Result.Success -> {
                val ratesResultList: Map<String, Result<BigDecimal>> = result.data.rates.mapValues {
                    Result.Success(it.value.toBigDecimal())
                }
                val updatedCurrencies = mutableMapOf<Currency, Result<BigDecimal>?>()
                currenciesToUpdate.forEach { currency ->
                    ratesResultList[currency.coinId]?.let {
                        updatedCurrencies[currency] = it
                        fiatRatesThrottler.updateThrottlingTo(currency)
                        fiatRatesThrottler.setValue(currency, it)
                    }
                }
                handleFiatRatesResult(updatedCurrencies)
            }
            is Result.Failure -> dispatchOnMain(WalletAction.LoadFiatRate.Failure)
        }
    }

    suspend fun onCardScanned(data: ScanResponse) {
        walletManagersThrottler.clear()
//        fiatRatesThrottler.clear()
        store.state.globalState.feedbackManager?.infoHolder?.setCardInfo(data)
        updateConfigManager(data)

        withMainContext {
            store.dispatch(WalletAction.ResetState)
            store.dispatch(GlobalAction.SaveScanNoteResponse(data))
            store.dispatch(WalletAction.SetIfTestnetCard(data.card.isTestCard))
            store.dispatch(WalletAction.MultiWallet.SetIsMultiwalletAllowed(data.card.isMultiwalletAllowed))
            loadData(data)
        }
    }

    fun updateConfigManager(data: ScanResponse) {
        val configManager = store.state.globalState.configManager
        val blockchain = data.getBlockchain()
        if (data.card.isStart2Coin) {
            configManager?.turnOff(ConfigManager.isSendingToPayIdEnabled)
            configManager?.turnOff(ConfigManager.isTopUpEnabled)
        } else if (blockchain == Blockchain.Bitcoin
            || data.walletData?.blockchain == Blockchain.Bitcoin.id
        ) {
            configManager?.resetToDefault(ConfigManager.isSendingToPayIdEnabled)
            configManager?.resetToDefault(ConfigManager.isTopUpEnabled)
        } else {
            configManager?.resetToDefault(ConfigManager.isSendingToPayIdEnabled)
            configManager?.resetToDefault(ConfigManager.isTopUpEnabled)
        }
    }

    suspend fun loadData(data: ScanResponse) {
        dispatchOnMain(WalletAction.LoadCardInfo(data.card))
        getActionIfUnknownBlockchainOrEmptyWallet(data)?.let {
            dispatchOnMain(it)
            return
        }

        val blockchain = data.getBlockchain()
        val primaryWalletManager = walletManagerFactory.makePrimaryWalletManager(data)

        if (blockchain != Blockchain.Unknown && primaryWalletManager != null) {
            val primaryToken = data.getPrimaryToken()

            dispatchOnMain(WalletAction.MultiWallet.SetPrimaryBlockchain(blockchain))
            if (primaryToken != null) {
                primaryWalletManager.addToken(primaryToken)
                dispatchOnMain(WalletAction.MultiWallet.SetPrimaryToken(primaryToken))
            }
            if (data.card.isMultiwalletAllowed) {
                loadMultiWalletData(data, blockchain, primaryWalletManager)
            } else {
                dispatchOnMain(
                    WalletAction.MultiWallet.AddBlockchains(
                        listOf(BlockchainNetwork.fromWalletManager(primaryWalletManager)),
                        listOf(primaryWalletManager)
                    )
                )
            }
        } else {
            if (data.card.isMultiwalletAllowed) {
                loadMultiWalletData(data, blockchain, null)
            }
        }
        dispatchOnMain(
            WalletAction.LoadWallet(),
            WalletAction.LoadFiatRate()
        )
    }

    private suspend fun loadMultiWalletData(
        scanResponse: ScanResponse,
        primaryBlockchain: Blockchain?,
        primaryWalletManager: WalletManager?
    ) {
        val primaryTokens = primaryWalletManager?.cardTokens?.toList() ?: emptyList()
        val savedCurrencies = currenciesRepository.loadSavedCurrencies(scanResponse.card.cardId, scanResponse.card.derivationStyle)

        if (savedCurrencies.isEmpty()) {
            if (primaryBlockchain != null && primaryWalletManager != null) {
                val blockchainNetwork = BlockchainNetwork.fromWalletManager(primaryWalletManager)
                dispatchOnMain(
                    WalletAction.MultiWallet.SaveCurrencies(listOf(blockchainNetwork)),
                    WalletAction.MultiWallet.AddBlockchains(
                        listOf(blockchainNetwork),
                        listOf(primaryWalletManager)
                    ),
                    WalletAction.MultiWallet.AddTokens(primaryTokens.toList(), blockchainNetwork)
                )

            } else {
                val derivationStyle = scanResponse.card.derivationStyle
                val blockchainNetworks = listOf(
                    BlockchainNetwork(Blockchain.Bitcoin, scanResponse.card),
                    BlockchainNetwork(Blockchain.Ethereum, scanResponse.card)
                )

                val walletManagers = walletManagerFactory.makeWalletManagersForApp(
                    scanResponse,
                    blockchainNetworks
                )

                dispatchOnMain(
                    WalletAction.MultiWallet.SaveCurrencies(blockchainNetworks),
                    WalletAction.MultiWallet.AddBlockchains(blockchainNetworks, walletManagers),
                )
            }
//            dispatchOnMain(
//                WalletAction.MultiWallet.FindBlockchainsInUse,
//                WalletAction.MultiWallet.FindTokensInUse,
//            )
        } else {
            val walletManagers = if (
                primaryTokens.isNotEmpty() &&
                primaryWalletManager != null &&
                primaryBlockchain != null && primaryBlockchain != Blockchain.Unknown
            ) {
                val blockchainsWithoutPrimary = savedCurrencies.filterNot { it.blockchain == primaryBlockchain }
                walletManagerFactory.makeWalletManagersForApp(
                    scanResponse,
                    blockchainsWithoutPrimary
                ).plus(primaryWalletManager)
            } else {
                walletManagerFactory.makeWalletManagersForApp(scanResponse, savedCurrencies)
            }
            dispatchOnMain(
                WalletAction.MultiWallet.AddBlockchains(savedCurrencies, walletManagers),
            )
            savedCurrencies.map {
                if (it.tokens.isNotEmpty()) {
                    dispatchOnMain(WalletAction.MultiWallet.AddTokens(it.tokens, it))
                }
            }
        }
    }

    suspend fun reloadData(data: ScanResponse) {
        withContext(Dispatchers.Main) {
            getActionIfUnknownBlockchainOrEmptyWallet(data)?.let {
                store.dispatch(it)
                return@withContext
            }
            if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
                store.dispatch(WalletAction.LoadData.Failure(TapError.NoInternetConnection))
                return@withContext
            }

            store.dispatch(WalletAction.LoadWallet())
            store.dispatch(WalletAction.LoadFiatRate())
        }
    }

    private fun getActionIfUnknownBlockchainOrEmptyWallet(data: ScanResponse): WalletAction? {
        return when {
            // check order is important
            data.isTangemTwins() && !data.twinsIsTwinned() -> {
                WalletAction.EmptyWallet
            }
            data.getBlockchain() == Blockchain.Unknown && !data.card.isMultiwalletAllowed -> {
                WalletAction.LoadData.Failure(TapError.UnknownBlockchain)
            }
            data.isDemoCard() -> {
                return null
            }
            data.card.wallets.isEmpty() -> {
                WalletAction.EmptyWallet
            }
            else -> null
        }
    }
}

fun Wallet.getFirstToken(): Token? {
    return getTokens().toList().getOrNull(0)
}