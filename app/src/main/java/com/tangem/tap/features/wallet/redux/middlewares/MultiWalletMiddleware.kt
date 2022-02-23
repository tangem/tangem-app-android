package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.blockchains.solana.RentProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.extensions.makeWalletManagersForApp
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.tap.features.wallet.models.getPendingTransactions
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
                action.walletManagers.forEach { checkForRentWarning(it) }
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
                if (scanResponse.supportsHdWallet()) return

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
                    store.dispatchOnMain(WalletAction.MultiWallet.TokenLoaded(result.data, token))
                }
                is Result.Failure -> {
                    when (val result = walletManager.safeUpdate()) {
                        is com.tangem.common.services.Result.Success -> {
                            val tokenAmount = result.data.getTokenAmount(token) ?: return@launch
                            store.dispatchOnMain(WalletAction.MultiWallet.TokenLoaded(tokenAmount, token))
                        }
                        is com.tangem.common.services.Result.Failure -> {
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
                is Result.Success -> {
                    fun isNeedToShowWarning(balance: BigDecimal, rentExempt: BigDecimal): Boolean = balance >= rentExempt

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
                is Result.Failure -> {}
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
                        store.dispatchOnMain(WalletAction.MultiWallet.TokenLoaded(result.data, it.token))
                    }
                    is Result.Failure -> {
                        when (val result = it.walletManager.safeUpdate()) {
                            is com.tangem.common.services.Result.Success -> {
                                val tokenAmount = result.data.getTokenAmount(it.token) ?: return@launch
                                store.dispatchOnMain(WalletAction.MultiWallet.TokenLoaded(tokenAmount, it.token))
                            }
                            is com.tangem.common.services.Result.Failure -> {
                            }
                        }
                    }
                }
            }
        }
    }

    private data class TokenWithManager(val token: Token, val walletManager: WalletManager?)
}