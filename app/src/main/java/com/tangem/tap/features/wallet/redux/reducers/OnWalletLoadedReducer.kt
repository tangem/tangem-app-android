package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.toFiatString
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.features.wallet.models.removeUnknownTransactions
import com.tangem.tap.features.wallet.models.toPendingTransactions
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletMainButton
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.features.wallet.ui.TokenData
import com.tangem.tap.store
import java.math.RoundingMode

class OnWalletLoadedReducer {

    fun reduce(wallet: Wallet, walletState: WalletState, topUpAllowed: Boolean? = null): WalletState {
        return if (!walletState.isMultiwalletAllowed) {
            onSingleWalletLoaded(wallet, walletState, topUpAllowed)
        } else {
            onMultiWalletLoaded(wallet, walletState, topUpAllowed)
        }
    }

    private fun onMultiWalletLoaded(
            wallet: Wallet, walletState: WalletState, topUpAllowed: Boolean? = null
    ): WalletState {
        val fiatCurrencySymbol = store.state.globalState.appCurrency
        val amount = wallet.amounts[AmountType.Coin]?.value
        if (walletState.getWalletData(wallet.blockchain.currency) == null) {
            return walletState
        }
        val formattedAmount = amount?.toFormattedCurrencyString(
                wallet.blockchain.decimals(),
                wallet.blockchain.currency)

        val pendingTransactions = wallet.recentTransactions
                .toPendingTransactions(wallet.address)

        val sendButtonEnabled = amount?.isZero() == false && pendingTransactions.isEmpty()
        val balanceStatus = if (pendingTransactions.isNotEmpty()) {
            BalanceStatus.TransactionInProgress
        } else {
            BalanceStatus.VerifiedOnline
        }
        val walletData = walletState.getWalletData(wallet.blockchain.currency)
                ?: WalletData()
        val newWalletData = walletData.copy(
                currencyData = walletData.currencyData.copy(
                        status = balanceStatus, currency = wallet.blockchain.fullName,
                        currencySymbol = wallet.blockchain.currency,
                        amount = formattedAmount,
                        fiatAmount = walletData.fiatRate?.let { amount?.toFiatString(it, fiatCurrencySymbol) }
                ),
                pendingTransactions = pendingTransactions.removeUnknownTransactions(),
                mainButton = WalletMainButton.SendButton(sendButtonEnabled)
        )

        val tokens = wallet.getTokens().mapNotNull { token ->
            val tokenWalletData = walletState.getWalletData(token.symbol)
            tokenWalletData?.copy(
                    currencyData = tokenWalletData.currencyData.copy(
                            status = balanceStatus,
                            amount = wallet.getTokenAmount(token)?.value?.toFormattedCurrencyString(
                                    token.decimals, token.symbol
                            ),
                            fiatAmount = tokenWalletData.fiatRate?.let {
                                wallet.getTokenAmount(token)?.value
                                        ?.toFiatString(it, fiatCurrencySymbol)
                            }
                    ),
                    pendingTransactions = pendingTransactions.removeUnknownTransactions(),
                    mainButton = WalletMainButton.SendButton(sendButtonEnabled)
            )
        }
        val wallets = walletState.replaceSomeWallets((tokens + newWalletData))

        val state = if (wallets.any { it.currencyData.status == BalanceStatus.Loading }) {
            ProgressState.Loading
        } else {
            ProgressState.Done
        }
        return walletState.copy(
                state = state, wallets = wallets
        )
    }

    private fun onSingleWalletLoaded(
            wallet: Wallet, walletState: WalletState, topUpAllowed: Boolean? = null
    ): WalletState {
        if (wallet.blockchain != walletState.primaryBlockchain) return walletState

        val fiatCurrencySymbol = store.state.globalState.appCurrency
        val token = wallet.getFirstToken()
        val tokenData = if (token != null) {
            val tokenAmount = wallet.getTokenAmount(token)
            if (tokenAmount != null) {
                val tokenFiatRate = walletState.primaryWallet?.currencyData?.token?.fiatRate
                val tokenFiatAmount = tokenFiatRate?.let { tokenAmount.value?.toFiatString(it, fiatCurrencySymbol) }
                TokenData(tokenAmount.value?.toFormattedString(tokenAmount.decimals) ?: "",
                        tokenAmount.currencySymbol, tokenFiatAmount)
            } else {
                null
            }
        } else {
            null
        }
        val amount = wallet.amounts[AmountType.Coin]?.value
        val formattedAmount = amount?.toFormattedCurrencyString(
                wallet.blockchain.decimals(),
                wallet.blockchain.currency)
        val fiatRate = walletState.primaryWallet?.fiatRate
        val fiatAmountRaw = fiatRate?.multiply(amount)?.setScale(2, RoundingMode.DOWN)
        val fiatAmount = fiatRate?.let { amount?.toFiatString(it, fiatCurrencySymbol) }

        val pendingTransactions = wallet.recentTransactions
                .toPendingTransactions(wallet.address)

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
                        formattedAmount,
                        token = tokenData,
                        fiatAmount = fiatAmount,
                        fiatAmountRaw = fiatAmountRaw
                ),
                pendingTransactions = pendingTransactions.removeUnknownTransactions(),
                mainButton = WalletMainButton.SendButton(sendButtonEnabled)
        )
        val wallets = walletData?.let { listOf(walletData) } ?: emptyList()
        return walletState.copy(
                state = ProgressState.Done, wallets = wallets
        )
    }
}