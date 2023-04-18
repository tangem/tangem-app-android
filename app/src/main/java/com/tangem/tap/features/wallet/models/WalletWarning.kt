package com.tangem.tap.features.wallet.models

import com.tangem.tap.domain.model.WalletStoreModel

sealed class WalletWarning(val showingPosition: Int) {

    data class ExistentialDeposit(
        val currencyName: String,
        val edStringValueWithSymbol: String,
    ) : WalletWarning(1)

    data class TransactionInProgress(val currencyName: String) : WalletWarning(showingPosition = 10)

    data class BalanceNotEnoughForFee(
        val currencyName: String,
        val blockchainFullName: String,
        val blockchainSymbol: String,
    ) : WalletWarning(showingPosition = 30)

    data class Rent(val walletRent: WalletStoreModel.WalletRent) : WalletWarning(showingPosition = 40)
}

data class WalletWarningDescription(val title: String, val message: String)
