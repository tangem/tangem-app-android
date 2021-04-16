package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TokenFinder
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.currenciesRepository
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
        when (action) {
            is WalletAction.MultiWallet.AddWalletManagers -> {
                val wallets = action.walletManagers.map { it.wallet }
                store.state.globalState.feedbackManager?.infoHolder?.setWalletsInfo(wallets)
            }
            is WalletAction.MultiWallet.SelectWallet -> {
                if (action.walletData != null) {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.WalletDetails))
                }
            }
            is WalletAction.MultiWallet.AddToken -> {
                globalState?.scanNoteResponse?.card?.cardId?.let {
                    currenciesRepository.saveAddedToken(it, action.token)
                }
                addToken(action.token, walletState)
            }
            is WalletAction.MultiWallet.AddTokens -> {
                action.tokens.map { addToken(it, walletState) }
            }
            is WalletAction.MultiWallet.AddBlockchain -> {
                globalState?.scanNoteResponse?.card?.let { card ->
                    currenciesRepository.saveAddedBlockchain(card.cardId, action.blockchain)
                    globalState.tapWalletManager.walletManagerFactory
                            .makeWalletManager(card, action.blockchain)?.let {
                                store.dispatch(WalletAction.MultiWallet.AddWalletManagers(it))
                            }
                }
                store.dispatch(WalletAction.LoadFiatRate(currency = action.blockchain.currency))
                store.dispatch(WalletAction.LoadWallet(currency = action.blockchain.currency))
            }
            is WalletAction.MultiWallet.SaveCurrencies -> {
                val cardId = globalState?.scanNoteResponse?.card?.cardId
                cardId?.let { currenciesRepository.saveCardCurrencies(it, action.cardCurrencies) }
            }
            is WalletAction.MultiWallet.RemoveWallet -> {
                val cardId = globalState?.scanNoteResponse?.card?.cardId
                if (action.walletData.token != null) {
                    cardId?.let { currenciesRepository.removeToken(it, action.walletData.token) }
                } else if (action.walletData.blockchain != null) {
                    cardId?.let { currenciesRepository.removeBlockchain(it, action.walletData.blockchain) }
                }
            }
            is WalletAction.MultiWallet.FindBlockchainsInUse -> {
                val blockchains = currenciesRepository.getBlockchains()
                        .filterNot { walletState?.blockchains?.contains(it) == true }
                val walletManagers = action.factory.makeWalletManagers(action.card, blockchains)

                scope.launch {
                    walletManagers.map { walletManager ->
                        async(Dispatchers.IO) {
                            try {
                                walletManager.update()
                                val wallet = walletManager.wallet
                                val coinAmount = wallet.amounts[AmountType.Coin]?.value
                                if (coinAmount != null && !coinAmount.isZero()) {
                                    if (walletState?.getWalletData(wallet.blockchain.currency) == null) {
                                        scope.launch(Dispatchers.Main) {
                                            store.dispatch(WalletAction.MultiWallet.AddWalletManagers(
                                                    listOfNotNull(walletManager)))
                                            store.dispatch(WalletAction.MultiWallet.AddBlockchain(
                                                    wallet.blockchain
                                            ))
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
                val walletManager = walletState?.getWalletManager(Blockchain.Ethereum.currency)
                        ?: return
                val tokenFinder = walletManager as TokenFinder
                scope.launch {
                    val result = tokenFinder.findTokens()
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is Result.Success -> {
                                if (result.data.isNotEmpty()) {
                                    store.dispatch(WalletAction.MultiWallet.AddTokens(
                                            walletManager.presetTokens.toList()
                                    ))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addToken(token: Token, walletState: WalletState?) {
        val walletManager = walletState?.getWalletManager(token.symbol)
        scope.launch {
            val result = walletManager?.addToken(token)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        store.dispatch(WalletAction.LoadFiatRate(currency = token.symbol))
                        store.dispatch(WalletAction.MultiWallet.TokenLoaded(result.data))
                    }
                }
            }
        }
    }
}
