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
import com.tangem.tap.features.wallet.models.TotalBalance
import com.tangem.tap.features.wallet.models.removeUnknownTransactions
import com.tangem.tap.features.wallet.models.toPendingTransactions
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.features.wallet.ui.TokenData
import com.tangem.tap.store
import java.math.BigDecimal
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
        val walletData = walletState.getWalletData(blockchainNetwork) ?: return walletState

        val fiatCurrency = store.state.globalState.appCurrency
        val exchangeManager = store.state.globalState.currencyExchangeManager

        val coinAmountValue = wallet.amounts[AmountType.Coin]?.value
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

        val fiatAmount = walletData.fiatRate?.let { coinAmountValue?.toFiatValue(it) }
        val newWalletData = walletData.copy(
            currencyData = walletData.currencyData.copy(
                status = balanceStatus, currency = wallet.blockchain.fullName,
                currencySymbol = wallet.blockchain.currency,
                blockchainAmount = coinAmountValue,
                amount = coinAmountValue,
                amountFormatted = formattedAmount,
                fiatAmount = fiatAmount,
                fiatAmountFormatted = fiatAmount?.toFormattedFiatValue(fiatCurrency.symbol)
            ),
            pendingTransactions = pendingTransactions.removeUnknownTransactions(),
            mainButton = WalletMainButton.SendButton(coinSendButton),
            currency = Currency.fromBlockchainNetwork(blockchainNetwork),
            tradeCryptoState = TradeCryptoState.from(exchangeManager, walletData),
        )

        val tokens = wallet.getTokens().mapNotNull { token ->
            val currency = Currency.fromBlockchainNetwork(blockchainNetwork, token)
            val tokenWalletData = walletState.getWalletData(currency)
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

            val tokenSendButton = newWalletData.shouldEnableTokenSendButton()
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
                    fiatAmountFormatted = tokenFiatAmount?.toFormattedFiatValue(fiatCurrency.code)
                ),
                pendingTransactions = tokenPendingTransactions.removeUnknownTransactions(),
                mainButton = WalletMainButton.SendButton(tokenSendButton),
                tradeCryptoState = TradeCryptoState.from(exchangeManager, tokenWalletData),
            )
        }
        val newWallets = tokens + newWalletData
        val wallets = walletState.replaceSomeWallets((newWallets))

        val totalBalance = TotalBalance(
            state = wallets.findTotalBalanceState(),
            fiatAmount = wallets.calculateTotalFiatAmount(),
            fiatCurrency = fiatCurrency,
        )

        val state = if (wallets.any { it.currencyData.status == BalanceStatus.Loading }) {
            ProgressState.Loading
        } else {
            ProgressState.Done
        }
        return walletState
            .updateWalletsData(wallets)
            .updateTotalBalance(totalBalance)
            .copy(
                state = state,
                error = null
            )
    }

    private fun onSingleWalletLoaded(wallet: Wallet, walletState: WalletState): WalletState {
        if (wallet.blockchain != walletState.primaryBlockchain) return walletState

        val fiatCurrencyName = store.state.globalState.appCurrency.code
        val exchangeManager = store.state.globalState.currencyExchangeManager

        val token = wallet.getFirstToken()
        val tokenData = if (token != null) {
            val tokenAmount = wallet.getTokenAmount(token)
            if (tokenAmount != null) {
                val tokenFiatRate = walletState.primaryWallet?.currencyData?.token?.fiatRate
                val tokenFiatAmount =
                    tokenFiatRate?.let { tokenAmount.value?.toFiatString(it, fiatCurrencyName) }
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
        val fiatAmount = fiatRate?.let { amount?.toFiatString(it, fiatCurrencyName) }

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

    private fun List<WalletData>.findTotalBalanceState(): TotalBalance.State {
        return this.mapToTotalBalanceState()
            .fold(initial = TotalBalance.State.Loading) { accState, newState ->
                accState or newState
            }
    }

    private fun List<WalletData>.calculateTotalFiatAmount(): BigDecimal {
        return this.map { it.currencyData.fiatAmount ?: BigDecimal.ZERO }
            .reduce(BigDecimal::plus)
    }

    private fun List<WalletData>.mapToTotalBalanceState(): List<TotalBalance.State> {
        return this.map {
            when (it.currencyData.status) {
                BalanceStatus.VerifiedOnline,
                BalanceStatus.SameCurrencyTransactionInProgress,
                BalanceStatus.TransactionInProgress -> TotalBalance.State.Success
                BalanceStatus.Unreachable,
                BalanceStatus.NoAccount,
                BalanceStatus.EmptyCard,
                BalanceStatus.UnknownBlockchain -> TotalBalance.State.SomeTokensFailed
                BalanceStatus.Loading,
                null -> TotalBalance.State.Loading
            }
        }
    }

    infix fun TotalBalance.State.or(newState: TotalBalance.State): TotalBalance.State {
        return when (this) {
            TotalBalance.State.Loading -> when (newState) {
                TotalBalance.State.Loading -> this
                TotalBalance.State.SomeTokensFailed,
                TotalBalance.State.Success -> newState
            }
            TotalBalance.State.Success,
            TotalBalance.State.SomeTokensFailed -> when (newState) {
                TotalBalance.State.Loading,
                TotalBalance.State.SomeTokensFailed -> newState
                TotalBalance.State.Success -> this
            }
        }
    }
}