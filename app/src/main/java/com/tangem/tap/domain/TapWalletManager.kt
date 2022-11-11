package com.tangem.tap.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.common.services.Result
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.ThrottlerWithValues
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.extensions.isMultiwalletAllowed
import com.tangem.tap.domain.extensions.makePrimaryWalletManager
import com.tangem.tap.domain.extensions.makeWalletManagersForApp
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.wallet.models.toBlockchainNetworks
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.store
import com.tangem.tap.userTokensRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TapWalletManager {
    val walletManagerFactory: WalletManagerFactory
        by lazy { WalletManagerFactory(blockchainSdkConfig) }
    val rates: RatesRepository = RatesRepository()

    private val blockchainSdkConfig by lazy {
        store.state.globalState.configManager?.config?.blockchainSdkConfig ?: BlockchainSdkConfig()
    }

    private val walletManagersThrottler =
        ThrottlerWithValues<BlockchainNetwork, Result<Wallet>>(10000)

    suspend fun loadWalletData(walletManager: WalletManager) {
        val blockchainNetwork = BlockchainNetwork.fromWalletManager(walletManager)
        val result = if (walletManagersThrottler.isStillThrottled(blockchainNetwork)) {
            walletManagersThrottler.geValue(blockchainNetwork)!!
        } else {
            val safeUpdateResult = walletManager.safeUpdate()
            walletManagersThrottler.updateThrottlingTo(blockchainNetwork)
            walletManagersThrottler.setValue(blockchainNetwork, safeUpdateResult)
            safeUpdateResult
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
                                (result.error as TapError.WalletManager.NoAccountError).customMessage,
                            ),
                        )
                    }
                    else -> {
                        dispatchOnMain(
                            WalletAction.LoadWallet.Failure(
                                walletManager.wallet,
                                result.error.localizedMessage,
                            ),
                        )
                    }
                }
            }
        }
    }

    suspend fun onCardScanned(data: ScanResponse) {
        walletManagersThrottler.clear()
        store.state.globalState.feedbackManager?.infoHolder?.setCardInfo(data)
        updateConfigManager(data)

        withMainContext {
            store.dispatch(WalletAction.ResetState(data.card.cardId))
            store.dispatch(WalletConnectAction.ResetState)
            store.dispatch(GlobalAction.SaveScanNoteResponse(data))
            store.dispatch(WalletAction.SetIfTestnetCard(data.card.isTestCard))
            store.dispatch(WalletAction.MultiWallet.SetIsMultiwalletAllowed(data.card.isMultiwalletAllowed))
            store.dispatch(WalletConnectAction.RestoreSessions(data))
            store.dispatch(
                WalletAction.MultiWallet.ShowWalletBackupWarning(
                    show = data.card.settings.isBackupAllowed
                        && data.card.backupStatus == CardDTO.BackupStatus.NoBackup,
                ),
            )
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

        if (data.card.isMultiwalletAllowed) {
            loadMultiWalletData(data)
        } else {
            loadSingleWalletData(data)
        }

        dispatchOnMain(WalletAction.LoadWallet())
    }

    private suspend fun loadMultiWalletData(scanResponse: ScanResponse) {
        loadUserCurrencies(scanResponse, walletManagerFactory)
    }

    private fun checkIfDerivationsAreMissing(blockchainNetworks: List<BlockchainNetwork>, scanResponse: ScanResponse) {
        blockchainNetworks.map {
            if (it.tokens.isNotEmpty()) {
                WalletAction.MultiWallet.AddTokens(it.tokens, it, false)
            }
        }
        val missingDerivations = blockchainNetworks
            .filter {
                it.derivationPath != null && !scanResponse.hasDerivation(it.blockchain, it.derivationPath)
            }
        if (missingDerivations.isNotEmpty()) {
            store.dispatch(WalletAction.MultiWallet.AddMissingDerivations(missingDerivations))
        }
    }

    private suspend fun loadSingleWalletData(data: ScanResponse) {
        val blockchain = data.getBlockchain()
        val primaryWalletManager = walletManagerFactory.makePrimaryWalletManager(data)

        if (blockchain != Blockchain.Unknown && primaryWalletManager != null) {
            val primaryToken = data.getPrimaryToken()

            dispatchOnMain(WalletAction.MultiWallet.SetPrimaryBlockchain(blockchain))
            if (primaryToken != null) {
                primaryWalletManager.addToken(primaryToken)
                dispatchOnMain(WalletAction.MultiWallet.SetPrimaryToken(primaryToken))
            }
            dispatchOnMain(
                WalletAction.MultiWallet.AddBlockchains(
                    blockchains = listOf(BlockchainNetwork.fromWalletManager(primaryWalletManager)),
                    walletManagers = listOf(primaryWalletManager),
                    save = false,
                ),
                WalletAction.LoadFiatRate(),
            )
        }
    }

    private suspend fun loadUserCurrencies(scanResponse: ScanResponse, walletManagerFactory: WalletManagerFactory) {
        val userTokens = userTokensRepository.getUserTokens(scanResponse.card)
        withMainContext {
            val blockchainNetworks = userTokens.toBlockchainNetworks()
            val walletManagers = walletManagerFactory.makeWalletManagersForApp(scanResponse, userTokens)
            store.dispatch(
                WalletAction.MultiWallet.AddBlockchains(
                    blockchains = blockchainNetworks,
                    walletManagers = walletManagers,
                    save = false,
                ),
            )

            blockchainNetworks.filter { it.tokens.isNotEmpty() }
                .map {
                    store.dispatch(
                        WalletAction.MultiWallet.AddTokens(
                            tokens = it.tokens,
                            blockchain = it,
                            save = false,
                        ),
                    )
                }
            checkIfDerivationsAreMissing(blockchainNetworks, scanResponse)
            store.dispatch(WalletAction.LoadFiatRate(coinsList = userTokens))
        }
    }

    suspend fun reloadData(data: ScanResponse) {
        loadUserCurrencies(data, walletManagerFactory)
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
