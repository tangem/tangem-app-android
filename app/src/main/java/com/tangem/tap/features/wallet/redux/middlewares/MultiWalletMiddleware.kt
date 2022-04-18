package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.blockchains.solana.SolanaWalletManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.extensions.makeWalletManagersForApp
import com.tangem.tap.domain.tokens.BlockchainNetwork
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class MultiWalletMiddleware {
    fun handle(
        action: WalletAction.MultiWallet, walletState: WalletState?, globalState: GlobalState?,
    ) {
        val globalState = globalState ?: return
        val tapWalletManager = globalState.tapWalletManager

        when (action) {
            is WalletAction.MultiWallet.AddBlockchains -> {
                handleAddingWalletManagers(globalState, action.walletManagers)
            }
            is WalletAction.MultiWallet.SelectWallet -> {
                if (action.walletData != null) {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.WalletDetails))
                }
            }
            is WalletAction.MultiWallet.AddToken -> {
                addTokens(listOf(action.token), action.blockchain, walletState, globalState)
            }
            is WalletAction.MultiWallet.AddTokens -> {
                addTokens(action.tokens, action.blockchain, walletState, globalState)
            }
            is WalletAction.MultiWallet.AddBlockchain -> {
                action.walletManager?.let {
                    handleAddingWalletManagers(globalState, listOf(action.walletManager))
                }

                globalState.scanResponse?.let {
                    currenciesRepository.saveUpdatedCurrency(
                        cardId = it.card.cardId,
                        blockchainNetwork = action.blockchain
                    )
                }
                store.dispatch(
                    WalletAction.LoadFiatRate(
                        currencyList = listOf(
                            Currency.Blockchain(
                                action.blockchain.blockchain,
                                action.blockchain.derivationPath
                            )
                        )
                    )
                )
                store.dispatch(
                    WalletAction.LoadWallet(action.blockchain, action.walletManager)
                )
            }
            is WalletAction.MultiWallet.SaveCurrencies -> {
                globalState.scanResponse?.card?.cardId?.let {
                    currenciesRepository.saveCurrencies(it, action.blockchainNetworks)
                }
            }
            is WalletAction.MultiWallet.RemoveWallet -> {
                val cardId = globalState.scanResponse?.card?.cardId
                when (val currency = action.walletData.currency) {
                    is Currency.Blockchain -> {
                        cardId?.let {
                            currenciesRepository.removeBlockchain(
                                cardId = it,
                                blockchainNetwork = BlockchainNetwork(
                                    currency.blockchain, currency.derivationPath,
                                    emptyList()
                                )
                            )
                        }
                    }
                    is Currency.Token -> {
                        val walletManager = walletState?.getWalletManager(currency)
                        if (walletManager != null) {
                            walletManager.removeToken(currency.token)
                            cardId?.let {
                                currenciesRepository.removeToken(
                                    cardId = it,
                                    token = currency.token,
                                    blockchainNetwork = BlockchainNetwork.fromWalletManager(walletManager)
                                )
                            }
                        }
                    }
                }
            }
            is WalletAction.MultiWallet.FindBlockchainsInUse -> {
                val scanResponse = globalState.scanResponse ?: return
                if (scanResponse.supportsHdWallet()) return

                val cardFirmware = scanResponse.card.firmwareVersion
                val blockchains = currenciesRepository.getBlockchains(cardFirmware)
                    .filterNot { walletState?.blockchains?.contains(it) == true }
                    .map { BlockchainNetwork(it, null, emptyList()) }
                val walletManagers =
                    tapWalletManager.walletManagerFactory.makeWalletManagersForApp(
                        scanResponse,
                        blockchains
                    )

                scope.launch {
                    walletManagers.map { walletManager ->
                        async(Dispatchers.IO) {
                            walletManager.safeUpdate()
                            val wallet = walletManager.wallet
                            val coinAmount = wallet.amounts[AmountType.Coin]?.value
                            if (coinAmount != null && !coinAmount.isZero()) {
                                scope.launch(Dispatchers.Main) {
                                    val blockchainNetwork = BlockchainNetwork.fromWalletManager(walletManager)
                                    if (walletState?.getWalletData(blockchainNetwork) == null) {
                                        store.dispatch(
                                            WalletAction.MultiWallet.AddBlockchain(
                                                blockchainNetwork, walletManager
                                            )
                                        )
                                        store.dispatch(
                                            WalletAction.LoadWallet.Success(
                                                wallet = wallet,
                                                blockchain = blockchainNetwork
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is WalletAction.MultiWallet.FindTokensInUse -> {
                val scanResponse = globalState.scanResponse ?: return
                if (scanResponse.supportsHdWallet()) return

                val walletFactory = tapWalletManager.walletManagerFactory
                val card = scanResponse.card

                val walletManager = walletState?.getWalletManager(
                    Currency.Blockchain(Blockchain.Ethereum, null)
                )
                    ?: walletFactory.makeWalletManagerForApp(
                        scanResponse,
                        Currency.Blockchain(Blockchain.Ethereum, null)
                    )

                val tokenFinder = walletManager as? TokenFinder ?: return
                scope.launch {
                    val result = tokenFinder.findTokens()

                    withContext(Dispatchers.Main) {
                        when (result) {
                            is Result.Success -> {
                                if (result.data.isNotEmpty()) {
                                    val blockchainNetwork = BlockchainNetwork(
                                        walletManager.wallet.blockchain,
                                        walletManager.wallet.publicKey.derivationPath?.rawPath,
                                        walletManager.cardTokens.toList()
                                    )
                                    currenciesRepository.saveUpdatedCurrency(
                                        card.cardId,
                                        blockchainNetwork
                                    )
                                    store.dispatch(
                                        WalletAction.MultiWallet.AddBlockchain(
                                            blockchainNetwork,
                                            walletManager
                                        )
                                    )
                                    store.dispatch(
                                        WalletAction.MultiWallet.AddTokens(
                                            walletManager.cardTokens.toList(),
                                            blockchainNetwork
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addDummyBalances(walletManagers: List<WalletManager>) {
        walletManagers.forEach {
            if (it.wallet.fundsAvailable(AmountType.Coin) == BigDecimal.ZERO) {
                DemoHelper.injectDemoBalance(it)
            }
        }
    }

    private fun handleAddingWalletManagers(
        globalState: GlobalState,
        walletManagers: List<WalletManager>
    ) {
        globalState.feedbackManager?.infoHolder?.setWalletsInfo(walletManagers)
        if (globalState.scanResponse?.isDemoCard() == true) {
            addDummyBalances(walletManagers)
        }
    }

    private fun addTokens(
        tokens: List<Token>, blockchainNetwork: BlockchainNetwork,
        walletState: WalletState?, globalState: GlobalState?
    ) {
        if (tokens.isEmpty()) return
        val scanResponse = globalState?.scanResponse ?: return
        val wmFactory = globalState.tapWalletManager.walletManagerFactory

        val walletManager = walletState?.getWalletManager(blockchainNetwork)
            ?: wmFactory.makeWalletManagerForApp(scanResponse, blockchainNetwork)?.also {
                store.dispatch(WalletAction.MultiWallet.AddBlockchain(blockchainNetwork, it))
            } ?: return

        store.dispatch(WalletAction.LoadFiatRate(currencyList = tokens.map { token ->
            Currency.Token(
                token, blockchainNetwork.blockchain, blockchainNetwork.derivationPath
            )
        }))
        if (tokens.isNotEmpty()) walletManager.addTokens(tokens)

        currenciesRepository.saveUpdatedCurrency(
            cardId = scanResponse.card.cardId,
            blockchainNetwork = BlockchainNetwork.fromWalletManager(walletManager)
        )
        scope.launch {
            when (val result = walletManager.safeUpdate()) {
                is com.tangem.common.services.Result.Success -> {
                    val wallet = result.data
                    wallet.getTokens()
                        .filter { tokens.contains(it) }
                        .mapNotNull { token ->
                            wallet.getTokenAmount(token)?.let { Pair(token, it) }
                        }
                        .forEach {
                            withContext(Dispatchers.Main) {
                                store.dispatch(
                                    WalletAction.MultiWallet.TokenLoaded(
                                        it.second,
                                        it.first,
                                        blockchainNetwork
                                    )
                                )
                            }
                        }
                }
                is com.tangem.common.services.Result.Failure -> {}
            }
        }
    }
}