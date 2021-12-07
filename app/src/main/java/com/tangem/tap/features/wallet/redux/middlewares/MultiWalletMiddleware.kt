package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.extensions.makeWalletManagersForApp
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MultiWalletMiddleware {
    fun handle(
        action: WalletAction.MultiWallet, walletState: WalletState?, globalState: GlobalState?,
    ) {
        val globalState = globalState ?: return
        val tapWalletManager = globalState.tapWalletManager

        when (action) {
            is WalletAction.MultiWallet.AddWalletManagers -> {
                globalState.feedbackManager?.infoHolder?.setWalletsInfo(action.walletManagers)
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
                addToken(action.token, walletState, globalState)
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
                store.dispatch(WalletAction.LoadFiatRate(currency = Currency.Blockchain(action.blockchain)))
                store.dispatch(WalletAction.LoadWallet(
                    moonpayStatus = globalState.moonpayStatus,
                    blockchain = action.blockchain
                )
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

                val cardFirmware = scanResponse.card.firmwareVersion
                val blockchains = currenciesRepository.getBlockchains(cardFirmware)
                    .filterNot { walletState?.blockchains?.contains(it) == true }
                val walletManagers =
                    tapWalletManager.walletManagerFactory.makeWalletManagersForApp(scanResponse, blockchains)

                scope.launch {
                    walletManagers.map { walletManager ->
                        async(Dispatchers.IO) {
                            try {
                                walletManager.update()
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
                            } catch (exception: Exception) {
                            }
                        }
                    }
                }
            }
            is WalletAction.MultiWallet.FindTokensInUse -> {
                val scanResponse = globalState.scanResponse ?: return

                val walletFactory = tapWalletManager.walletManagerFactory
                val card = scanResponse.card
                val walletManager = walletState?.getWalletManager(Blockchain.Ethereum)
                    ?: walletFactory.makeWalletManagerForApp(scanResponse, Blockchain.Ethereum)

                val tokenFinder = walletManager as TokenFinder
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

    private fun addToken(token: Token, walletState: WalletState?, globalState: GlobalState?) {
        val scanResponse = globalState?.scanResponse ?: return

        val walletManager = walletState?.getWalletManager(token)
            ?: globalState.tapWalletManager.walletManagerFactory.makeWalletManagerForApp(
                scanResponse,
                token.blockchain
            )?.also { walletManager ->
                store.dispatch(WalletAction.MultiWallet.AddWalletManagers(walletManager))
                store.dispatch(WalletAction.MultiWallet.AddBlockchain(walletManager.wallet.blockchain))
            }

        store.dispatch(WalletAction.LoadFiatRate(currency = Currency.Token(token)))

        scope.launch {
            when (val result = walletManager?.addToken(token)) {
                is Result.Success -> {
                    store.dispatchOnMain(
                        WalletAction.MultiWallet.TokenLoaded(amount = result.data, token = token)
                    )
                }
            }
        }
    }

    private fun addTokens(
        tokens: List<Token>,
        walletState: WalletState?,
        globalState: GlobalState?,
    ) {
        val scanResponse = globalState?.scanResponse ?: return

        val tokensWithManagers = tokens.map { token ->
            val walletManager = walletState?.getWalletManager(token)
                ?: globalState.tapWalletManager.walletManagerFactory.makeWalletManagerForApp(
                    scanResponse,
                    token.blockchain
                )?.also { walletManager ->
                    store.dispatch(WalletAction.MultiWallet.AddWalletManagers(walletManager))
                    store.dispatch(WalletAction.MultiWallet.AddBlockchain(walletManager.wallet.blockchain))
                }
            store.dispatch(
                WalletAction.LoadFiatRate(currency = Currency.Token(token))
            )
            TokenWithManager(token, walletManager)
        }
        scope.launch {
            tokensWithManagers.forEach {
                when (val result = it.walletManager?.addToken(it.token)) {
                    is Result.Success -> {
                        store.dispatchOnMain(
                            WalletAction.MultiWallet.TokenLoaded(
                                amount = result.data, token = it.token
                            )
                        )
                    }
                }
            }
        }
    }

    private data class TokenWithManager(val token: Token, val walletManager: WalletManager?)
}
