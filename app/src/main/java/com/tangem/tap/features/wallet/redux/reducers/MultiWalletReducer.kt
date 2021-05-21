package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
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
                        walletManagers = state.walletManagers + action.walletManagers
                )
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
                            topUpState = TopUpState(allowed = false),
                            currency = Currency.Blockchain(blockchain)
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
                        topUpState = TopUpState(allowed = false),
                        currency = Currency.Blockchain(action.blockchain)
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
                    val walletManager = state.getWalletManager(token)?.wallet
                    WalletData(
                            currencyData = BalanceWidgetData(
                                    BalanceStatus.Loading,
                                    currency = token.name,
                                    currencySymbol = token.symbol
                            ),
                            walletAddresses = createAddressList(walletManager),
                            mainButton = WalletMainButton.SendButton(false),
                            topUpState = TopUpState(allowed = false),
                            currency = Currency.Token(
                                token = token,
                                blockchain = walletManager?.blockchain ?: Blockchain.Ethereum
                            )
                    )
                }
                state.copy(wallets = state.replaceSomeWallets(wallets))
            }
            is WalletAction.MultiWallet.AddToken -> {
                if (!state.isMultiwalletAllowed) return state
                val walletManager = state.getWalletManager(action.token)?.wallet
                val walletAddresses = createAddressList(walletManager)

                val wallet = WalletData(
                        currencyData = BalanceWidgetData(
                                BalanceStatus.Loading,
                                currency = action.token.name,
                                currencySymbol = action.token.symbol
                        ),
                        walletAddresses = walletAddresses,
                        mainButton = WalletMainButton.SendButton(false),
                        topUpState = TopUpState(allowed = false),
                    currency = Currency.Token(
                        token = action.token,
                        blockchain = walletManager?.blockchain ?: Blockchain.Ethereum
                    )
                )
                val wallets = state.replaceWalletInWallets(wallet)
                state.copy(wallets = wallets)
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