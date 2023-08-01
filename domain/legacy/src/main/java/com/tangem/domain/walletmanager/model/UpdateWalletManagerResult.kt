package com.tangem.domain.walletmanager.model

import java.math.BigDecimal

sealed class UpdateWalletManagerResult {

    object MissedDerivation : UpdateWalletManagerResult()

    object Unreachable : UpdateWalletManagerResult()

    data class Verified(
        val tokensAmounts: Set<CryptoCurrencyAmount>,
        val hasTransactionsInProgress: Boolean, // TODO: May be add recent transactions
    ) : UpdateWalletManagerResult()

    data class NoAccount(val amountToCreateAccount: BigDecimal) : UpdateWalletManagerResult()
}
