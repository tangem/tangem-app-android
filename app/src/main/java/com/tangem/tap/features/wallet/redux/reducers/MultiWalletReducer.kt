package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.toFiatString
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.tokens.BlockchainNetwork
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
            is WalletAction.MultiWallet.AddBlockchains -> {
                val wallets: List<WalletStore> = action.blockchains.mapNotNull { blockchain ->
                    val walletManager = action.walletManagers.firstOrNull {
                        it.wallet.blockchain == blockchain.blockchain &&
                                (it.wallet.publicKey.derivationPath?.rawPath == blockchain.derivationPath)
                    } ?: return@mapNotNull null
                    val wallet = walletManager.wallet
                    val cardToken = if (!state.isMultiwalletAllowed) {
                        wallet.getFirstToken()?.symbol?.let { TokenData("", tokenSymbol = it) }
                    } else {
                        null
                    }
                    val walletData = WalletData(
                        currencyData = BalanceWidgetData(
                            BalanceStatus.Loading,
                            blockchain.blockchain.fullName,
                            currencySymbol = blockchain.blockchain.currency,
                            token = cardToken
                        ),
                        walletAddresses = createAddressList(wallet),
                        mainButton = WalletMainButton.SendButton(false),
                        currency = Currency.Blockchain(
                            blockchain.blockchain,
                            blockchain.derivationPath
                        ),
                    )

                    WalletStore(
                        walletManager = walletManager,
                        blockchainNetwork = blockchain,
                        walletsData = listOf(walletData)
                    )
                }

                val selectedCurrency = if (!state.isMultiwalletAllowed) {
                    wallets[0].walletsData[0].currency
                } else {
                    state.selectedCurrency
                }
                state.copy(
                    wallets = wallets,
                    selectedCurrency = selectedCurrency
                )
            }
            is WalletAction.MultiWallet.AddBlockchain -> {
                val walletManager = action.walletManager ?: state.getWalletManager(action.blockchain)
                val wallet = walletManager?.wallet

                val walletData = WalletData(
                    currencyData = BalanceWidgetData(
                        BalanceStatus.Loading,
                        action.blockchain.blockchain.fullName,
                        currencySymbol = action.blockchain.blockchain.currency,
                    ),
                    walletAddresses = createAddressList(wallet),
                    mainButton = WalletMainButton.SendButton(false),
                    currency = Currency.Blockchain(
                        action.blockchain.blockchain,
                        action.blockchain.derivationPath
                    ),
                )
                val walletStore = WalletStore(
                    walletManager = walletManager,
                    blockchainNetwork = action.blockchain,
                    walletsData = listOf(walletData)
                )

                val newState = state.updateWalletStore(walletStore)
                if (wallet != null && wallet.amounts[AmountType.Coin]?.value != null) {
                    OnWalletLoadedReducer().reduce(wallet, action.blockchain, newState)
                } else {
                    newState
                }
            }
            is WalletAction.MultiWallet.AddTokens -> {
                addTokens(action.tokens, action.blockchain, state)
            }
            is WalletAction.MultiWallet.AddToken -> {
                addTokens(listOf(action.token), action.blockchain, state)
            }
            is WalletAction.MultiWallet.TokenLoaded -> {
                val currency = Currency.fromBlockchainNetwork(action.blockchain, action.token)
                val pendingTransactions = state.getWalletManager(currency)
                    ?.wallet?.let { wallet ->
                        wallet.recentTransactions.toPendingTransactions(wallet.address)
                    } ?: emptyList()

                val sendButtonEnabled =
                    action.amount.value?.isZero() == false && pendingTransactions.isEmpty()
                val tokenPendingTransactions = pendingTransactions
                    .filter { it.currency == action.amount.currencySymbol }
                val tokenBalanceStatus = when {
                    tokenPendingTransactions.isNotEmpty() -> BalanceStatus.TransactionInProgress
                    pendingTransactions.isNotEmpty() -> BalanceStatus.SameCurrencyTransactionInProgress
                    else -> BalanceStatus.VerifiedOnline
                }
                val tokenWalletData = state.getWalletData(currency)
                val newTokenWalletData = tokenWalletData?.copy(
                    currencyData = tokenWalletData.currencyData.copy(
                        status = tokenBalanceStatus,
                        amount = action.amount.value,
                        amountFormatted = action.amount.value?.toFormattedCurrencyString(
                            action.amount.decimals, action.amount.currencySymbol
                        ),
                        fiatAmountFormatted = tokenWalletData.fiatRate?.let {
                            action.amount.value
                                ?.toFiatString(it, store.state.globalState.appCurrency)
                        }
                    ),
                    pendingTransactions = pendingTransactions.removeUnknownTransactions(),
                    mainButton = WalletMainButton.SendButton(sendButtonEnabled),
                    currency = Currency.Token(
                        token = action.token,
                        blockchain = action.blockchain.blockchain,
                        derivationPath = action.blockchain.derivationPath
                    )
                )
                state.updateWalletData(newTokenWalletData)
            }
            is WalletAction.MultiWallet.SetIsMultiwalletAllowed ->
                state.copy(isMultiwalletAllowed = action.isMultiwalletAllowed)

            is WalletAction.MultiWallet.SelectWallet ->
                state.copy(selectedCurrency = action.walletData?.currency)

            is WalletAction.MultiWallet.RemoveWallet -> {
                state.removeWallet(action.walletData)
            }
            is WalletAction.MultiWallet.SetPrimaryBlockchain ->
                state.copy(primaryBlockchain = action.blockchain)

            is WalletAction.MultiWallet.SetPrimaryToken ->
                state.copy(primaryToken = action.token)
//            is WalletAction.MultiWallet.FindTokensInUse -> state
//            is WalletAction.MultiWallet.FindBlockchainsInUse -> state
            is WalletAction.MultiWallet.SaveCurrencies -> state
        }
    }
}

private fun addTokens(
    tokens: List<Token>, blockchain: BlockchainNetwork, state: WalletState
): WalletState {
    val wallets = tokens.mapNotNull { token -> token.toWallet(state, blockchain) }
    return state.updateWalletsData(wallets)
}

fun Token.toWallet(state: WalletState, blockchain: BlockchainNetwork): WalletData? {
    if (!state.isMultiwalletAllowed) return null
    val currency = Currency.fromBlockchainNetwork(blockchain, this)
    if (state.currencies.contains(currency)) return null

    val walletManager = state.getWalletManager(currency)?.wallet
    val walletAddresses = createAddressList(walletManager)

    return WalletData(
        currencyData = BalanceWidgetData(
            BalanceStatus.Loading,
            currency = this.name,
            currencySymbol = this.symbol
        ),
        walletAddresses = walletAddresses,
        mainButton = WalletMainButton.SendButton(false),
        currency = currency
    )
}