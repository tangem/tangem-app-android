package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.tap.features.wallet.models.TotalBalance
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.ui.BalanceStatus
import java.math.BigDecimal

fun List<WalletData>.findTotalBalanceState(): TotalBalance.State {
    return this.mapToTotalBalanceState()
        .reduce(TotalBalance.State::or)
}

fun List<WalletData>.calculateTotalFiatAmount(): BigDecimal {
    return this.map { it.currencyData.fiatAmount ?: BigDecimal.ZERO }
        .reduce(BigDecimal::plus)
}

fun List<WalletData>.mapToTotalBalanceState(): List<TotalBalance.State> {
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
            TotalBalance.State.Loading,
            TotalBalance.State.SomeTokensFailed,
            TotalBalance.State.Success -> this
        }
        TotalBalance.State.Success,
        TotalBalance.State.SomeTokensFailed -> when (newState) {
            TotalBalance.State.Loading,
            TotalBalance.State.SomeTokensFailed -> newState
            TotalBalance.State.Success -> this
        }
    }
}