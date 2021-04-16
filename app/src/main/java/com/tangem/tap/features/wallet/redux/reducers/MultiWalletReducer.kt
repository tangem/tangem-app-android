package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.AmountType
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
                state.copy(
                        walletManagers =   state.walletManagers + action.walletManagers
                )
            }
            is WalletAction.MultiWallet.AddBlockchains -> {
                val wallets = action.blockchains.map { blockchain ->
                    val wallet = state.getWalletManager(blockchain.currency)?.wallet
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
                            topUpState = TopUpState(allowed = false),
                            blockchain = blockchain
                    )
                }

                val selectedWallet = if (!state.isMultiwalletAllowed) {
                    wallets[0].currencyData.currencySymbol
                } else {
                    state.selectedWallet
                }
                state.copy(
                        wallets = wallets,
                        selectedWallet = selectedWallet
                )
            }
            is WalletAction.MultiWallet.AddBlockchain -> {
                val wallet = state.getWalletManager(action.blockchain.currency)?.wallet
                val walletData = WalletData(
                        currencyData = BalanceWidgetData(
                                BalanceStatus.Loading,
                                action.blockchain.fullName,
                                currencySymbol = action.blockchain.currency,
                        ),
                        walletAddresses = createAddressList(wallet),
                        mainButton = WalletMainButton.SendButton(false),
                        topUpState = TopUpState(allowed = false),
                        blockchain = action.blockchain
                )
                val newState = state.copy(wallets = state.replaceWalletInWallets(walletData))
                if (wallet != null && wallet.amounts[AmountType.Coin]?.value != null) {
                    OnWalletLoadedReducer().reduce(wallet, newState)
                } else {
                    newState
                }
            }
            is WalletAction.MultiWallet.AddTokens -> {
                if (!state.isMultiwalletAllowed) return state
                val wallets = action.tokens.map { token ->
                    WalletData(
                            currencyData = BalanceWidgetData(
                                    BalanceStatus.Loading,
                                    currency = token.name,
                                    currencySymbol = token.symbol
                            ),
                            walletAddresses = createAddressList(
                                    state.getWalletManagerForToken(token.symbol)?.wallet
                            ),
                            mainButton = WalletMainButton.SendButton(false),
                            topUpState = TopUpState(allowed = false),
                            token = token
                    )
                }
                state.copy(wallets = state.replaceSomeWallets(wallets))
            }
            is WalletAction.MultiWallet.AddToken -> {
                if (!state.isMultiwalletAllowed) return state

                val walletAddresses = createAddressList(
                        state.getWalletManagerForToken(action.token.symbol)?.wallet

                )
                val wallet = WalletData(
                        currencyData = BalanceWidgetData(
                                BalanceStatus.Loading,
                                currency = action.token.name,
                                currencySymbol = action.token.symbol
                        ),
                        walletAddresses = walletAddresses,
                        mainButton = WalletMainButton.SendButton(false),
                        topUpState = TopUpState(allowed = false),
                        token = action.token
                )
                val wallets = state.replaceWalletInWallets(wallet)
                state.copy(wallets = wallets)
            }
            is WalletAction.MultiWallet.TokenLoaded -> {
                val pendingTransactions = state.getWalletManagerForToken(action.amount.currencySymbol)
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
                val tokenWalletData = state.getWalletData(action.amount.currencySymbol)
                val newTokenWalletData = tokenWalletData?.copy(
                        currencyData = tokenWalletData.currencyData.copy(
                                status = tokenBalanceStatus,
                                amount = action.amount.value?.toFormattedCurrencyString(
                                        action.amount.decimals, action.amount.currencySymbol
                                ),
                                fiatAmount = tokenWalletData.fiatRate?.let {
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
                state.copy(selectedWallet = action.walletData?.currencyData?.currencySymbol)

            is WalletAction.MultiWallet.RemoveWallet -> {
                state.copy(wallets = state.wallets.filterNot {
                    it.currencyData.currencySymbol == action.walletData.currencyData.currencySymbol
                })
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