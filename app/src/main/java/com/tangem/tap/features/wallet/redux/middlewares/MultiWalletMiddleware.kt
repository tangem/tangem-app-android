package com.tangem.tap.features.wallet.redux.middlewares

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
            is WalletAction.MultiWallet.AddWalletManagers -> {
                globalState.feedbackManager?.infoHolder?.setWalletsInfo(action.walletManagers)
                if (globalState.scanResponse?.isDemoCard() == true) {
                    addDummyBalances(action.walletManagers)
                }
            }
            is WalletAction.MultiWallet.SelectWallet -> {
                if (action.walletData != null) {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.WalletDetails))
                }
            }
            is WalletAction.MultiWallet.AddToken -> {
                globalState.scanResponse?.card?.cardId?.let {
                    currenciesRepository.saveAddedToken(it, action.token)
                }
                addTokens(listOf(action.token), walletState, globalState)
            }
            is WalletAction.MultiWallet.AddTokens -> {
                addTokens(action.tokens, walletState, globalState)
            }
            is WalletAction.MultiWallet.AddBlockchain -> {
                globalState.scanResponse?.let {
                    currenciesRepository.saveAddedBlockchain(it.card.cardId, action.blockchain)
                    if (walletState?.blockchains?.contains(action.blockchain) != true) {
                        tapWalletManager.walletManagerFactory
                            .makeWalletManagerForApp(it, action.blockchain)?.let { walletManager ->
                                store.dispatch(WalletAction.MultiWallet.AddWalletManagers(walletManager))
                            }
                    }
                }
                store.dispatch(WalletAction.LoadFiatRate(
                    currencyList = listOf(Currency.Blockchain(action.blockchain)))
                )
                store.dispatch(WalletAction.LoadWallet(action.blockchain)
                )
            }
            is WalletAction.MultiWallet.SaveCurrencies -> {
                globalState.scanResponse?.card?.cardId?.let {
                    currenciesRepository.saveCardCurrencies(it, action.cardCurrencies)
                }
            }
            is WalletAction.MultiWallet.RemoveWallet -> {
                val cardId = globalState.scanResponse?.card?.cardId
                when (val currency = action.walletData.currency) {
                    is Currency.Blockchain -> {
                        cardId?.let {
                            currenciesRepository.removeBlockchain(it, currency.blockchain)
                        }
                    }
                    is Currency.Token -> {
                        walletState?.getWalletManager(currency.token)
                            ?.removeToken(currency.token)
                        cardId?.let { currenciesRepository.removeToken(it, currency.token) }
                    }
                }
            }
            is WalletAction.MultiWallet.FindBlockchainsInUse -> {
                val scanResponse = globalState.scanResponse ?: return
                if (scanResponse.supportsHdWallet()) return

                val cardFirmware = scanResponse.card.firmwareVersion
                val blockchains = currenciesRepository.getBlockchains(cardFirmware)
                    .filterNot { walletState?.blockchains?.contains(it) == true }
                val walletManagers =
                    tapWalletManager.walletManagerFactory.makeWalletManagersForApp(scanResponse, blockchains)

                scope.launch {
                    walletManagers.map { walletManager ->
                        async(Dispatchers.IO) {
                            walletManager.safeUpdate()
                            val wallet = walletManager.wallet
                            val coinAmount = wallet.amounts[AmountType.Coin]?.value
                            if (coinAmount != null && !coinAmount.isZero()) {
                                scope.launch(Dispatchers.Main) {
                                    if (walletState?.getWalletData(wallet.blockchain) == null) {
                                        store.dispatch(
                                            WalletAction.MultiWallet.AddWalletManagers(
                                                listOfNotNull(walletManager)
                                            )
                                        )
                                        store.dispatch(
                                            WalletAction.MultiWallet.AddBlockchain(
                                                wallet.blockchain
                                            )
                                        )
                                        store.dispatch(WalletAction.LoadWallet.Success(wallet))
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
                val walletManager = walletState?.getWalletManager(Blockchain.Ethereum)
                    ?: walletFactory.makeWalletManagerForApp(scanResponse, Blockchain.Ethereum)

                val tokenFinder = walletManager as? TokenFinder ?: return
                scope.launch {
                    val result = tokenFinder.findTokens()
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is Result.Success -> {
                                if (result.data.isNotEmpty()) {
                                    currenciesRepository.saveAddedTokens(card.cardId, result.data)
                                    store.dispatch(
                                        WalletAction.MultiWallet.AddWalletManagers(
                                            walletManager
                                        )
                                    )
                                    store.dispatch(
                                        WalletAction.MultiWallet.AddBlockchain(
                                            walletManager.wallet.blockchain
                                        )
                                    )
                                    store.dispatch(
                                        WalletAction.MultiWallet.AddTokens(
                                            walletManager.cardTokens.toList()
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

    private fun addTokens(tokens: List<Token>, walletState: WalletState?, globalState: GlobalState?) {
        val scanResponse = globalState?.scanResponse ?: return
        val wmFactory = globalState.tapWalletManager.walletManagerFactory

        val groupedTokens = tokens.groupBy { it.blockchain }
        val walletManagers = groupedTokens.mapNotNull { entry ->
            val blockchain = entry.key
            val tokensList = entry.value
            val walletManager = walletState?.getWalletManager(blockchain)
                ?: wmFactory.makeWalletManagerForApp(scanResponse, blockchain)?.also {
                    store.dispatch(WalletAction.MultiWallet.AddWalletManagers(it))
                    store.dispatch(WalletAction.MultiWallet.AddBlockchain(blockchain))
                }
            store.dispatch(WalletAction.LoadFiatRate(currencyList = tokensList.map { Currency.Token(it) }))
            walletManager?.apply {
                scope.launch { async { addTokens(tokensList) }.await() }
            }
        }
        scope.launch {
            walletManagers.forEach { walletManager ->
                when (val result = walletManager.safeUpdate()) {
                    is com.tangem.common.services.Result.Success -> {
                        val wallet = result.data
                        wallet.getTokens()
                            .filter { tokens.contains(it) }
                            .mapNotNull { token -> wallet.getTokenAmount(token)?.let { Pair(token, it) } }
                            .forEach {
                                withContext(Dispatchers.Main) {
                                    store.dispatch(WalletAction.MultiWallet.TokenLoaded(it.second, it.first))
                                }
                            }
                    }
                    is com.tangem.common.services.Result.Failure -> {}
                }
            }
        }
    }
}