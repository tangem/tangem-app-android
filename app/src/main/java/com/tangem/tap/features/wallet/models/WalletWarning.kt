package com.tangem.tap.features.wallet.models

sealed class WalletWarning(
    val showingPosition: Int,
) {
    data class ExistentialDeposit(
        val currencyName: String,
        val edStringValueWithSymbol: String,
    ) : WalletWarning(1)

    data class TransactionInProgress(val currencyName: String) : WalletWarning(showingPosition = 10)
    data class BalanceNotEnoughForFee(val blockchainFullName: String) : WalletWarning(showingPosition = 30)
    data class Rent(val walletRent: WalletRent) : WalletWarning(showingPosition = 40)
}

data class WalletWarningDescription(
    val title: String,
    val message: String,
)

data class WalletRent(
    val minRentValue: String,
    val rentExemptValue: String,
)
