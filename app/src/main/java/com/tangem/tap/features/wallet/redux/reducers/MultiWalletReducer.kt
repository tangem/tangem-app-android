package com.tangem.tap.features.wallet.redux.reducers

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchain.blockchains.polkadot.ExistentialDepositProvider
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.common.extensions.dispatchToastNotification
import com.tangem.tap.common.extensions.toFiatString
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.WalletRent
import com.tangem.tap.features.wallet.models.filterByToken
import com.tangem.tap.features.wallet.models.getPendingTransactions
import com.tangem.tap.features.wallet.models.removeUnknownTransactions
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletMainButton
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.WalletState.Companion.UNKNOWN_AMOUNT_SIGN
import com.tangem.tap.features.wallet.redux.WalletStore
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.features.wallet.ui.TokenData
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManager
import com.tangem.wallet.R
import java.math.BigDecimal

class MultiWalletReducer {
    fun reduce(action: WalletAction.MultiWallet, state: WalletState): WalletState {
        return when (action) {
            is WalletAction.MultiWallet.AddBlockchains -> {
                val wallets: List<WalletStore> = action.blockchains.map { blockchain ->
                    val walletManager = action.walletManagers.firstOrNull {
                        it.wallet.blockchain == blockchain.blockchain &&
                            (it.wallet.publicKey.derivationPath?.rawPath == blockchain.derivationPath)
                    }
                    val wallet = walletManager?.wallet
                    val cardToken = if (!state.isMultiwalletAllowed) {
                        wallet?.getFirstToken()?.symbol?.let { TokenData("", tokenSymbol = it) }
                    } else {
                        null
                    }
                    val walletData = WalletData(
                        currencyData = BalanceWidgetData(
                            status = BalanceStatus.Loading,
                            currency = blockchain.blockchain.fullName,
                            currencySymbol = blockchain.blockchain.currency,
                            token = cardToken
                        ),
                        walletAddresses = createAddressList(wallet),
                        mainButton = WalletMainButton.SendButton(false),
                        currency = Currency.Blockchain(
                            blockchain.blockchain,
                            blockchain.derivationPath
                        ),
                        existentialDepositString = getExistentialDeposit(walletManager),
                    )

                    WalletStore(
                        walletManager = walletManager,
                        blockchainNetwork = blockchain,
                        walletsData = listOf(walletData)
                    )
                }

                val selectedCurrency = if (!state.isMultiwalletAllowed) {
                    wallets.firstOrNull()?.walletsData?.firstOrNull()?.currency
                } else {
                    state.selectedCurrency
                }
                state.copy(wallets = wallets, selectedCurrency = selectedCurrency)
            }
            is WalletAction.MultiWallet.AddBlockchain -> {
                val walletManager = action.walletManager ?: state.getWalletManager(action.blockchain)
                val wallet = walletManager?.wallet

                val walletData = WalletData(
                    currencyData = BalanceWidgetData(
                        status = BalanceStatus.Loading,
                        currency = action.blockchain.blockchain.fullName,
                        currencySymbol = action.blockchain.blockchain.currency,
                    ),
                    walletAddresses = createAddressList(wallet),
                    mainButton = WalletMainButton.SendButton(false),
                    currency = Currency.Blockchain(
                        action.blockchain.blockchain,
                        action.blockchain.derivationPath
                    ),
                    existentialDepositString = getExistentialDeposit(walletManager),
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
            is WalletAction.MultiWallet.AddTokens -> addTokens(action.tokens, action.blockchain, state)
            is WalletAction.MultiWallet.AddToken -> addTokens(listOf(action.token), action.blockchain, state)
            is WalletAction.MultiWallet.TokenLoaded -> {
                val currency = Currency.fromBlockchainNetwork(action.blockchain, action.token)
                val walletManager = state.getWalletManager(currency)
                if (walletManager == null) {
                    if (userWalletsListManager.hasSavedUserWallets) {
                        store.dispatch(NavigationAction.PopBackTo(screen = AppScreen.Welcome))
                    } else {
                        store.dispatch(NavigationAction.PopBackTo(screen = AppScreen.Home))
                    }
                    FirebaseCrashlytics.getInstance().recordException(
                        IllegalStateException("MultiWallet.TokenLoaded: walletManager is null"),
                    )
                    store.dispatchToastNotification(R.string.internal_error_wallet_manager_not_found)
                    return state
                }
                val wallet = walletManager.wallet
                val pendingTransactions = wallet.getPendingTransactions()
                val tokenPendingTransactions = pendingTransactions.filterByToken(action.token)
                val tokenBalanceStatus = when {
                    tokenPendingTransactions.isNotEmpty() -> BalanceStatus.TransactionInProgress
                    pendingTransactions.isNotEmpty() -> BalanceStatus.SameCurrencyTransactionInProgress
                    else -> BalanceStatus.VerifiedOnline
                }
                val tokenWalletData = state.getWalletData(currency)
                val isTokenSendButtonEnabled = tokenWalletData?.shouldEnableTokenSendButton() == true
                    && pendingTransactions.isEmpty()

                val newTokenWalletData = tokenWalletData?.copy(
                    currencyData = tokenWalletData.currencyData.copy(
                        status = tokenBalanceStatus,
                        amount = action.amount.value,
                        amountFormatted = action.amount.value?.toFormattedCurrencyString(
                            action.amount.decimals, action.amount.currencySymbol
                        ),
                        fiatAmountFormatted = tokenWalletData.fiatRate?.let {
                            action.amount.value?.toFiatString(it, store.state.globalState.appCurrency.symbol)
                        } ?: UNKNOWN_AMOUNT_SIGN,
                        blockchainAmount = wallet.amounts[AmountType.Coin]?.value ?: BigDecimal.ZERO
                    ),
                    pendingTransactions = pendingTransactions.removeUnknownTransactions(),
                    mainButton = WalletMainButton.SendButton(isTokenSendButtonEnabled),
                    currency = Currency.Token(
                        token = action.token,
                        blockchain = action.blockchain.blockchain,
                        derivationPath = action.blockchain.derivationPath
                    ),
                    walletRent = findWalletRent(state.getWalletStore(walletManager.wallet))
                )
                state.updateWalletData(newTokenWalletData)
            }
            is WalletAction.MultiWallet.SetIsMultiwalletAllowed -> state.copy(isMultiwalletAllowed = action.isMultiwalletAllowed)
            is WalletAction.MultiWallet.SelectWallet -> state.copy(selectedCurrency = action.walletData?.currency)
            is WalletAction.MultiWallet.TryToRemoveWallet -> state
            is WalletAction.MultiWallet.RemoveWallet -> state.removeWallet(state.getWalletData(action.currency))
            is WalletAction.MultiWallet.RemoveWallets -> {
                var updatedState = state
                action.currencies.forEach { updatedState = updatedState.removeWallet(state.getWalletData(it)) }
                updatedState
            }
            is WalletAction.MultiWallet.SetPrimaryBlockchain -> state.copy(primaryBlockchain = action.blockchain)
            is WalletAction.MultiWallet.SetPrimaryToken -> state.copy(primaryToken = action.token)
            is WalletAction.MultiWallet.SaveCurrencies -> state
            is WalletAction.MultiWallet.ShowWalletBackupWarning -> state.copy(showBackupWarning = action.show)
            is WalletAction.MultiWallet.AddMissingDerivations -> state.copy(missingDerivations = action.blockchains)
            is WalletAction.MultiWallet.BackupWallet -> state
            is WalletAction.MultiWallet.ScanToGetDerivations -> state.copy(state = ProgressState.Loading)
        }
    }

    private fun findWalletRent(walletStore: WalletStore?): WalletRent? {
        return walletStore?.walletsData?.firstOrNull { it.walletRent != null }?.walletRent
    }

    private fun getExistentialDeposit(walletManager: WalletManager?): String? {
        return (walletManager as? ExistentialDepositProvider)?.getExistentialDeposit()?.toPlainString()
    }

    private fun addTokens(tokens: List<Token>, blockchain: BlockchainNetwork, state: WalletState): WalletState {
        val wallets = tokens.mapNotNull { token -> token.toWallet(state, blockchain) }
        return state.updateWalletsData(wallets)
    }
}

fun Token.toWallet(state: WalletState, blockchain: BlockchainNetwork): WalletData? {
    if (!state.isMultiwalletAllowed) return null
    val currency = Currency.fromBlockchainNetwork(blockchain, this)
    if (state.currencies.contains(currency)) return null

    val walletManager = state.getWalletManager(currency)?.wallet
    val walletAddresses = createAddressList(walletManager)

    return WalletData(
        currencyData = BalanceWidgetData(
            status = BalanceStatus.Loading,
            currency = this.name,
            currencySymbol = this.symbol
        ),
        walletAddresses = walletAddresses,
        mainButton = WalletMainButton.SendButton(false),
        currency = currency
    )
}
