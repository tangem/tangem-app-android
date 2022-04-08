package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.toFiatString
import com.tangem.tap.common.extensions.toFiatValue
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.common.extensions.toFormattedFiatValue
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.tokens.BlockchainNetwork
import com.tangem.tap.features.wallet.models.removeUnknownTransactions
import com.tangem.tap.features.wallet.models.toPendingTransactions
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.features.wallet.ui.TokenData
import com.tangem.tap.store
import java.math.RoundingMode

class OnWalletLoadedReducer {

    fun reduce(wallet: Wallet, blockchainNetwork: BlockchainNetwork, walletState: WalletState): WalletState {
        return if (!walletState.isMultiwalletAllowed) {
            onSingleWalletLoaded(wallet, walletState)
        } else {
            onMultiWalletLoaded(wallet, blockchainNetwork, walletState)
        }
    }

    private fun onMultiWalletLoaded(
        wallet: Wallet,
        blockchainNetwork: BlockchainNetwork,
        walletState: WalletState
    ): WalletState {
        val fiatCurrencySymbol = store.state.globalState.appCurrency
        val exchangeManager = store.state.globalState.currencyExchangeManager

        val coinAmountValue = wallet.amounts[AmountType.Coin]?.value
        if (walletState.getWalletData(blockchainNetwork) == null) {
            return walletState
        }
        val formattedAmount = coinAmountValue?.toFormattedCurrencyString(
            wallet.blockchain.decimals(),
            wallet.blockchain.currency
        )

        val pendingTransactions = wallet.recentTransactions
            .toPendingTransactions(wallet.address)

        val coinSendButton = coinAmountValue?.isZero() == false && pendingTransactions.isEmpty()
        val balanceStatus = if (pendingTransactions.isNotEmpty()) {
            BalanceStatus.TransactionInProgress
        } else {
            BalanceStatus.VerifiedOnline
        }
        val walletData = walletState.getWalletData(blockchainNetwork)

        val fiatAmount = walletData?.fiatRate?.let { coinAmountValue?.toFiatValue(it) }
        val newWalletData = walletData?.copy(
            currencyData = walletData.currencyData.copy(
                status = balanceStatus, currency = wallet.blockchain.fullName,
                currencySymbol = wallet.blockchain.currency,
                blockchainAmount = coinAmountValue,
                amount = coinAmountValue,
                amountFormatted = formattedAmount,
                fiatAmount = fiatAmount,
                fiatAmountFormatted = fiatAmount?.toFormattedFiatValue(fiatCurrencySymbol)
            ),
            pendingTransactions = pendingTransactions.removeUnknownTransactions(),
            mainButton = WalletMainButton.SendButton(coinSendButton),
            currency = Currency.fromBlockchainNetwork(blockchainNetwork),
            tradeCryptoState = TradeCryptoState.from(exchangeManager, walletData),
        )

        val tokens = wallet.getTokens().mapNotNull { token ->
            val tokenWalletData = walletState.getWalletData(token)
            val tokenPendingTransactions =
                pendingTransactions.filter { it.currency == token.symbol }
            val tokenBalanceStatus = when {
                tokenPendingTransactions.isNotEmpty() -> BalanceStatus.TransactionInProgress
                pendingTransactions.isNotEmpty() -> BalanceStatus.SameCurrencyTransactionInProgress
                else -> BalanceStatus.VerifiedOnline
            }
            val tokenAmountValue = wallet.getTokenAmount(token)?.value
            val tokenFiatAmount =
                tokenWalletData?.fiatRate?.let { rate -> tokenAmountValue?.toFiatValue(rate) }

            val tokenSendButton = newWalletData?.shouldEnableTokenSendButton() == true
                    && tokenPendingTransactions.isEmpty()
            tokenWalletData?.copy(
                currencyData = tokenWalletData.currencyData.copy(
                    status = tokenBalanceStatus,
                    blockchainAmount = coinAmountValue,
                    amount = tokenAmountValue,
                    amountFormatted = tokenAmountValue?.toFormattedCurrencyString(
                        token.decimals,
                        token.symbol
                    ),
                    fiatAmount = tokenFiatAmount,
                    fiatAmountFormatted = tokenFiatAmount?.toFormattedFiatValue(fiatCurrencySymbol)
                ),
                pendingTransactions = tokenPendingTransactions.removeUnknownTransactions(),
                mainButton = WalletMainButton.SendButton(tokenSendButton),
                tradeCryptoState = TradeCryptoState.from(exchangeManager, tokenWalletData),
            )
        }
        val newWallets = (tokens + newWalletData).mapNotNull { it }
        val wallets = walletState.replaceSomeWallets((newWallets))

        val state = if (wallets.any { it.currencyData.status == BalanceStatus.Loading }) {
            ProgressState.Loading
        } else {
            ProgressState.Done
        }
        val newState = walletState.updateWalletsData(wallets)
        return newState.copy(
            state = state, error = null
        )
    }

    private fun onSingleWalletLoaded(wallet: Wallet, walletState: WalletState): WalletState {
        if (wallet.blockchain != walletState.primaryBlockchain) return walletState

        val fiatCurrencySymbol = store.state.globalState.appCurrency
        val exchangeManager = store.state.globalState.currencyExchangeManager

        val token = wallet.getFirstToken()
        val tokenData = if (token != null) {
            val tokenAmount = wallet.getTokenAmount(token)
            if (tokenAmount != null) {
                val tokenFiatRate = walletState.primaryWallet?.currencyData?.token?.fiatRate
                val tokenFiatAmount =
                    tokenFiatRate?.let { tokenAmount.value?.toFiatString(it, fiatCurrencySymbol) }
                TokenData(
                    tokenAmount.value?.toFormattedCurrencyString(
                        token.decimals, token.symbol
                    ) ?: "",
                    tokenAmount.currencySymbol, tokenFiatAmount
                )
            } else {
                null
            }
        } else {
            null
        }
        val amount = wallet.amounts[AmountType.Coin]?.value
        val formattedAmount = amount?.toFormattedCurrencyString(
            wallet.blockchain.decimals(),
            wallet.blockchain.currency
        )
        val fiatRate = walletState.primaryWallet?.fiatRate
        val fiatAmountRaw = fiatRate?.multiply(amount)?.setScale(2, RoundingMode.DOWN)
        val fiatAmount = fiatRate?.let { amount?.toFiatString(it, fiatCurrencySymbol) }

        val pendingTransactions = wallet.recentTransactions.toPendingTransactions(wallet.address)
        val sendButtonEnabled = amount?.isZero() == false && pendingTransactions.isEmpty()
        val balanceStatus = if (pendingTransactions.isNotEmpty()) {
            BalanceStatus.TransactionInProgress
        } else {
            BalanceStatus.VerifiedOnline
        }
        val walletData = walletState.primaryWallet?.copy(
            currencyData = BalanceWidgetData(
                balanceStatus, wallet.blockchain.fullName,
                currencySymbol = wallet.blockchain.currency,
                token = tokenData,
                blockchainAmount = amount,
                amount = amount,
                amountFormatted = formattedAmount,
                fiatAmountFormatted = fiatAmount,
                fiatAmount = fiatAmountRaw
            ),
            pendingTransactions = pendingTransactions.removeUnknownTransactions(),
            mainButton = WalletMainButton.SendButton(sendButtonEnabled),
            tradeCryptoState = TradeCryptoState.from(exchangeManager, walletState.primaryWallet),
        )
        val wallets = listOfNotNull(walletData)
        val updatedStore = walletState.getWalletStore(walletData?.currency)?.updateWallets(wallets)

        return walletState.updateWalletStore(updatedStore).copy(
            state = ProgressState.Done, error = null
        )
    }
}