package com.tangem.domain.tokens.model.warnings

sealed class DynamicAddressesWarnings : CryptoCurrencyWarning() {

    data object FundsFound : DynamicAddressesWarnings()
}