package com.tangem.tap.domain

import com.tangem.blockchain.blockchains.solana.RentProvider
import com.tangem.blockchain.common.*
import com.tangem.common.services.Result
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.TapWorkarounds.isStart2Coin
import com.tangem.tap.domain.TapWorkarounds.isTestCard
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.extensions.makePrimaryWalletManager
import com.tangem.tap.domain.extensions.makeWalletManagersForApp
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.tokens.CardCurrencies
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.tap.features.wallet.models.getPendingTransactions
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.network.coinmarketcap.CoinMarketCapService
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal


class TapWalletManager {
    val walletManagerFactory: WalletManagerFactory
        by lazy { WalletManagerFactory(blockchainSdkConfig) }

    private val coinMarketCapService = CoinMarketCapService()
    private val blockchainSdkConfig by lazy {
        store.state.globalState.configManager?.config?.blockchainSdkConfig ?: BlockchainSdkConfig()
    }

    private val throttlingDuration = 10000
    private val throttlingWalletManagers: MutableMap<Blockchain, Long> = mutableMapOf()

    private val throttlingFiatRateDuration = 60000
    private val throttlingFiatRate: MutableMap<Currency, Long> = mutableMapOf()

    suspend fun loadWalletData(walletManager: WalletManager) {
        val result = if (managerIsStillThrottled(walletManager)) {
            delay(200)
            Result.Success(walletManager.wallet)
        } else {
            val blockchain = walletManager.wallet.blockchain
            val now = System.currentTimeMillis()
            val throttledUpTo = throttlingWalletManagers[blockchain] ?: 0L
            if (throttledUpTo == 0L || throttledUpTo < now) {
                val newTime = now + throttlingDuration
                throttlingWalletManagers[blockchain] = newTime
            }
            walletManager.safeUpdate()
        }
        handleUpdateWalletResult(result, walletManager)
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
        val toUpdate = currencies.mapNotNull {
            if (fiatRateIsStillThrottled(it)) {
                null
            } else {
                val now = System.currentTimeMillis()
                val throttledUpTo = throttlingFiatRate[it] ?: 0L
                if (throttledUpTo == 0L || throttledUpTo < now) {
                    val newTime = now + throttlingFiatRateDuration
                    throttlingFiatRate[it] = newTime
                }
                it
            }
        }

        toUpdate.forEach {
            results.add(it to coinMarketCapService.getRate(it.currencySymbol, fiatCurrency))
        }
        handleFiatRatesResult(results)
    }

    suspend fun onCardScanned(data: ScanResponse) {
        store.state.globalState.feedbackManager?.infoHolder?.setCardInfo(data.card)
        updateConfigManager(data)

        withContext(Dispatchers.Main) {
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
            getActionIfUnknownBlockchainOrEmptyWallet(data)?.let {
                store.dispatch(it)
                return@withContext
            }

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
                    loadMultiWalletData(data, blockchain, primaryWalletManager)
                } else {
                    store.dispatch(WalletAction.MultiWallet.AddWalletManagers(primaryWalletManager))
                    store.dispatch(WalletAction.MultiWallet.AddBlockchains(listOf(blockchain)))
                }

            } else {
                if (data.card.isMultiwalletAllowed) {
                    loadMultiWalletData(data, blockchain, null)
                }
            }
            store.dispatch(WalletAction.LoadWallet())
            store.dispatch(WalletAction.LoadFiatRate())
        }
    }

    private fun loadMultiWalletData(
        scanResponse: ScanResponse, primaryBlockchain: Blockchain?, primaryWalletManager: WalletManager?
    ) {
        val primaryTokens = primaryWalletManager?.cardTokens?.toList() ?: emptyList()
        val savedCurrencies = currenciesRepository.loadCardCurrencies(scanResponse.card.cardId)

        if (savedCurrencies == null) {
            if (primaryBlockchain != null && primaryWalletManager != null) {
                store.dispatch(WalletAction.MultiWallet.SaveCurrencies(
                    CardCurrencies(
                        blockchains = listOf(primaryBlockchain), tokens = primaryTokens
                    )))
                store.dispatch(WalletAction.MultiWallet.AddWalletManagers(primaryWalletManager))
                store.dispatch(WalletAction.MultiWallet.AddBlockchains(listOf(primaryBlockchain)))
                store.dispatch(WalletAction.MultiWallet.AddTokens(primaryTokens.toList()))
            } else {
                val blockchains = listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
                store.dispatch(WalletAction.MultiWallet.SaveCurrencies(
                    CardCurrencies(blockchains = blockchains, tokens = emptyList())
                ))
                val walletManagers =
                    walletManagerFactory.makeWalletManagersForApp(scanResponse, blockchains.toList())
                store.dispatch(WalletAction.MultiWallet.AddWalletManagers(walletManagers))
                store.dispatch(WalletAction.MultiWallet.AddBlockchains(blockchains.toList()))
            }
            store.dispatch(WalletAction.MultiWallet.FindBlockchainsInUse)
            store.dispatch(WalletAction.MultiWallet.FindTokensInUse)
        } else {
            val blockchains = savedCurrencies.blockchains.toList()
            val walletManagers = if (
                primaryTokens.isNotEmpty() &&
                primaryWalletManager != null && primaryBlockchain != null
            ) {
                val blockchainsWithoutPrimary = blockchains.filterNot { it == primaryBlockchain }
                walletManagerFactory.makeWalletManagersForApp(scanResponse, blockchainsWithoutPrimary)
                    .plus(primaryWalletManager)
            } else {
                walletManagerFactory.makeWalletManagersForApp(scanResponse, blockchains)
            }

            store.dispatch(WalletAction.MultiWallet.AddWalletManagers(walletManagers))
            store.dispatch(WalletAction.MultiWallet.AddBlockchains(blockchains))
            store.dispatch(WalletAction.MultiWallet.AddTokens(savedCurrencies.tokens.toList()))
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

    private suspend fun handleUpdateWalletResult(result: Result<Wallet>, walletManager: WalletManager) {
        withContext(Dispatchers.Main) {
            when (result) {
                is Result.Success -> {
                    checkForRentWarning(walletManager)
                    store.dispatch(WalletAction.LoadWallet.Success(result.data))
                }
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

    private fun checkForRentWarning(walletManager: WalletManager) {
        val rentProvider = walletManager as? RentProvider ?: return

        scope.launch {
            when (val result = rentProvider.minimalBalanceForRentExemption()) {
                is com.tangem.blockchain.extensions.Result.Success -> {
                    fun isNeedToShowWarning(balance: BigDecimal, rentExempt: BigDecimal): Boolean = balance < rentExempt

                    val balance = walletManager.wallet.fundsAvailable(AmountType.Coin)
                    val outgoingTxs = walletManager.wallet.getPendingTransactions(PendingTransactionType.Outgoing)
                    val rentExempt = result.data
                    val show = if (outgoingTxs.isEmpty()) {
                        isNeedToShowWarning(balance, rentExempt)
                    } else {
                        val outgoingAmount = outgoingTxs.sumOf { it.amount ?: BigDecimal.ZERO }
                        val rest = balance.minus(outgoingAmount)
                        isNeedToShowWarning(rest, rentExempt)
                    }
                    if (!show) return@launch

                    val currency = walletManager.wallet.blockchain.currency
                    store.dispatchOnMain(WalletAction.SetWalletRent(
                        blockchain = walletManager.wallet.blockchain,
                        minRent = ("${rentProvider.rentAmount().stripZeroPlainString()} $currency"),
                        rentExempt = ("${rentExempt.stripZeroPlainString()} $currency")
                    ))
                }
                is com.tangem.blockchain.extensions.Result.Failure -> {}
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

    private fun managerIsStillThrottled(walletManager: WalletManager): Boolean {
        val inThrottlingUpTo = throttlingWalletManagers[walletManager.wallet.blockchain] ?: return false
        val diff = System.currentTimeMillis() - inThrottlingUpTo
        return diff < 0
    }

    private fun fiatRateIsStillThrottled(currency: Currency): Boolean {
        val inThrottlingUpTo = throttlingFiatRate[currency] ?: return false
        val diff = System.currentTimeMillis() - inThrottlingUpTo
        return diff < 0
    }
}

fun Wallet.getFirstToken(): Token? {
    return getTokens().toList().getOrNull(0)
}