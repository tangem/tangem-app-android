package com.tangem.domain.walletmanager.model

import java.math.BigDecimal

sealed class UpdateWalletManagerResult {

    object MissedDerivation : UpdateWalletManagerResult()

    object Unreachable : UpdateWalletManagerResult()

    data class Verified(
        val defaultAddress: String,
        val addresses: Set<String>,
        val currenciesAmounts: Set<CryptoCurrencyAmount>,
        val currentTransactions: Set<CryptoCurrencyTransaction>,
    ) : UpdateWalletManagerResult()

    data class NoAccount(
        val defaultAddress: String,
        val addresses: Set<String>,
        val amountToCreateAccount: BigDecimal,
    ) : UpdateWalletManagerResult()
}