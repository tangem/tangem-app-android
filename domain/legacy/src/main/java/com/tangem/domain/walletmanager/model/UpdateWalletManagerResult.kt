package com.tangem.domain.walletmanager.model

import java.math.BigDecimal

sealed class UpdateWalletManagerResult {

    object MissedDerivation : UpdateWalletManagerResult()

    object Unreachable : UpdateWalletManagerResult()

    data class Verified(
        val selectedAddress: String,
        val addresses: Set<Address>,
        val currenciesAmounts: Set<CryptoCurrencyAmount>,
        val currentTransactions: Set<CryptoCurrencyTransaction>,
    ) : UpdateWalletManagerResult()

    data class NoAccount(
        val selectedAddress: String,
        val addresses: Set<Address>,
        val amountToCreateAccount: BigDecimal,
        val errorMessage: String,
    ) : UpdateWalletManagerResult()
}
