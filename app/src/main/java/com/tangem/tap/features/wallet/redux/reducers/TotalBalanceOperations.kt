package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.ui.BalanceStatus
import java.math.BigDecimal

fun List<WalletData>.findProgressState(): ProgressState {
    return this
        .mapToProgressState()
        .reduce(ProgressState::or)
}

fun List<WalletData>.calculateTotalFiatAmount(): BigDecimal {
    return this
        .map { it.currencyData.fiatAmount ?: BigDecimal.ZERO }
        .reduce(BigDecimal::plus)
}

fun List<WalletData>.calculateTotalCryptoAmount(): BigDecimal {
    return this
        .map { it.currencyData.amount ?: BigDecimal.ZERO }
        .reduce(BigDecimal::plus)
}

private fun List<WalletData>.mapToProgressState(): List<ProgressState> {
    return this.map { wallet ->
        if (wallet.fiatRate == null) {
            ProgressState.Error
        } else {
            when (wallet.currencyData.status) {
                BalanceStatus.Refreshing -> ProgressState.Refreshing
                BalanceStatus.VerifiedOnline,
                BalanceStatus.SameCurrencyTransactionInProgress,
                BalanceStatus.TransactionInProgress,
                BalanceStatus.NoAccount,
                -> ProgressState.Done
                BalanceStatus.Unreachable,
                BalanceStatus.EmptyCard,
                BalanceStatus.UnknownBlockchain,
                BalanceStatus.MissedDerivation,
                -> ProgressState.Error
                BalanceStatus.Loading,
                null,
                -> ProgressState.Loading
            }
        }
    }
}

private infix fun ProgressState.or(newState: ProgressState): ProgressState {
    return when (this) {
        ProgressState.Loading -> when (newState) {
            ProgressState.Loading,
            ProgressState.Refreshing,
            ProgressState.Error,
            ProgressState.Done,
            -> this
        }
        ProgressState.Done,
        ProgressState.Error,
        -> when (newState) {
            ProgressState.Loading,
            ProgressState.Refreshing,
            ProgressState.Error,
            -> newState
            ProgressState.Done -> this
        }
        ProgressState.Refreshing -> when (newState) {
            ProgressState.Loading -> this
            ProgressState.Refreshing,
            ProgressState.Error,
            ProgressState.Done,
            -> newState
        }
    }
}
