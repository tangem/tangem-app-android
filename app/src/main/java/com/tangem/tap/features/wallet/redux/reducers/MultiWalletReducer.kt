package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.toFiatString
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.features.wallet.models.removeUnknownTransactions
import com.tangem.tap.features.wallet.models.toPendingTransactions
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.features.wallet.ui.TokenData
import com.tangem.tap.store

class MultiWalletReducer {
    fun reduce(action: WalletAction.MultiWallet, state: WalletState): WalletState {
        return when (action) {
            is WalletAction.MultiWallet.AddWalletManagers -> {
                state.addWalletManagers(action.walletManagers)
            }
            is WalletAction.MultiWallet.AddBlockchains -> {
                val wallets = action.blockchains.map { blockchain ->
                    val wallet = state.getWalletManager(blockchain)?.wallet
                    val cardToken = if (!state.isMultiwalletAllowed) {
                        wallet?.getFirstToken()?.symbol?.let { TokenData("", tokenSymbol = it) }
                    } else {
                        null
                    }
                    WalletData(
                            currencyData = BalanceWidgetData(
                                    BalanceStatus.Loading,
                                    blockchain.fullName,
                                    currencySymbol = blockchain.currency,
                                    token = cardToken
                            ),
                            walletAddresses = createAddressList(wallet),
                            mainButton = WalletMainButton.SendButton(false),
                            currency = Currency.Blockchain(blockchain),
                            tradeCryptoState = TradeCryptoState(
                                sellingAllowed = state.tradeCryptoAllowed.sellingAllowed &&
                                        state.tradeCryptoAllowed.availableToSell.contains(blockchain.currency),
                                buyingAllowed = state.tradeCryptoAllowed.buyingAllowed
                            )
                    )
                }

                val selectedWallet = if (!state.isMultiwalletAllowed) {
                    wallets[0].currency
                } else {
                    state.selectedWallet
                }
                state.copy(
                        wallets = wallets,
                        selectedWallet = selectedWallet
                )
            }
            is WalletAction.MultiWallet.AddBlockchain -> {
                val wallet = state.getWalletManager(action.blockchain)?.wallet
                val walletData = WalletData(
                        currencyData = BalanceWidgetData(
                                BalanceStatus.Loading,
                                action.blockchain.fullName,
                                currencySymbol = action.blockchain.currency,
                        ),
                        walletAddresses = createAddressList(wallet),
                        mainButton = WalletMainButton.SendButton(false),
                        currency = Currency.Blockchain(action.blockchain),
                        tradeCryptoState = TradeCryptoState(
                            sellingAllowed = state.tradeCryptoAllowed.sellingAllowed &&
                                    state.tradeCryptoAllowed.availableToSell.contains(action.blockchain.currency),
                            buyingAllowed = state.tradeCryptoAllowed.buyingAllowed
                        )
                )
                val newState = state.copy(wallets = state.replaceWalletInWallets(walletData))
                if (wallet != null && wallet.amounts[AmountType.Coin]?.value != null) {
                    OnWalletLoadedReducer().reduce(wallet, newState)
                } else {
                    newState
                }
            }
            is WalletAction.MultiWallet.AddTokens -> {
                val wallets = action.tokens.mapNotNull { token -> token.toWallet(state) }
                state.copy(wallets = state.replaceSomeWallets(wallets))
            }
            is WalletAction.MultiWallet.AddToken -> {
                val walletData = action.token.toWallet(state) ?: return state
                state.copy(wallets = state.replaceWalletInWallets(walletData))
            }
            is WalletAction.MultiWallet.TokenLoaded -> {
                val pendingTransactions = state.getWalletManager(action.token)
                        ?.wallet?.let { wallet ->
                            wallet.recentTransactions.toPendingTransactions(wallet.address)
                        } ?: emptyList()

                val sendButtonEnabled = action.amount.value?.isZero() == false && pendingTransactions.isEmpty()
                val tokenPendingTransactions = pendingTransactions
                        .filter { it.currency ==  action.amount.currencySymbol }
                val tokenBalanceStatus = when {
                    tokenPendingTransactions.isNotEmpty() -> BalanceStatus.TransactionInProgress
                    pendingTransactions.isNotEmpty() -> BalanceStatus.SameCurrencyTransactionInProgress
                    else -> BalanceStatus.VerifiedOnline
                }
                val tokenWalletData = state.getWalletData(action.token)
                val newTokenWalletData = tokenWalletData?.copy(
                        currencyData = tokenWalletData.currencyData.copy(
                                status = tokenBalanceStatus,
                                amount = action.amount.value?.toFormattedCurrencyString(
                                        action.amount.decimals, action.amount.currencySymbol
                                ),
                                fiatAmountFormatted = tokenWalletData.fiatRate?.let {
                                    action.amount.value
                                            ?.toFiatString(it, store.state.globalState.appCurrency)
                                }
                        ),
                        pendingTransactions = pendingTransactions.removeUnknownTransactions(),
                        mainButton = WalletMainButton.SendButton(sendButtonEnabled)
                )
                val wallets = state.replaceWalletInWallets(newTokenWalletData)
                state.copy(wallets = wallets)
            }
            is WalletAction.MultiWallet.SetIsMultiwalletAllowed ->
                state.copy(isMultiwalletAllowed = action.isMultiwalletAllowed)

            is WalletAction.MultiWallet.SelectWallet ->
                state.copy(selectedWallet = action.walletData?.currency)

            is WalletAction.MultiWallet.RemoveWallet -> {
                val wallets = state.wallets.filterNot {
                    it.currency == action.walletData.currency
                }
                if (action.walletData.currency is Currency.Blockchain) {
                    state.copy(
                        wallets = wallets,
                        walletManagers = state.walletManagers.filterNot {
                            it.wallet.blockchain == action.walletData.currency.blockchain
                        }
                    )
                } else {
                    state.copy(wallets = wallets)
                }


            }
            is WalletAction.MultiWallet.SetPrimaryBlockchain ->
                state.copy(primaryBlockchain = action.blockchain)

            is WalletAction.MultiWallet.SetPrimaryToken ->
                state.copy(primaryToken = action.token)
            is WalletAction.MultiWallet.FindTokensInUse -> state
            is WalletAction.MultiWallet.FindBlockchainsInUse -> state
            is WalletAction.MultiWallet.SaveCurrencies -> state
        }
    }
}

fun Token.toWallet(state: WalletState): WalletData? {
    if (!state.isMultiwalletAllowed) return null
    if (state.currencies.any { it is Currency.Token && it.token == this }) {
        return null
    }

    val walletManager = state.getWalletManager(this)?.wallet
    val walletAddresses = createAddressList(walletManager)

    return WalletData(
        currencyData = BalanceWidgetData(
            BalanceStatus.Loading,
            currency = this.name,
            currencySymbol = this.symbol
        ),
        walletAddresses = walletAddresses,
        mainButton = WalletMainButton.SendButton(false),
        currency = Currency.Token(this),
        tradeCryptoState = TradeCryptoState(
            sellingAllowed = state.tradeCryptoAllowed.sellingAllowed &&
                    state.tradeCryptoAllowed.availableToSell.contains(this.symbol),
            buyingAllowed = state.tradeCryptoAllowed.buyingAllowed
        )
    )
}